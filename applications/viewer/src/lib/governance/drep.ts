import { bech32Decode, bech32Encode } from '$lib/bech32';

/*
 * DRep ids. Yaci Store uses CIP-129 ids (drep1 + a header byte: 0x22 key, 0x23 script). Votes and certificates
 * sometimes only carry the 28-byte hash, and older tools use CIP-105 ids (drep1 / drep_script1 without a header).
 */

const hexToBytes = (h: string) => new Uint8Array(h.match(/../g)!.map((b) => parseInt(b, 16)));

export function drepIdFromHash(hash: string, script = false): string | null {
	if (!/^[0-9a-f]{56}$/i.test(hash)) return null;
	return bech32Encode('drep', new Uint8Array([script ? 0x23 : 0x22, ...hexToBytes(hash)]));
}

/** Candidate CIP-129 ids for whatever was typed or linked: a CIP-129 id, a CIP-105 id, or a hex hash. */
export function drepIdCandidates(input: string): string[] {
	const s = input.trim();
	if (/^[0-9a-f]{56}$/i.test(s)) return [drepIdFromHash(s)!, drepIdFromHash(s, true)!];
	const d = bech32Decode(s);
	if (!d) return [];
	if ((d.hrp === 'drep' || d.hrp === 'drep_script') && d.bytes.length === 28) {
		const hex = Array.from(d.bytes, (b) => b.toString(16).padStart(2, '0')).join('');
		return [drepIdFromHash(hex, d.hrp === 'drep_script')!];
	}
	if (d.hrp === 'drep' && d.bytes.length === 29) return [s.toLowerCase()];
	return [];
}

export function drepHrefFor(voterType: string | undefined, hash: string, drepId?: string | null): string {
	return `/governance/dreps/${drepId ?? drepIdFromHash(hash, (voterType ?? '').includes('SCRIPT')) ?? hash}`;
}
