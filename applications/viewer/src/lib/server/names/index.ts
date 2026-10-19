import { env } from '$env/dynamic/private';
import { api, tryApi } from '$lib/api/client';
import { KEY } from '$lib/names/keys';
import type { NamesQuery, NamesResult, PoolName, ScriptName } from '$lib/names/types';
import { actionTitle, drepName, fillDocs, poolName, syncDocs, type DocEntry } from './docs';
import { loadJson, saveJson } from './persist';
import { REGISTRIES, crfaNames, mergeScriptNames, stricaNames, type RegistrySource } from './registries';
import { untarGz } from './tar';
import { TokenNames, type TokenEntry } from './tokens';

/*
 * Names for ids, on Mainnet only. On any other network (the devkit, testnets), or when the store can't say which
 * network it is, nothing is fetched and every lookup is empty.
 *
 * In the background, after the server starts:
 *  - dApp labels for script hashes from the Strica and Cardano Fans registries (daily);
 *  - governance action titles and DRep names from their anchor documents (every 6 hours);
 *  - pool tickers from pool metadata, the registrations list read from the newest down to what was seen last time
 *    (every 12 hours; the first run reads all of it, a few minutes);
 *  - token tickers and decimals only when a page asks (tokens.ts).
 * Each kind is saved as a JSON file in NAMES_DIR, so after a restart the names are there at once and only what
 * changed on chain is read again. A document that couldn't be read is tried again a day (pools: three days) later.
 *
 * NAMES=off turns all of it off.
 */

const MAINNET_MAGIC = 764824073;
const HOUR = 60 * 60_000;

export const NAMES = {
	tickMs: 15 * 60_000,
	every: { scripts: 24 * HOUR, actions: 6 * HOUR, dreps: 6 * HOUR, pools: 12 * HOUR },
	retry: { actions: 24 * HOUR, dreps: 24 * HOUR, pools: 72 * HOUR },
	concurrency: { actions: 4, dreps: 6, pools: 8 },
	maxPages: 1000
} as const;

type Docs<T> = Record<string, DocEntry<T>>;
interface Saved<T> {
	version: 1;
	magic: number;
	at: number;
	data: T;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

const sleep = (ms: number) => new Promise<void>((r) => setTimeout(r, ms).unref?.());
const dir = () => env.NAMES_DIR || '.cache/names';

async function pages<T>(path: string, params: Record<string, string | number> = {}, stop?: (rows: T[]) => boolean): Promise<T[]> {
	const all: T[] = [];
	for (let page = 1; page <= NAMES.maxPages; page++) {
		const rows = await api<T[]>(path, { params: { ...params, count: 100, page }, timeoutMs: 60_000 });
		if (!Array.isArray(rows)) break;
		all.push(...rows);
		if (rows.length < 100 || stop?.(rows)) break;
	}
	return all;
}

async function archive(url: string): Promise<Uint8Array> {
	const res = await fetch(url, { signal: AbortSignal.timeout(60_000) });
	if (!res.ok) throw new Error(`HTTP ${res.status}`);
	if (Number(res.headers.get('content-length') ?? 0) > 20 * 1024 * 1024) throw new Error('archive too large');
	return new Uint8Array(await res.arrayBuffer());
}

class NameBook {
	/** null until the store has said which network it is. */
	mainnet: boolean | null = null;
	scripts: Saved<Record<string, ScriptName>> = { version: 1, magic: MAINNET_MAGIC, at: 0, data: {} };
	actions: Saved<Docs<string>> = { version: 1, magic: MAINNET_MAGIC, at: 0, data: {} };
	dreps: Saved<Docs<string>> = { version: 1, magic: MAINNET_MAGIC, at: 0, data: {} };
	pools: Saved<{ lastBlock: number; docs: Docs<PoolName> }> = { version: 1, magic: MAINNET_MAGIC, at: 0, data: { lastBlock: 0, docs: {} } };
	tokens = new TokenNames({ fetch, onChange: () => this.saveTokensSoon() });
	private started = false;
	private tokenSave: ReturnType<typeof setTimeout> | null = null;

