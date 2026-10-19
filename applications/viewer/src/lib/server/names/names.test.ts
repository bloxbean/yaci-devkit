import { gzipSync } from 'node:zlib';
import { describe, expect, it, vi } from 'vitest';
import { cleanText } from './clean';
import { fillDocs, syncDocs, type DocEntry } from './docs';
import { crfaNames, crfaScriptHash, mergeScriptNames, stricaNames } from './registries';
import { untarGz } from './tar';
import { TokenNames, tokenFromRegistry } from './tokens';

/** A .tar.gz with these files, the way GitHub archives them (a top folder, a pax header for a long name). */
function tarGz(files: Record<string, string>): Uint8Array {
	const blocks: Uint8Array[] = [];
	const header = (name: string, size: number, type: string) => {
		const h = new Uint8Array(512);
		const put = (s: string, at: number) => h.set(new TextEncoder().encode(s), at);
		put(name.slice(0, 99), 0);
		put(size.toString(8).padStart(11, '0'), 124);
		put(type, 156);
		put('ustar\u000000', 257);
		return h;
	};
	const pad = (b: Uint8Array) => {
		const out = new Uint8Array(Math.ceil(b.length / 512) * 512);
		out.set(b);
		return out;
	};
	blocks.push(header('pax_global_header', 0, 'g'));
	for (const [path, text] of Object.entries(files)) {
		const data = new TextEncoder().encode(text);
		if (path.length > 99) {
			const rec = ` path=${path}\n`;
			const len = String(rec.length + String(rec.length).length);
			const pax = new TextEncoder().encode(`${len}${rec}`);
			blocks.push(header('PaxHeader', pax.length, 'x'), pad(pax));
		}
		blocks.push(header(path, data.length, '0'), pad(data));
	}
	blocks.push(new Uint8Array(1024));
	const all = new Uint8Array(blocks.reduce((n, b) => n + b.length, 0));
	let off = 0;
	for (const b of blocks) all.set(b, (off += b.length) - b.length);
	return gzipSync(all);
}

const h = (c: string) => c.repeat(56);

describe('registries', () => {
	const long = `repo-master/projects/${'x'.repeat(100)}.json`;
	const strica = untarGz(
		tarGz({
			'repo-master/README.md': '# hi',
			'repo-master/projects/minswap.json': JSON.stringify({
				projectName: 'Minswap',
				labelPrefix: 'Minswap',
				category: 'DEX',
				website: 'https://minswap.org',
				contracts: [
					{ name: 'Order Contract', version: 2, scriptHash: h('a') },
					{ name: 'contract', version: 1, scriptHash: h('b') },
					{ name: 'minswap batcher', version: 1, scriptHash: h('f') },
					{ name: 'Bad', version: 1, scriptHash: 'xyz' }
				]
			}),
			[long]: JSON.stringify({ projectName: 'Long‮Name', labelPrefix: 'Long', contracts: [{ name: 'Pool', version: 1, scriptHash: h('c') }] })
		})
	);

	it('reads a GitHub archive, long names included', () => {
		expect(strica.map((f) => f.path)).toEqual(['repo-master/README.md', 'repo-master/projects/minswap.json', long]);
	});

	it('labels Strica contracts as prefix + name (+ version past 1)', () => {
		const names = stricaNames(strica);
		expect(names.get(h('a'))).toEqual({ label: 'Minswap Order Contract v2', project: 'Minswap', category: 'DEX', url: 'https://minswap.org', source: 'strica' });
		expect(names.get(h('b'))?.label).toBe('Minswap');
		expect(names.get(h('f'))?.label).toBe('minswap batcher');
		expect(names.get(h('c'))?.project).toBe('Long Name');
		expect(names.size).toBe(4);
	});

	it('labels CRFA scripts and minting policies, and Strica wins on overlap', () => {
		const crfa = crfaNames(
			untarGz(
				tarGz({
					'r-main/dApps/x.json': JSON.stringify({
						projectName: 'Indigo',
						subCategory: 'SYNTHETICS',
						link: 'http://insecure.example',
						scripts: [
							{
								name: 'CDP',
								versions: [
									{ version: 1, scriptHash: h('d') },
									{ version: 2, scriptHash: h('a'), mintPolicyID: h('e') }
								]
							}
						]
					})
				})
			)
		);
		expect(crfa.get(h('d'))).toEqual({ label: 'Indigo CDP v1', project: 'Indigo', category: 'SYNTHETICS', url: undefined, source: 'crfa' });
		expect(crfa.get(h('e'))?.label).toBe('Indigo CDP v2');
		const merged = mergeScriptNames(stricaNames(strica), crfa);
		expect(merged[h('a')].source).toBe('strica');
		expect(merged[h('d')].source).toBe('crfa');
	});
});

describe('crfaScriptHash', () => {
	it('takes the script hash out of a whole address body', () => {
		expect(crfaScriptHash(h('a'))).toBe(h('a'));
		expect(crfaScriptHash(h('a') + h('b'))).toBe(h('a'));
		expect(crfaScriptHash('11' + h('a') + h('b'))).toBe(h('a'));
		expect(crfaScriptHash('71' + h('a'))).toBe(h('a'));
		expect(crfaScriptHash('xyz')).toBeNull();
	});
});

