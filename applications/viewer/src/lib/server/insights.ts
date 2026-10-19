import { addDays } from '$lib/analytics/queries';
import {
	adaPotsSql,
	dailyScriptsSql,
	dailyTxsSql,
	drepPowerSql,
	epochBlocksSql,
	poolBlocksSql,
	poolStakeSql,
	scriptDailySql,
	stakeHistorySql,
	stakeRewardsByEpochSql,
	topScriptsSql
} from '$lib/analytics/insights';
import type { AnalyticsSource, DayPoint, DRepPower, NetworkInsights, PoolInsights, ScriptInsights, StakeInsights } from '$lib/analytics/types';
import { analyticsInfo, analyticsSql, today, type AnalyticsInfo } from './analytics';
import { SwrCache } from './swr-cache';

/*
 * Charts and numbers from Yaci Store analytics, for stores that have it; null otherwise (the devkit's store), so the
 * pages leave them out. The exports are daily, so answers are kept and served while they refresh in the background:
 * network-wide charts for an hour, one script's, pool's or account's for 30 minutes. Only the first request after a
 * restart (or after a day) waits for the store.
 */

type Fetch = typeof globalThis.fetch;
// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Row = Record<string, any>;

export const INSIGHTS = {
	networkTtlMs: 60 * 60_000,
	entityTtlMs: 30 * 60_000,
	maxStaleMs: 24 * 60 * 60_000,
	maxEntries: 2000,
	/** Days in the daily charts, days the top scripts are counted over, and epochs in the per-epoch charts. */
	days: 90,
	topScriptDays: 7,
	epochs: 50
} as const;

const cache = new SwrCache({ maxEntries: INSIGHTS.maxEntries, maxStaleMs: INSIGHTS.maxStaleMs });
const n = (v: unknown) => (v == null ? 0 : Number(v));

/** Rows of one query, through the cache (keyed by the SQL itself). */
function rows(sql: string, ttlMs: number, fetch: Fetch, maxRows = 1000): Promise<Row[]> {
	return cache.get(sql, ttlMs, () => analyticsSql<Row>(sql, { fetch, maxRows, cacheMs: 0 }));
}

/** The same, null when that query fails: one missing chart shouldn't hide the others. */
const maybe = (sql: string, ttlMs: number, fetch: Fetch, maxRows?: number) => rows(sql, ttlMs, fetch, maxRows).catch(() => null);

const sourceOf = (info: AnalyticsInfo): AnalyticsSource => ({ exportedTo: info.exportedTo, live: false });

/** Network-wide: daily transactions and script use, the most-run scripts, the ada pots, blocks per epoch. */
export async function networkInsights(fetch: Fetch, currentEpoch: number | null): Promise<NetworkInsights | null> {
	const info = await analyticsInfo(fetch);
	if (!info) return null;
	const t = info.tables;
	const ttl = INSIGHTS.networkTtlMs;
	const from = addDays(today(), -INSIGHTS.days);
	const fromEpoch = Math.max(0, (currentEpoch ?? 0) - INSIGHTS.epochs);
	const [txs, scripts, top, pots, epochs] = await Promise.all([
		maybe(dailyTxsSql(from, t), ttl, fetch),
		maybe(dailyScriptsSql(from, t), ttl, fetch),
		maybe(topScriptsSql(addDays(today(), -INSIGHTS.topScriptDays), 10, t), ttl, fetch),
		maybe(adaPotsSql(t), ttl, fetch),
		currentEpoch != null ? maybe(epochBlocksSql(fromEpoch, t), ttl, fetch) : null
	]);
	const byDate = new Map<string, DayPoint>();
	const day = (date: string) => byDate.get(date) ?? byDate.set(date, { date, txs: 0, fees: 0, runs: 0, scriptTxs: 0, steps: 0, mem: 0 }).get(date)!;
	for (const r of txs ?? []) Object.assign(day(String(r.date)), { txs: n(r.txs), fees: n(r.fees) });
	for (const r of scripts ?? []) Object.assign(day(String(r.date)), { runs: n(r.runs), scriptTxs: n(r.txs), steps: n(r.steps), mem: n(r.mem) });
	return {
		...sourceOf(info),
		from,
		days: txs || scripts ? [...byDate.values()].sort((a, b) => a.date.localeCompare(b.date)) : null,
		topScripts: top?.map((r) => ({ script_hash: String(r.script_hash), runs: n(r.runs), txs: n(r.txs), steps: n(r.steps), mem: n(r.mem) })) ?? null,
		topScriptDays: INSIGHTS.topScriptDays,
		pots: pots?.map((r) => ({ epoch: n(r.epoch), treasury: n(r.treasury), reserves: n(r.reserves), fees: n(r.fees), rewards: n(r.rewards) })) ?? null,
		epochs: epochs?.map((r) => ({ epoch: n(r.epoch), blocks: n(r.blocks), pools: n(r.pools), txs: n(r.txs) })) ?? null
	};
}

