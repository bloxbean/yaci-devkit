import { dateLiteral, hashLiteral, intLiteral, stakeLiteral, type Tables } from './queries';

/*
 * SQL for the analytics charts: aggregates by day or by epoch over Yaci Store's exports, which DuckDB answers in a
 * fraction of a second. Daily charts read whole days (from `from` up to the last export), so the newest bar is never a
 * day in progress. Values are checked before they are written into the SQL (see queries.ts).
 */

/** Transactions and fees per day. */
export function dailyTxsSql(from: string, t: Tables): string {
	return `SELECT date, count(*) AS txs, sum(fee) AS fees FROM ${t.transaction} WHERE date >= ${dateLiteral(from)} GROUP BY date ORDER BY date`;
}

/** Script runs (one per redeemer), transactions with scripts, and the ex-units they used, per day. */
export function dailyScriptsSql(from: string, t: Tables): string {
	return `SELECT date, count(*) AS runs, count(DISTINCT tx_hash) AS txs, sum(unit_steps) AS steps, sum(unit_mem) AS mem
FROM ${t.transactionScripts} WHERE date >= ${dateLiteral(from)} GROUP BY date ORDER BY date`;
}

/** The scripts run most often since `from`. */
export function topScriptsSql(from: string, limit: number, t: Tables): string {
	return `SELECT script_hash, count(*) AS runs, count(DISTINCT tx_hash) AS txs, sum(unit_steps) AS steps, sum(unit_mem) AS mem
FROM ${t.transactionScripts} WHERE date >= ${dateLiteral(from)} GROUP BY script_hash ORDER BY runs DESC LIMIT ${intLiteral(limit)}`;
}

/** Treasury, reserves, fees and rewards per epoch. */
export function adaPotsSql(t: Tables): string {
	return `SELECT epoch, treasury, reserves, fees, distributed_rewards AS rewards FROM ${t.adapot} ORDER BY epoch`;
}

/** Blocks, the pools that made them, and transactions per epoch since `fromEpoch`. */
export function epochBlocksSql(fromEpoch: number, t: Tables): string {
	return `SELECT epoch, count(*) AS blocks, count(DISTINCT slot_leader) AS pools, sum(no_of_txs) AS txs
FROM ${t.block} WHERE epoch >= ${intLiteral(fromEpoch)} GROUP BY epoch ORDER BY epoch`;
}

/** The DReps (and the abstain / no-confidence options) with the most voting power in the latest exported epoch. */
export function drepPowerSql(minEpoch: number, limit: number, t: Tables): string {
	return `WITH e AS (SELECT max(epoch) AS e FROM ${t.drepDist} WHERE epoch >= ${intLiteral(minEpoch)})
SELECT epoch, drep_id, drep_type, amount, sum(amount) OVER () AS total, count(*) OVER () AS dreps
FROM ${t.drepDist} WHERE epoch = (SELECT e FROM e) ORDER BY amount DESC LIMIT ${intLiteral(limit)}`;
}

/** One script's runs and ex-units per day, and the most steps a single run took. */
export function scriptDailySql(hash: string, from: string, t: Tables): string {
	return `SELECT date, count(*) AS runs, count(DISTINCT tx_hash) AS txs, sum(unit_steps) AS steps, sum(unit_mem) AS mem, max(unit_steps) AS max_steps
FROM ${t.transactionScripts} WHERE script_hash = ${hashLiteral(hash)} AND date >= ${dateLiteral(from)} GROUP BY date ORDER BY date`;
}

/** Blocks one pool made per epoch since `fromEpoch`. */
export function poolBlocksSql(poolHash: string, fromEpoch: number, t: Tables): string {
	return `SELECT epoch, count(*) AS blocks FROM ${t.block} WHERE slot_leader = ${hashLiteral(poolHash)} AND epoch >= ${intLiteral(fromEpoch)} GROUP BY epoch ORDER BY epoch`;
}

/** One pool's active stake and delegators per epoch since `fromEpoch`. */
export function poolStakeSql(poolHash: string, fromEpoch: number, t: Tables): string {
	return `SELECT epoch, sum(amount) AS stake, count(*) AS delegators FROM ${t.epochStake}
WHERE pool_id = ${hashLiteral(poolHash)} AND epoch >= ${intLiteral(fromEpoch)} GROUP BY epoch ORDER BY epoch`;
}

/** A stake account's rewards per epoch (when earned) since `fromEpoch`. */
export function stakeRewardsByEpochSql(stake: string, fromEpoch: number, t: Tables): string {
	return `SELECT epoch, sum(amount) AS rewards FROM ${t.reward} WHERE address = ${stakeLiteral(stake)} AND epoch >= ${intLiteral(fromEpoch)} GROUP BY epoch ORDER BY epoch`;
}

/** A stake account's active stake, and the pool it was with, per epoch since `fromEpoch`. */
export function stakeHistorySql(stake: string, fromEpoch: number, t: Tables): string {
	return `SELECT epoch, amount, pool_id FROM ${t.epochStake} WHERE address = ${stakeLiteral(stake)} AND epoch >= ${intLiteral(fromEpoch)} ORDER BY epoch`;
}
