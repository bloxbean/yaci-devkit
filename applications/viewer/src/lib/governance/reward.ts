import { bech32Encode } from '$lib/bech32';

/** A reward address in hex (header e0/e1/f0/f1 + 28-byte hash) → stake1… / stake_test1…, or null. */
export function rewardHexToStake(hex: string): string | null {
	if (!/^(e0|e1|f0|f1)[0-9a-f]{56}$/i.test(hex)) return null;
	const bytes = new Uint8Array(hex.match(/../g)!.map((b) => parseInt(b, 16)));
	return bech32Encode((bytes[0] & 0x0f) === 1 ? 'stake' : 'stake_test', bytes);
}
