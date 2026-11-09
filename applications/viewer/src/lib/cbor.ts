/*
 * A small decoder for Plutus data (datums, redeemers) from CBOR hex, into the "detailed schema" JSON that
 * cardano-cli and most tools use: { constructor, fields } | { int } | { bytes } | { list } | { map }.
 */

export type PlutusData =
	| { constructor: number; fields: PlutusData[] }
	| { int: string }
	| { bytes: string }
	| { list: PlutusData[] }
	| { map: { k: PlutusData; v: PlutusData }[] };

class Reader {
	pos = 0;
	constructor(private b: Uint8Array) {}

	byte() {
		if (this.pos >= this.b.length) throw new Error('Unexpected end of CBOR');
		return this.b[this.pos++];
	}

	/** Reads a head: major type and argument (as bigint; null for indefinite length). */
	head(): { major: number; arg: bigint | null; info: number } {
		const ib = this.byte();
		const major = ib >> 5;
		const info = ib & 0x1f;
		if (info < 24) return { major, arg: BigInt(info), info };
		if (info === 31) return { major, arg: null, info };
		const n = info === 24 ? 1 : info === 25 ? 2 : info === 26 ? 4 : info === 27 ? 8 : -1;
		if (n < 0) throw new Error(`Bad CBOR additional info ${info}`);
		let v = 0n;
		for (let i = 0; i < n; i++) v = (v << 8n) | BigInt(this.byte());
		return { major, arg: v, info };
	}

	bytes(n: number) {
		if (this.pos + n > this.b.length) throw new Error('Unexpected end of CBOR');
		const out = this.b.slice(this.pos, this.pos + n);
		this.pos += n;
		return out;
	}

	isBreak() {
		return this.b[this.pos] === 0xff;
	}
}

const toHex = (u: Uint8Array) => Array.from(u, (x) => x.toString(16).padStart(2, '0')).join('');

function bigFromBytes(u: Uint8Array) {
	let v = 0n;
	for (const x of u) v = (v << 8n) | BigInt(x);
	return v;
}

function readByteString(r: Reader, arg: bigint | null): Uint8Array {
	if (arg !== null) return r.bytes(Number(arg));
	// Indefinite: chunks until break (Plutus splits bytes over 64 long).
	const parts: Uint8Array[] = [];
	while (!r.isBreak()) {
		const h = r.head();
		if (h.major !== 2 || h.arg === null) throw new Error('Bad bytes chunk');
		parts.push(r.bytes(Number(h.arg)));
	}
	r.byte();
	const out = new Uint8Array(parts.reduce((s, p) => s + p.length, 0));
	let o = 0;
	for (const p of parts) {
		out.set(p, o);
		o += p.length;
	}
	return out;
}

function readItems(r: Reader, arg: bigint | null, each: () => void) {
	if (arg === null) {
		while (!r.isBreak()) each();
		r.byte();
	} else {
		for (let i = 0n; i < arg; i++) each();
	}
}

function read(r: Reader): PlutusData {
	const { major, arg } = r.head();
	switch (major) {
		case 0:
			return { int: String(arg) };
		case 1:
			return { int: String(-1n - (arg ?? 0n)) };
		case 2:
			return { bytes: toHex(readByteString(r, arg)) };
		case 4: {
			const list: PlutusData[] = [];
			readItems(r, arg, () => list.push(read(r)));
			return { list };
		}
		case 5: {
			const map: { k: PlutusData; v: PlutusData }[] = [];
			readItems(r, arg, () => map.push({ k: read(r), v: read(r) }));
			return { map };
		}
		case 6: {
			const tag = Number(arg);
			// Constructors 0-6 → tags 121-127; 7-127 → 1280-1400; any other via tag 102 [n, fields].
			if (tag >= 121 && tag <= 127) return { constructor: tag - 121, fields: listFields(read(r)) };
			if (tag >= 1280 && tag <= 1400) return { constructor: tag - 1280 + 7, fields: listFields(read(r)) };
			if (tag === 102) {
				const inner = read(r);
				if (!('list' in inner) || inner.list.length !== 2 || !('int' in inner.list[0])) throw new Error('Bad tag 102');
				return { constructor: Number(inner.list[0].int), fields: listFields(inner.list[1]) };
			}
			if (tag === 2 || tag === 3) {
				const inner = read(r);
				if (!('bytes' in inner)) throw new Error('Bad bignum');
				const v = bigFromBytes(new Uint8Array(inner.bytes.match(/../g)?.map((h) => parseInt(h, 16)) ?? []));
				return { int: String(tag === 2 ? v : -1n - v) };
			}
			throw new Error(`Unsupported CBOR tag ${tag}`);
		}
		default:
			throw new Error(`Not Plutus data (CBOR major type ${major})`);
	}
}

