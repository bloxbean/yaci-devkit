/*
 * Names the viewer shows next to ids on Mainnet. Script labels come from curated registries; DRep names, pool
 * tickers and governance action titles are what their owners put in their own metadata (checked against the hash on
 * chain, but not unique and not verified); token tickers come from the Cardano Foundation's token registry.
 */

export interface ScriptName {
	/** e.g. "Minswap Order Contract v2" */
	label: string;
	project: string;
	category?: string;
	url?: string;
	source: 'strica' | 'crfa';
}

export interface PoolName {
	ticker?: string;
	name?: string;
}

export interface TokenName {
	ticker?: string;
	name?: string;
	decimals?: number;
}

/** What the browser asks for: lists of keys per kind (see keys.ts for their form). */
export interface NamesQuery {
	scripts?: string[];
	dreps?: string[];
	pools?: string[];
	actions?: string[];
	tokens?: string[];
}

/** The answer: only the keys that have a name. */
export interface NamesResult {
	/** false off Mainnet (or with NAMES=off): the browser stops asking. null while the server is still finding out. */
	enabled?: boolean | null;
	scripts: Record<string, ScriptName>;
	dreps: Record<string, string>;
	pools: Record<string, PoolName>;
	actions: Record<string, string>;
	tokens: Record<string, TokenName>;
}

export type NameKind = keyof NamesQuery;