describe('cleanText', () => {
	it('keeps names short and plain', () => {
		expect(cleanText({ '@value': '  Ada\n Lovelace ' }, 60)).toBe('Ada Lovelace');
		expect(cleanText('evil‮name', 60)).toBe('evil name');
		expect(cleanText('x'.repeat(10), 5)).toBe('xxxx…');
		expect(cleanText(42, 5)).toBeUndefined();
		expect(cleanText('   ', 5)).toBeUndefined();
	});
});

describe('documents', () => {
	it('reads new and long-failed documents, keeps names, notes failures', async () => {
		let now = 1_000_000;
		const docs: Record<string, DocEntry<string>> = {
			a: { url: 'https://a', hash: '1' },
			b: { url: 'https://b', hash: '2', value: 'kept' },
			c: { url: 'https://c', hash: '3', failedAt: now - 10 },
			d: { url: 'https://d', hash: '4', failedAt: now - 10_000 },
			e: { url: 'https://e', hash: '5' }
		};
		const read = vi.fn(async (url: string): Promise<string | null> => (url === 'https://e' ? Promise.reject(new Error('down')) : url === 'https://d' ? null : `name of ${url}`));
		const r = await fillDocs(docs, read, { concurrency: 2, retryMs: 1000, now: () => now });
		expect(read.mock.calls.map((c) => c[0]).sort()).toEqual(['https://a', 'https://d', 'https://e']);
		expect(r).toEqual({ read: 3, named: 1 });
		expect(docs.a.value).toBe('name of https://a');
		expect(docs.b.value).toBe('kept');
		expect(docs.d.failedAt).toBe(now);
		expect(docs.e.failedAt).toBe(now);
		now += 1;
	});

	it('reads a document again when its url or hash changes, and drops ids no longer listed', () => {
		const docs: Record<string, DocEntry<string>> = { a: { url: 'u', hash: '1', value: 'A' }, b: { url: 'u', hash: '2', value: 'B' }, gone: { url: 'u', hash: '9', value: 'G' } };
		const out = syncDocs(
			docs,
			new Map([
				['a', { url: 'u', hash: '1' }],
				['b', { url: 'u', hash: 'changed' }],
				['new', { url: 'n', hash: '3' }]
			])
		);
		expect(out).toEqual({ a: { url: 'u', hash: '1', value: 'A' }, b: { url: 'u', hash: 'changed' }, new: { url: 'n', hash: '3' } });
	});
});

describe('token names', () => {
	const unit = (c: string) => c.repeat(56) + '4d494e';
	const answer = (subjects: string[]) =>
		new Response(JSON.stringify({ subjects: subjects.map((s) => ({ subject: s, ticker: { value: 'MIN' }, name: { value: 'Minswap' }, decimals: { value: 6 } })) }));

	it('reads the registry fields', () => {
		expect(tokenFromRegistry({ ticker: { value: 'MIN' }, decimals: { value: 6 } })).toEqual({ ticker: 'MIN', name: undefined, decimals: 6 });
		expect(tokenFromRegistry({ decimals: { value: 99 } })).toBeNull();
	});

	it('asks the registry once per token, batches, and remembers unknown tokens', async () => {
		let now = 0;
		const fetch = vi.fn(async (_url: string | URL | Request, init?: RequestInit) => {
			const { subjects } = JSON.parse(String(init?.body));
			return answer(subjects.filter((s: string) => s === unit('a')));
		});
		const t = new TokenNames({ fetch: fetch as unknown as typeof globalThis.fetch, now: () => now });
		const [first, second] = await Promise.all([t.get([unit('a'), unit('b')]), t.get([unit('a')])]);
		expect(fetch).toHaveBeenCalledTimes(1);
		expect(first).toEqual({ [unit('a')]: { ticker: 'MIN', name: 'Minswap', decimals: 6 } });
		expect(second).toEqual(first);
		await t.get([unit('a'), unit('b')]);
		expect(fetch).toHaveBeenCalledTimes(1);
		now += 25 * 60 * 60_000; // a day later: the unknown one is asked again, the known one is still fresh
		await t.get([unit('a'), unit('b')]);
		expect(fetch).toHaveBeenCalledTimes(2);
		expect(JSON.parse(String(fetch.mock.calls[1][1]?.body)).subjects).toEqual([unit('b')]);
	});

	it('gives up waiting on a slow registry and answers later', async () => {
		let release: (r: Response) => void = () => {};
		const fetch = vi.fn(() => new Promise<Response>((r) => (release = r)));
		const t = new TokenNames({ fetch: fetch as unknown as typeof globalThis.fetch });
		expect(await t.get([unit('a')], 10)).toEqual({});
		release(answer([unit('a')]));
		await new Promise((r) => setTimeout(r, 0));
		expect(await t.get([unit('a')], 10)).toEqual({ [unit('a')]: { ticker: 'MIN', name: 'Minswap', decimals: 6 } });
	});
});