/** DReps (and the abstain / no-confidence options) with the most voting power. */
export async function drepPower(fetch: Fetch, currentEpoch: number | null): Promise<DRepPower | null> {
	const info = await analyticsInfo(fetch);
	if (!info) return null;
	// Without the current epoch, look through every epoch for the latest (the table is small).
	const r = await maybe(drepPowerSql(currentEpoch != null ? currentEpoch - 3 : 0, 10, info.tables), INSIGHTS.networkTtlMs, fetch, 10);
	if (!r?.length) return null;
	return {
		...sourceOf(info),
		epoch: n(r[0].epoch),
		total: n(r[0].total),
		dreps: n(r[0].dreps),
		top: r.map((x) => ({ drep_id: x.drep_id ?? null, drep_type: String(x.drep_type), amount: n(x.amount) }))
	};
}

/** One script's runs and ex-units per day. */
export async function scriptInsights(hash: string, fetch: Fetch): Promise<ScriptInsights | null> {
	const info = await analyticsInfo(fetch);
	if (!info) return null;
	const from = addDays(today(), -INSIGHTS.days);
	const r = await maybe(scriptDailySql(hash, from, info.tables), INSIGHTS.entityTtlMs, fetch);
	if (!r) return null;
	return {
		...sourceOf(info),
		from,
		days: r.map((x) => ({ date: String(x.date), runs: n(x.runs), txs: n(x.txs), steps: n(x.steps), mem: n(x.mem), maxSteps: n(x.max_steps) }))
	};
}

/** One pool's blocks, active stake and delegators per finished epoch. */
export async function poolInsights(poolHash: string, fetch: Fetch, currentEpoch: number | null): Promise<PoolInsights | null> {
	const info = await analyticsInfo(fetch);
	if (!info || currentEpoch == null) return null;
	const fromEpoch = Math.max(0, currentEpoch - INSIGHTS.epochs);
	const [blocks, stake] = await Promise.all([
		maybe(poolBlocksSql(poolHash, fromEpoch, info.tables), INSIGHTS.entityTtlMs, fetch),
		maybe(poolStakeSql(poolHash, fromEpoch, info.tables), INSIGHTS.entityTtlMs, fetch)
	]);
	if (!blocks && !stake) return null;
	const byEpoch = new Map<number, PoolInsights['epochs'][number]>();
	const at = (epoch: number) => byEpoch.get(epoch) ?? byEpoch.set(epoch, { epoch, blocks: 0, stake: 0, delegators: 0 }).get(epoch)!;
	for (const r of stake ?? []) Object.assign(at(n(r.epoch)), { stake: n(r.stake), delegators: n(r.delegators) });
	for (const r of blocks ?? []) at(n(r.epoch)).blocks = n(r.blocks);
	// Finished epochs only: the current one has some of its blocks and no stake snapshot yet.
	return { ...sourceOf(info), epochs: [...byEpoch.values()].filter((x) => x.epoch < currentEpoch).sort((a, b) => a.epoch - b.epoch) };
}

/** A stake account's rewards and active stake per epoch. */
export async function stakeInsights(stake: string, fetch: Fetch, currentEpoch: number | null): Promise<StakeInsights | null> {
	const info = await analyticsInfo(fetch);
	if (!info || currentEpoch == null) return null;
	const fromEpoch = Math.max(0, currentEpoch - INSIGHTS.epochs);
	const [rewards, history] = await Promise.all([
		maybe(stakeRewardsByEpochSql(stake, fromEpoch, info.tables), INSIGHTS.entityTtlMs, fetch),
		maybe(stakeHistorySql(stake, fromEpoch, info.tables), INSIGHTS.entityTtlMs, fetch)
	]);
	if (!rewards?.length && !history?.length) return null;
	const byEpoch = new Map<number, StakeInsights['epochs'][number]>();
	const at = (epoch: number) => byEpoch.get(epoch) ?? byEpoch.set(epoch, { epoch, rewards: 0, stake: null, pool_id: null }).get(epoch)!;
	for (const r of history ?? []) Object.assign(at(n(r.epoch)), { stake: n(r.amount), pool_id: r.pool_id ?? null });
	for (const r of rewards ?? []) at(n(r.epoch)).rewards = n(r.rewards);
	return { ...sourceOf(info), epochs: [...byEpoch.values()].sort((a, b) => a.epoch - b.epoch) };
}
