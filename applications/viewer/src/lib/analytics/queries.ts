/*
 * SQL for Yaci Store's analytics query API (POST /analytics/query/sql, DuckDB over the store's daily exports).
 * The API takes plain SQL with no bind parameters, so every value is checked against a strict pattern before it is
 * written into a query, and anything else throws.
 */

const BECH32_ADDRESS = /^(addr|addr_test)1[02-9ac-hj-np-z]{6,}$/;
const BYRON_ADDRESS = /^[1-9A-HJ-NP-Za-km-z]{20,}$/;
const BECH32_STAKE = /^(stake|stake_test)1[02-9ac-hj-np-z]{6,}$/;
const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

export class QueryValueError extends Error {}

export function addressLiteral(address: string): string {
	if (!BECH32_ADDRESS.test(address) && !BYRON_ADDRESS.test(address)) throw new QueryValueError(`Not an address: ${address}`);
	return `'${address}'`;
}

export function stakeLiteral(stake: string): string {
	if (!BECH32_STAKE.test(stake)) throw new QueryValueError(`Not a stake address: ${stake}`);
	return `'${stake}'`;
}

export function dateLiteral(day: string): string {
	if (!ISO_DATE.test(day) || Number.isNaN(Date.parse(day))) throw new QueryValueError(`Not a date: ${day}`);
	return `DATE '${day}'`;
}

export function intLiteral(n: number): string {
	if (!Number.isSafeInteger(n)) throw new QueryValueError(`Not an integer: ${n}`);
	return String(n);
}

/** The ISO date `days` after (or before, if negative) `day`. */
export function addDays(day: string, days: number): string {
	const d = new Date(`${day}T00:00:00Z`);
	d.setUTCDate(d.getUTCDate() + days);
	return d.toISOString().slice(0, 10);
}

/** The table names to read: the live view where it is known to answer quickly, otherwise the exported data. */
export interface Tables {
	reward: string;
	rewardRest: string;
	stakeBalance: string;
	delegation: string;
}

/** A stake account's rewards (or, with `rest`, its other rewards: refunds, treasury and reserve payouts) over epochs. */
export function stakeRewardsSql(q: { stake: string; fromEpoch: number; toEpoch: number; rest?: boolean; tables: Tables }): string {
	const s = stakeLiteral(q.stake);
	const where = `address = ${s} AND epoch >= ${intLiteral(q.fromEpoch)} AND epoch <= ${intLiteral(q.toEpoch)}`;
	return q.rest
		? `SELECT epoch, type, amount, spendable_epoch FROM ${q.tables.rewardRest} WHERE ${where} ORDER BY epoch DESC, type`
		: `SELECT epoch, type, pool_id, amount, spendable_epoch FROM ${q.tables.reward} WHERE ${where} ORDER BY epoch DESC, type`;
}

/** What a stake account controls, as of its latest balance change, and the pool it last delegated to. */
export function stakeAccountSql(stake: string, tables: Tables): string {
	const s = stakeLiteral(stake);
	return `SELECT
  (SELECT quantity FROM ${tables.stakeBalance} WHERE address = ${s} ORDER BY slot DESC LIMIT 1) AS controlled_amount,
  (SELECT pool_id FROM ${tables.delegation} WHERE address = ${s} ORDER BY slot DESC LIMIT 1) AS pool_id`;
}
