/*
 * A general CBOR reader for Cardano transactions. Unlike $lib/cbor (Plutus data only), it reads every major type and
 * keeps each item's byte range, so hashes can be taken over the exact bytes (transaction body, datums, scripts) and
 * a CBOR view can show what each field encodes.
 */

export type CborItem =
	| { type: 'uint' | 'nint'; value: bigint; start: number; end: number }
	| { type: 'bytes'; value: Uint8Array; start: number; end: number; indefinite?: boolean }
	| { type: 'text'; value: string; start: number; end: number }
	| { type: 'array'; items: CborItem[]; start: number; end: number; indefinite?: boolean }
	| { type: 'map'; entries: { k: CborItem; v: CborItem }[]; start: number; end: number; indefinite?: boolean }
	| { type: 'tag'; tag: number; item: CborItem; start: number; end: number }
	| { type: 'simple'; value: boolean | null | undefined | number; start: number; end: number }
	| { type: 'float'; value: number; start: number; end: number };

export const hexToBytes = (hex: string) => {
	const clean = hex.trim().toLowerCase();
	if (!/^([0-9a-f]{2})*$/.test(clean)) throw new Error('Not hex');
	return new Uint8Array((clean.match(/../g) ?? []).map((h) => parseInt(h, 16)));
};
export const bytesToHex = (b: Uint8Array) => Array.from(b, (x) => x.toString(16).padStart(2, '0')).join('');

class Reader {
	pos = 0;
	constructor(readonly b: Uint8Array) {}
	byte() {
		if (this.pos >= this.b.length) throw new Error(`Unexpected end of CBOR at byte ${this.pos}`);
		return this.b[this.pos++];
	}
	take(n: number) {
		if (this.pos + n > this.b.length) throw new Error(`Unexpected end of CBOR at byte ${this.pos}`);
		const out = this.b.subarray(this.pos, this.pos + n);
		this.pos += n;
		return out;
	}
	head() {
		const ib = this.byte();
		const major = ib >> 5;
		const info = ib & 0x1f;
		if (info < 24) return { major, info, arg: BigInt(info) as bigint | null };
		if (info === 31) return { major, info, arg: null };
		const n = info === 24 ? 1 : info === 25 ? 2 : info === 26 ? 4 : info === 27 ? 8 : -1;
		if (n < 0) throw new Error(`Bad CBOR additional info ${info} at byte ${this.pos - 1}`);
		let v = 0n;
		for (const x of this.take(n)) v = (v << 8n) | BigInt(x);
		return { major, info, arg: v as bigint | null };
	}
}

function halfFloat(h: number) {
	const exp = (h >> 10) & 0x1f;
	const mant = h & 0x3ff;
	const v = exp === 0 ? mant * 2 ** -24 : exp === 31 ? (mant ? NaN : Infinity) : (mant + 1024) * 2 ** (exp - 25);
	return h & 0x8000 ? -v : v;
}

function read(r: Reader, depth: number): CborItem {
	if (depth > 256) throw new Error('CBOR nested too deeply');
	const start = r.pos;
	const { major, info, arg } = r.head();
	const count = (each: () => void) => {
		if (arg === null) {
			while (r.b[r.pos] !== 0xff) each();
			r.pos++;
		} else for (let i = 0n; i < arg; i++) each();
	};
	switch (major) {
		case 0:
			return { type: 'uint', value: arg ?? 0n, start, end: r.pos };
		case 1:
			return { type: 'nint', value: -1n - (arg ?? 0n), start, end: r.pos };
		case 2:
		case 3: {
			let bytes: Uint8Array;
			if (arg !== null) bytes = r.take(Number(arg)).slice();
			else {
				const parts: Uint8Array[] = [];
				count(() => {
					const h = r.head();
					if (h.major !== major || h.arg === null) throw new Error('Bad chunk in an indefinite string');
					parts.push(r.take(Number(h.arg)));
				});
				bytes = new Uint8Array(parts.reduce((s, p) => s + p.length, 0));
				let o = 0;
				for (const p of parts) (bytes.set(p, o), (o += p.length));
			}
			return major === 2 ? { type: 'bytes', value: bytes, start, end: r.pos, ...(arg === null ? { indefinite: true } : {}) } : { type: 'text', value: new TextDecoder().decode(bytes), start, end: r.pos };
		}
		case 4: {
			const items: CborItem[] = [];
			count(() => items.push(read(r, depth + 1)));
			return { type: 'array', items, start, end: r.pos, ...(arg === null ? { indefinite: true } : {}) };
		}
		case 5: {
			const entries: { k: CborItem; v: CborItem }[] = [];
			count(() => entries.push({ k: read(r, depth + 1), v: read(r, depth + 1) }));
			return { type: 'map', entries, start, end: r.pos, ...(arg === null ? { indefinite: true } : {}) };
		}
		case 6: {
			const item = read(r, depth + 1);
			return { type: 'tag', tag: Number(arg), item, start, end: r.pos };
		}
		default: {
			if (info === 20 || info === 21) return { type: 'simple', value: info === 21, start, end: r.pos };
			if (info === 22) return { type: 'simple', value: null, start, end: r.pos };
			if (info === 23) return { type: 'simple', value: undefined, start, end: r.pos };
			if (info === 25) return { type: 'float', value: halfFloat(Number(arg)), start, end: r.pos };
			if (info === 26 || info === 27) {
				const dv = new DataView(new ArrayBuffer(8));
				if (info === 26) dv.setUint32(0, Number(arg));
				else dv.setBigUint64(0, arg ?? 0n);
				return { type: 'float', value: info === 26 ? dv.getFloat32(0) : dv.getFloat64(0), start, end: r.pos };
			}
			return { type: 'simple', value: Number(arg), start, end: r.pos };
		}
	}
}

/** Decode one CBOR item; `trailing` says how many bytes follow it. */
export function decodeCbor(bytes: Uint8Array): { item: CborItem; trailing: number } {
	const r = new Reader(bytes);
	const item = read(r, 0);
	return { item, trailing: bytes.length - r.pos };
}

/** The exact bytes of an item, as they appear in the input. */
export const rawOf = (bytes: Uint8Array, item: CborItem) => bytes.subarray(item.start, item.end);

/** Skip a set's tag 258 (Conway sets may be tagged). */
export const untag = (item: CborItem | undefined, tag = 258) => (item?.type === 'tag' && item.tag === tag ? item.item : item);