function listFields(d: PlutusData): PlutusData[] {
	if (!('list' in d)) throw new Error('Constructor fields must be a list');
	return d.list;
}

export function decodePlutusData(hex: string): PlutusData {
	const clean = hex.trim().toLowerCase();
	if (!/^([0-9a-f]{2})+$/.test(clean)) throw new Error('Not hex');
	const r = new Reader(new Uint8Array(clean.match(/../g)!.map((h) => parseInt(h, 16))));
	const out = read(r);
	return out;
}

export function tryDecodePlutusData(hex: string | null | undefined): PlutusData | null {
	if (!hex) return null;
	try {
		return decodePlutusData(hex);
	} catch {
		return null;
	}
}

// Not `'constructor' in d`: every object inherits a `constructor` property.
const isConstr = (d: PlutusData): d is { constructor: number; fields: PlutusData[] } => typeof (d as { constructor: unknown }).constructor === 'number';

/*
 * Encode Plutus data to CBOR hex the way the Plutus libraries do: indefinite-length lists (definite when empty),
 * byte strings over 64 bytes in 64-byte chunks, integers beyond 64 bits as bignums, and constructor tags
 * 121-127 / 1280-1400 / 102. Used to hand a modified ScriptContext back to the julc VM.
 */
export function encodePlutusData(d: PlutusData): string {
	const out: number[] = [];
	const head = (major: number, n: bigint) => {
		const m = major << 5;
		if (n < 24n) out.push(m | Number(n));
		else if (n < 0x100n) out.push(m | 24, Number(n));
		else if (n < 0x10000n) out.push(m | 25, Number(n >> 8n), Number(n & 0xffn));
		else if (n < 0x100000000n) out.push(m | 26, ...[24n, 16n, 8n, 0n].map((s) => Number((n >> s) & 0xffn)));
		else out.push(m | 27, ...[56n, 48n, 40n, 32n, 24n, 16n, 8n, 0n].map((s) => Number((n >> s) & 0xffn)));
	};
	const bytes = (b: number[]) => {
		if (b.length <= 64) {
			head(2, BigInt(b.length));
			out.push(...b);
			return;
		}
		out.push(0x5f);
		for (let i = 0; i < b.length; i += 64) {
			const chunk = b.slice(i, i + 64);
			head(2, BigInt(chunk.length));
			out.push(...chunk);
		}
		out.push(0xff);
	};
	const list = (items: PlutusData[]) => {
		if (!items.length) return out.push(0x80);
		out.push(0x9f);
		items.forEach(write);
		out.push(0xff);
	};
	const write = (x: PlutusData): void => {
		if (isConstr(x)) {
			const c = x.constructor;
			if (c <= 6) head(6, BigInt(121 + c));
			else if (c <= 127) head(6, BigInt(1280 + c - 7));
			else {
				head(6, 102n);
				out.push(0x82);
				head(0, BigInt(c));
			}
			list(x.fields);
		} else if ('int' in x) {
			const v = BigInt(x.int);
			const n = v < 0n ? -1n - v : v;
			if (n < 1n << 64n) head(v < 0n ? 1 : 0, n);
			else {
				head(6, v < 0n ? 3n : 2n);
				const hex = n.toString(16);
				bytes((hex.length % 2 ? '0' + hex : hex).match(/../g)!.map((h) => parseInt(h, 16)));
			}
		} else if ('bytes' in x) bytes((x.bytes.match(/../g) ?? []).map((h) => parseInt(h, 16)));
		else if ('list' in x) list(x.list);
		else {
			head(5, BigInt(x.map.length));
			for (const { k, v } of x.map) {
				write(k);
				write(v);
			}
		}
	};
	write(d);
	return out.map((b) => b.toString(16).padStart(2, '0')).join('');
}
