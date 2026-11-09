/*
 * Turn whatever someone pastes into the search box into places to go. Some inputs are certain from their shape
 * (bech32 prefixes); hex hashes are ambiguous, so they come back as several candidates that `resolve` checks.
 */

export type EntityKind =
	| 'block'
	| 'tx'
	| 'address'
	| 'stake'
	| 'pool'
	| 'drep'
	| 'asset'
	| 'policy'
	| 'fingerprint'
	| 'script'
	| 'datum'
	| 'epoch'
	| 'govaction';

export interface Candidate {
	kind: EntityKind;
	value: string;
	href: string;
	/** API path to confirm the entity exists, for ambiguous inputs. */
	check?: string;
	/** True when the shape alone is proof enough. */
	certain?: boolean;
}

export const KIND_LABEL: Record<EntityKind, string> = {
	block: 'Block',
	tx: 'Transaction',
	address: 'Address',
	stake: 'Stake account',
	pool: 'Stake pool',
	drep: 'DRep',
	asset: 'Asset',
	policy: 'Policy',
	fingerprint: 'Asset fingerprint',
	script: 'Script',
	datum: 'Datum',
	epoch: 'Epoch',
	govaction: 'Governance action'
};

const HEX = /^[0-9a-f]+$/i;

export function classify(raw: string): Candidate[] {
	const q = raw.trim().replace(/^["']|["']$/g, '');
	if (!q) return [];
	const lower = q.toLowerCase();

	// "epoch 512" / "e512"
	const epochMatch = lower.match(/^(?:epoch\s*|e)(\d{1,7})$/);
	if (epochMatch) return [{ kind: 'epoch', value: epochMatch[1], href: `/epochs/${epochMatch[1]}`, certain: true }];

	if (/^\d+$/.test(q)) {
		return [
			{ kind: 'block', value: q, href: `/blocks/${q}`, check: `/blocks/${q}` },
			{ kind: 'epoch', value: q, href: `/epochs/${q}`, check: `/epochs/${q}` }
		];
	}

	if (/^(addr1|addr_test1)[0-9a-z]+$/.test(lower) || /^(Ae2|DdzFF)[1-9A-HJ-NP-Za-km-z]+$/.test(q)) {
		return [{ kind: 'address', value: q, href: `/addresses/${q}`, certain: true }];
	}
	if (/^(stake1|stake_test1)[0-9a-z]+$/.test(lower)) {
		return [{ kind: 'stake', value: q, href: `/stake/${q}`, certain: true }];
	}
	if (/^pool1[0-9a-z]+$/.test(lower)) return [{ kind: 'pool', value: q, href: `/pools/${q}`, certain: true }];
	if (/^drep1[0-9a-z]+$/.test(lower) || /^drep_script1[0-9a-z]+$/.test(lower)) {
		return [{ kind: 'drep', value: q, href: `/governance/dreps/${q}`, certain: true }];
	}
	if (/^asset1[0-9a-z]+$/.test(lower)) {
		return [{ kind: 'fingerprint', value: q, href: `/assets/fingerprint/${q}`, certain: true }];
	}

	// <txhash>#<index>: a UTxO reference (go to the tx) or a governance action id.
	const ref = q.match(/^([0-9a-f]{64})[#_](\d+)$/i);
	if (ref) {
		const [, h, i] = ref;
		return [
			{ kind: 'tx', value: h.toLowerCase(), href: `/transactions/${h.toLowerCase()}`, check: `/txs/${h.toLowerCase()}` },
			{
				kind: 'govaction',
				value: q,
				href: `/governance/govactions/${h.toLowerCase()}_${i}`,
				check: `/governance/proposals/${h.toLowerCase()}`
			}
		];
	}

	if (HEX.test(q)) {
		const h = lower;
		if (h.length === 64) {
			return [
				{ kind: 'tx', value: h, href: `/transactions/${h}`, check: `/txs/${h}` },
				{ kind: 'block', value: h, href: `/blocks/${h}`, check: `/blocks/${h}` },
				{ kind: 'datum', value: h, href: `/datums/${h}`, check: `/scripts/datum/${h}` },
				// A policy id plus a 4-byte asset name is also 64 hex characters.
				{ kind: 'asset', value: h, href: `/assets/unit/${h}`, check: `/assets/${h}/supply` }
			];
		}
		if (h.length === 56) {
			return [
				{ kind: 'policy', value: h, href: `/assets/policy/${h}`, check: `/assets/policy/${h}/supply` },
				{ kind: 'script', value: h, href: `/scripts/${h}`, check: `/scripts/${h}` },
				{ kind: 'pool', value: h, href: `/pools/${h}` },
				{ kind: 'drep', value: h, href: `/governance/dreps/${h}`, check: `/governance-state/dreps/${h}` }
			];
		}
		if (h.length > 56 && h.length <= 120) {
			return [{ kind: 'asset', value: h, href: `/assets/unit/${h}`, check: `/assets/${h}/supply` }];
		}
	}
	return [];
}

/** Hint shown under the box while typing. */
export const SEARCH_HINT =
	'Block number or hash, transaction, address, stake address, pool, DRep, policy, asset or fingerprint, script, datum, or "epoch 12"';