	start() {
		if (this.started) return;
		this.started = true;
		if (env.NAMES === 'off') return;
		void this.run();
	}

	private async run() {
		while (this.mainnet === null) {
			this.mainnet = await this.network();
			if (this.mainnet === null) await sleep(10 * 60_000);
		}
		if (!this.mainnet) {
			console.info('[names] not Mainnet: no dApp, DRep, pool, governance or token names');
			return;
		}
		await this.load();
		for (;;) {
			await this.refreshDue();
			await sleep(NAMES.tickMs);
		}
	}

	private async network(): Promise<boolean | null> {
		const g = (await tryApi<Any>('/genesis', { timeoutMs: 15_000 })) ?? (await tryApi<Any>('/blockfrost/genesis', { timeoutMs: 15_000 }));
		const magic = Number(g?.network_magic);
		return Number.isFinite(magic) && g?.network_magic != null ? magic === MAINNET_MAGIC : null;
	}

	private async load() {
		const fit = <T>(s: Saved<T> | null) => (s?.version === 1 && s.magic === MAINNET_MAGIC ? s : null);
		this.scripts = fit(await loadJson<Saved<Record<string, ScriptName>>>(dir(), 'scripts')) ?? this.scripts;
		this.actions = fit(await loadJson<Saved<Docs<string>>>(dir(), 'actions')) ?? this.actions;
		this.dreps = fit(await loadJson<Saved<Docs<string>>>(dir(), 'dreps')) ?? this.dreps;
		this.pools = fit(await loadJson<typeof this.pools>(dir(), 'pools')) ?? this.pools;
		const tokens = fit(await loadJson<Saved<Record<string, TokenEntry>>>(dir(), 'tokens'));
		for (const [u, e] of Object.entries(tokens?.data ?? {})) this.tokens.entries.set(u, e);
		const named = <T>(d: Docs<T>) => Object.values(d).filter((e) => e.value !== undefined).length;
		console.info(
			`[names] loaded: ${Object.keys(this.scripts.data).length} script labels, ${named(this.dreps.data)} DRep names, ` +
				`${named(this.pools.data.docs)} pool tickers, ${named(this.actions.data)} action titles, ${this.tokens.entries.size} tokens`
		);
	}

	private async refreshDue() {
		const now = Date.now();
		const tasks: [keyof typeof NAMES.every, () => Promise<void>][] = [
			['scripts', () => this.refreshScripts()],
			['actions', () => this.refreshActions()],
			['dreps', () => this.refreshDreps()],
			['pools', () => this.refreshPools()]
		];
		for (const [kind, task] of tasks) {
			if (now - this[kind].at < NAMES.every[kind]) continue;
			try {
				await task();
			} catch (e) {
				// The store or a registry is down: tried again on the next tick.
				console.warn(`[names] ${kind}: ${(e as Error).message}`);
			}
		}
	}

	private async refreshScripts() {
		const kept = (source: RegistrySource) => new Map(Object.entries(this.scripts.data).filter(([, v]) => v.source === source));
		const read = async (i: number, parse: typeof stricaNames) => {
			try {
				return parse(untarGz(await archive(REGISTRIES[i].archive)));
			} catch (e) {
				console.warn(`[names] ${REGISTRIES[i].source} registry: ${(e as Error).message}`);
				return null;
			}
		};
		const [strica, crfa] = await Promise.all([read(0, stricaNames), read(1, crfaNames)]);
		if (!strica && !crfa) throw new Error('no registry could be read');
		this.scripts = { ...this.scripts, at: Date.now(), data: mergeScriptNames(strica ?? kept('strica'), crfa ?? kept('crfa')) };
		await saveJson(dir(), 'scripts', this.scripts);
		console.info(`[names] scripts: ${Object.keys(this.scripts.data).length} labels (Strica ${strica?.size ?? 'kept'}, Cardano Fans ${crfa?.size ?? 'kept'})`);
	}

