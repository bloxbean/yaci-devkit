import { gunzipSync } from 'node:zlib';

/*
 * The files in a .tar.gz, enough for GitHub's repository archives: plain files, long names from pax headers ("x")
 * and the ustar prefix field. Everything else (directories, links, the global pax header) is skipped.
 */

export interface TarFile {
	path: string;
	data: Uint8Array;
}

const text = (b: Uint8Array, start: number, len: number) => {
	const s = new TextDecoder().decode(b.subarray(start, start + len));
	const nul = s.indexOf('\0');
	return nul < 0 ? s : s.slice(0, nul);
};

/** Values of a pax extended header ("<len> key=value\n" records). */
function paxValues(data: Uint8Array): Record<string, string> {
	const out: Record<string, string> = {};
	const s = new TextDecoder().decode(data);
	let i = 0;
	while (i < s.length) {
		const space = s.indexOf(' ', i);
		const len = parseInt(s.slice(i, space), 10);
		if (!(len > 0) || space < 0) break;
		const record = s.slice(space + 1, i + len - 1);
		const eq = record.indexOf('=');
		if (eq > 0) out[record.slice(0, eq)] = record.slice(eq + 1);
		i += len;
	}
	return out;
}

export function untarGz(gz: Uint8Array, maxBytes = 32 * 1024 * 1024): TarFile[] {
	const tar = new Uint8Array(gunzipSync(gz, { maxOutputLength: maxBytes }));
	const files: TarFile[] = [];
	let nextPath: string | null = null;
	for (let off = 0; off + 512 <= tar.length; ) {
		const header = tar.subarray(off, off + 512);
		if (header.every((b) => b === 0)) break;
		const size = parseInt(text(header, 124, 12).trim() || '0', 8);
		const type = String.fromCharCode(header[156] || 48);
		const data = tar.subarray(off + 512, off + 512 + size);
		off += 512 + Math.ceil(size / 512) * 512;
		if (type === 'x') {
			nextPath = paxValues(data).path ?? null;
			continue;
		}
		if (type === 'g') continue;
		const prefix = text(header, 345, 155);
		const name = text(header, 0, 100);
		const path = nextPath ?? (prefix ? `${prefix}/${name}` : name);
		nextPath = null;
		if (type === '0' || type === '\0') files.push({ path, data });
	}
	return files;
}
