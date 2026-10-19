import type { TokenName } from '$lib/names/types';
import { cleanText } from './clean';

/*
 * Token tickers and decimals from the Cardano Foundation's token registry (CIP-26), looked up when a page shows a
 * token, not in bulk: there are far more tokens than pages ever show. The batch endpoint of the v1 API answers
 * only the asked properties (the v2 one always includes the logo, often tens of KB per token). Answers are kept for
 * a week, "not registered" for a day; a token already being looked up isn't asked for twice.
 */

export const TOKEN_REGISTRY = 'https://tokens.cardano.org/metadata/query';

const KNOWN_MS = 7 * 24 * 60 * 60_000;
const UNKNOWN_MS = 24 * 60 * 60_000;
const MAX_ENTRIES = 20_000;
const BATCH = 100;

export interface TokenEntry {
	value: TokenName | null;
	at: number;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

/** One registry answer → the fields shown (null when it has none of them). */
export function tokenFromRegistry(s: Any): TokenName | null {
	const ticker = cleanText(s?.ticker?.value, 12);
	const name = cleanText(s?.name?.value, 50);
	const d = Number(s?.decimals?.value);
	const decimals = Number.isInteger(d) && d >= 0 && d <= 19 ? d : undefined;
	return ticker || name || decimals ? { ticker, name, decimals } : null;
}

export class TokenNames {
	entries = new Map<string, TokenEntry>();
	private pending = new Map<string, Promise<void>>();

	constructor(private opts: { fetch: typeof fetch; now?: () => number; timeoutMs?: number; onChange?: () => void } = { fetch }) {}

	private now() {
		return (this.opts.now ?? Date.now)();
	}

	private fresh(e: TokenEntry | undefined) {
		return !!e && this.now() - e.at < (e.value ? KNOWN_MS : UNKNOWN_MS);
	}

	/** Names for these units: what is known now, after asking the registry for the rest (at most `waitMs`). */
	async get(units: string[], waitMs = 3000): Promise<Record<string, TokenName>> {
		const ask = units.filter((u) => !this.fresh(this.entries.get(u)) && !this.pending.has(u));
		for (let i = 0; i < ask.length; i += BATCH) void this.load(ask.slice(i, i + BATCH));
		const waiting = [...new Set(units.map((u) => this.pending.get(u)).filter(Boolean))];
		if (waiting.length) await Promise.race([Promise.allSettled(waiting), new Promise((r) => setTimeout(r, waitMs))]);
		const out: Record<string, TokenName> = {};
		for (const u of units) {
			const v = this.entries.get(u)?.value;
			if (v) out[u] = v;
		}
		return out;
	}

	private load(units: string[]): Promise<void> {
		const p = (async () => {
			try {
				const res = await this.opts.fetch(TOKEN_REGISTRY, {
					method: 'POST',
					headers: { 'content-type': 'application/json', accept: 'application/json' },
					body: JSON.stringify({ subjects: units, properties: ['name', 'ticker', 'decimals'] }),
					signal: AbortSignal.timeout(this.opts.timeoutMs ?? 8000)
				});
				if (!res.ok) return;
				const j = await res.json();
				const found = new Map<string, TokenName | null>();
				for (const s of Array.isArray(j?.subjects) ? j.subjects : []) {
					if (typeof s?.subject === 'string') found.set(s.subject.toLowerCase(), tokenFromRegistry(s));
				}
				const at = this.now();
				for (const u of units) this.entries.set(u, { value: found.get(u) ?? null, at });
				this.trim();
				this.opts.onChange?.();
			} catch {
				// The registry is down or slow: ask again on the next page that shows these tokens.
			} finally {
				for (const u of units) this.pending.delete(u);
			}
		})();
		for (const u of units) this.pending.set(u, p);
		return p;
	}

	private trim() {
		if (this.entries.size <= MAX_ENTRIES) return;
		const oldest = [...this.entries].sort((a, b) => a[1].at - b[1].at).slice(0, this.entries.size - MAX_ENTRIES);
		for (const [u] of oldest) this.entries.delete(u);
	}
}
