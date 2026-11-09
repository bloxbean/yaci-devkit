import { describe, expect, it } from 'vitest';
import { untar } from './untar.mjs';

/* Build a tar archive in memory: entries of [name, body, typeflag]. */
function tar(entries: [string, string, string?][]): Buffer {
	const blocks: Buffer[] = [];
	for (const [name, body, type = '0'] of entries) {
		const header = Buffer.alloc(512);
		header.write(name, 0, 100, 'utf8');
		header.write(body.length.toString(8).padStart(11, '0') + '\0', 124, 12, 'utf8');
		header.write(type, 156, 1, 'utf8');
		blocks.push(header);
		const data = Buffer.alloc(Math.ceil(body.length / 512) * 512);
		data.write(body, 0, 'utf8');
		blocks.push(data);
	}
	blocks.push(Buffer.alloc(1024));
	return Buffer.concat(blocks);
}

describe('untar', () => {
	it('returns top-level regular files', () => {
		const files = untar(tar([['./', '', '5'], ['./engine.json', '{"a":1}'], ['julc-x.js', 'x'.repeat(700)]]));
		expect([...files.keys()]).toEqual(['engine.json', 'julc-x.js']);
		expect(files.get('engine.json')!.toString()).toBe('{"a":1}');
		expect(files.get('julc-x.js')!.length).toBe(700);
	});

	it('skips paths that could escape the target folder, links and pax headers', () => {
		const files = untar(
			tar([
				['../evil.js', 'x'],
				['/etc/passwd', 'x'],
				['sub/dir.js', 'x'],
				['.hidden', 'x'],
				['link.js', '', '2'],
				['./PaxHeaders/x', 'path=a', 'x'],
				['ok.js', 'y']
			])
		);
		expect([...files.keys()]).toEqual(['ok.js']);
	});

	it('rejects a truncated archive', () => {
		const t = tar([['a.js', 'x'.repeat(600)]]);
		expect(() => untar(t.subarray(0, 700))).toThrow(/truncated/);
	});
});
