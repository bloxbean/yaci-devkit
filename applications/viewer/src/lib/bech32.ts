/* Minimal bech32 (BIP-173) decode/encode, enough to derive a stake address from a Shelley base address. */

const CHARSET = 'qpzry9x8gf2tvdw0s3jn54khce6mua7l';
const GEN = [0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3];

function polymod(values: number[]) {
	let chk = 1;
	for (const v of values) {
		const top = chk >> 25;
		chk = ((chk & 0x1ffffff) << 5) ^ v;
		for (let i = 0; i < 5; i++) if ((top >> i) & 1) chk ^= GEN[i];
	}
	return chk;
}

const hrpExpand = (hrp: string) => [...[...hrp].map((c) => c.charCodeAt(0) >> 5), 0, ...[...hrp].map((c) => c.charCodeAt(0) & 31)];

function convert(data: number[], from: number, to: number, pad: boolean): number[] | null {
	let acc = 0;
	let bits = 0;
	const out: number[] = [];
	const max = (1 << to) - 1;
	for (const v of data) {
		acc = (acc << from) | v;
		bits += from;
		while (bits >= to) {
			bits -= to;
			out.push((acc >> bits) & max);
		}
	}
	if (pad) {
		if (bits > 0) out.push((acc << (to - bits)) & max);
	} else if (bits >= from || ((acc << (to - bits)) & max)) {
		return null;
	}
	return out;
}

export function bech32Decode(s: string): { hrp: string; bytes: Uint8Array } | null {
	const str = s.toLowerCase();
	const pos = str.lastIndexOf('1');
	if (pos < 1 || pos + 7 > str.length) return null;
	const hrp = str.slice(0, pos);
	const data: number[] = [];
	for (const c of str.slice(pos + 1)) {
		const d = CHARSET.indexOf(c);
		if (d < 0) return null;
		data.push(d);
	}
	if (polymod([...hrpExpand(hrp), ...data]) !== 1) return null;
	const bytes = convert(data.slice(0, -6), 5, 8, false);
	return bytes ? { hrp, bytes: new Uint8Array(bytes) } : null;
}

export function bech32Encode(hrp: string, bytes: Uint8Array): string {
	const data = convert([...bytes], 8, 5, true)!;
	const values = [...hrpExpand(hrp), ...data, 0, 0, 0, 0, 0, 0];
	const mod = polymod(values) ^ 1;
	const checksum = Array.from({ length: 6 }, (_, i) => (mod >> (5 * (5 - i))) & 31);
	return `${hrp}1${[...data, ...checksum].map((d) => CHARSET[d]).join('')}`;
}

/** The stake address of a Shelley base address (types 0-3), or null. */
export function stakeAddressOf(address: string): string | null {
	const d = bech32Decode(address);
	if (!d || !d.hrp.startsWith('addr') || d.bytes.length !== 57) return null;
	const type = d.bytes[0] >> 4;
	if (type > 3) return null;
	const network = d.bytes[0] & 0x0f;
	const scriptStake = type === 2 || type === 3;
	const header = (scriptStake ? 0xf0 : 0xe0) | network;
	return bech32Encode(d.hrp === 'addr' ? 'stake' : 'stake_test', new Uint8Array([header, ...d.bytes.slice(29)]));
}
