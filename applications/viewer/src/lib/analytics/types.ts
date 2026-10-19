/* What pages get back when a section came from Yaci Store analytics instead of the regular endpoints. */

/** Where analytics data came from, for the label next to it. */
export interface AnalyticsSource {
	/** The last exported day (ISO date): newer data comes from the live views where the store has them. */
	exportedTo: string | null;
	live: boolean;
}

/* Charts from analytics. Amounts are lovelace; past 2^53 they lose a few lovelace, which charts can't show anyway. */

export interface DayPoint {
	/** ISO date. */
	date: string;
	txs: number;
	fees: number;
	/** Script runs (redeemers) and transactions with scripts. */
	runs: number;
	scriptTxs: number;
	steps: number;
	mem: number;
}

export interface TopScript {
	script_hash: string;
	runs: number;
	txs: number;
	steps: number;
	mem: number;
}

export interface PotPoint {
	epoch: number;
	treasury: number;
	reserves: number;
	fees: number;
	rewards: number;
}

export interface EpochPoint {
	epoch: number;
	blocks: number;
	pools: number;
	txs: number;
}

export interface StakeConcentration {
	epoch: number;
	pools: number;
	totalStake: number;
	top10Share: number;
	nakamoto: number;
}

export interface NetworkInsights extends AnalyticsSource {
	/** Days covered by `days` (from, to). */
	from: string;
	days: DayPoint[] | null;
	topScripts: TopScript[] | null;
	/** Days the top scripts are counted over. */
	topScriptDays: number;
	pots: PotPoint[] | null;
	epochs: EpochPoint[] | null;
	concentration: StakeConcentration | null;
}

export interface DRepPower extends AnalyticsSource {
	epoch: number;
	total: number;
	dreps: number;
	top: { drep_id: string | null; drep_type: string; amount: number }[];
}

export interface ScriptDay {
	date: string;
	runs: number;
	txs: number;
	steps: number;
	mem: number;
	maxSteps: number;
}

export interface ScriptInsights extends AnalyticsSource {
	from: string;
	days: ScriptDay[];
}

export interface PoolInsights extends AnalyticsSource {
	epochs: { epoch: number; blocks: number; stake: number; delegators: number }[];
}

export interface StakeInsights extends AnalyticsSource {
	epochs: { epoch: number; rewards: number; stake: number | null; pool_id: string | null }[];
}
