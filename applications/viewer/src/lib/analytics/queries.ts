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
	/** Outputs, newest included where possible. */
	outputs: string;
	/** Outputs, exported data only (for joins). */
	outputsHistory: string;
	/** Spent outputs. */
	spends: string;
	reward: string;
	rewardRest: string;
	stakeBalance: string;
	delegation: string;
}

/**
 * Transactions that paid the address (its outputs) or spent from it, newest first, between two days (inclusive) and
 * before a slot. Spends only count outputs created since `lookbackFrom`: matching every spend against the address's
 * whole history is too slow for busy addresses.
 */
export function addressTxsSql(q: { address: string; from: string; to: string; lookbackFrom: string; beforeSlot?: number; limit: number; tables: Tables }): string {
	const a = addressLiteral(q.address);
	const from = dateLiteral(q.from);
	const to = dateLiteral(q.to);
	const before = q.beforeSlot != null ? intLiteral(q.beforeSlot) : null;
	const t = q.tables;
	return `WITH mine AS (
  SELECT tx_hash, output_index FROM ${t.outputsHistory}
  WHERE owner_addr = ${a} AND date >= ${dateLiteral(q.lookbackFrom)} AND date <= ${to}
  GROUP BY tx_hash, output_index
), paid AS (
  SELECT tx_hash, max(slot) AS slot, max(block_time) AS block_time, true AS received, false AS sent FROM ${t.outputs}
  WHERE owner_addr = ${a} AND date >= ${from} AND date <= ${to}${before ? ` AND slot < ${before}` : ''}
  GROUP BY tx_hash
), spent AS (
  SELECT i.spent_tx_hash AS tx_hash, max(i.spent_at_slot) AS slot, max(i.spent_block_time) AS block_time, false AS received, true AS sent
  FROM ${t.spends} i JOIN mine m ON m.tx_hash = i.tx_hash AND m.output_index = i.output_index
  WHERE i.date >= ${from} AND i.date <= ${to}${before ? ` AND i.spent_at_slot < ${before}` : ''}
  GROUP BY i.spent_tx_hash
)
SELECT tx_hash, max(slot) AS slot, max(block_time) AS block_time, bool_or(received) AS received, bool_or(sent) AS sent
FROM (SELECT * FROM paid UNION ALL SELECT * FROM spent)
GROUP BY tx_hash
ORDER BY slot DESC
LIMIT ${intLiteral(q.limit)}`;
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