	private async refreshActions() {
		const rows = await pages<Any>('/governance/proposals');
		const latest = new Map<string, { url: string; hash: string }>();
		for (const r of rows) {
			const key = KEY.actions(`${r.tx_hash}#${r.index}`);
			if (key && r.anchor_url && r.anchor_hash) latest.set(key, { url: r.anchor_url, hash: r.anchor_hash });
		}
		await this.fill('actions', syncDocs(this.actions.data, latest), actionTitle);
	}

	private async refreshDreps() {
		const rows = await pages<Any>('/blockfrost/governance/dreps');
		const latest = new Map<string, { url: string; hash: string }>();
		for (const r of rows) {
			const key = KEY.dreps(r.drep_id);
			if (key && r.metadata?.url && r.metadata?.hash) latest.set(key, { url: r.metadata.url, hash: r.metadata.hash });
		}
		await this.fill('dreps', syncDocs(this.dreps.data, latest), drepName);
	}

	/** Registrations newest first, down to the last block seen; each pool's newest registration wins. */
	private async refreshPools() {
		const last = this.pools.data.lastBlock;
		const rows = await pages<Any>('/pools/registrations', { order: 'desc' }, (rows) => rows.every((r) => Number(r.block_number) <= last));
		const latest = new Map(Object.entries(this.pools.data.docs).map(([k, e]) => [k, { url: e.url, hash: e.hash }]));
		const seen = new Set<string>();
		let top = last;
		for (const r of rows) {
			const key = KEY.pools(r.pool_id);
			const block = Number(r.block_number);
			if (!key || seen.has(key) || block <= last) continue;
			seen.add(key);
			top = Math.max(top, block);
			if (r.metadata_url && r.metadata_hash) latest.set(key, { url: r.metadata_url, hash: r.metadata_hash });
			else latest.delete(key);
		}
		this.pools.data.lastBlock = top;
		const docs = syncDocs(this.pools.data.docs, latest);
		this.pools.data.docs = docs;
		await this.fill('pools', docs, poolName);
	}

	private async fill<T>(kind: 'actions' | 'dreps' | 'pools', docs: Docs<T>, read: (url: string, hash: string) => Promise<T | null>) {
		const saved = this[kind] as Saved<unknown>;
		if (kind === 'pools') this.pools.data.docs = docs as Docs<PoolName>;
		else saved.data = docs;
		const save = () => saveJson(dir(), kind, saved);
		await save();
		const r = await fillDocs(docs, read, { concurrency: NAMES.concurrency[kind], retryMs: NAMES.retry[kind], onProgress: save });
		saved.at = Date.now();
		await save();
		const total = Object.keys(docs).length;
		const named = Object.values(docs).filter((e) => e.value !== undefined).length;
		console.info(`[names] ${kind}: ${named} of ${total} named (read ${r.read} documents now, ${r.named} gave a name)`);
	}

	private saveTokensSoon() {
		if (this.tokenSave) return;
		this.tokenSave = setTimeout(() => {
			this.tokenSave = null;
			void saveJson(dir(), 'tokens', { version: 1, magic: MAINNET_MAGIC, at: Date.now(), data: Object.fromEntries(this.tokens.entries) });
		}, 30_000);
		this.tokenSave.unref?.();
	}

	/** The names that are known for these keys (already normalised, see keys.ts). Empty off Mainnet. */
	async lookup(q: Required<NamesQuery>): Promise<NamesResult> {
		const enabled = env.NAMES === 'off' ? false : this.mainnet;
		const out: NamesResult = { enabled, scripts: {}, dreps: {}, pools: {}, actions: {}, tokens: {} };
		if (!enabled) return out;
		for (const k of q.scripts) if (this.scripts.data[k]) out.scripts[k] = this.scripts.data[k];
		for (const k of q.dreps) if (this.dreps.data[k]?.value) out.dreps[k] = this.dreps.data[k].value!;
		for (const k of q.pools) if (this.pools.data.docs[k]?.value) out.pools[k] = this.pools.data.docs[k].value!;
		for (const k of q.actions) if (this.actions.data[k]?.value) out.actions[k] = this.actions.data[k].value!;
		if (q.tokens.length) out.tokens = await this.tokens.get(q.tokens);
		return out;
	}
}

export const names = new NameBook();
