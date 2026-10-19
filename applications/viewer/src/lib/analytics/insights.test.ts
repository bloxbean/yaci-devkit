import { describe, expect, it } from 'vitest';
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
} from './insights';
import { QueryValueError, type Tables } from './queries';

const t: Tables = {
	transaction: 'parquet_transaction',
	transactionScripts: 'parquet_transaction_scripts',
	block: 'parquet_block',
	adapot: 'parquet_adapot',
	epochStake: 'parquet_epoch_stake',
	drepDist: 'parquet_drep_dist',
	reward: 'reward',
	rewardRest: 'parquet_reward_rest',
	stakeBalance: 'parquet_stake_address_balance',
	delegation: 'parquet_delegation'
};
const SCRIPT = 'd8b69fc53637bcfadbc4469083f706bc293f4d9d2296646c5ca167bb';
const POOL = 'ec3308cedfbe904927d51f9a0d9cb93492da29e22e32900f85a15d8c';
const STAKE = 'stake1u9f9v0z5zzlldgx58n8tklphu8mf7h4jvp2j2gddluemnssjfnkzz';

describe('chart queries', () => {
	it('group by day from a fixed date, over the given tables', () => {
		expect(dailyTxsSql('2026-07-21', t)).toBe(
			"SELECT date, count(*) AS txs, sum(fee) AS fees FROM parquet_transaction WHERE date >= DATE '2026-07-21' GROUP BY date ORDER BY date"
		);
		expect(dailyScriptsSql('2026-07-21', t)).toContain("FROM parquet_transaction_scripts WHERE date >= DATE '2026-07-21' GROUP BY date");
		expect(topScriptsSql('2026-10-12', 10, t)).toMatch(/GROUP BY script_hash ORDER BY runs DESC LIMIT 10$/);
		expect(scriptDailySql(SCRIPT, '2026-07-21', t)).toContain(`WHERE script_hash = '${SCRIPT}' AND date >= DATE '2026-07-21'`);
	});

	it('group by epoch from a given epoch', () => {
		expect(adaPotsSql(t)).toBe('SELECT epoch, treasury, reserves, fees, distributed_rewards AS rewards FROM parquet_adapot ORDER BY epoch');
		expect(epochBlocksSql(612, t)).toContain('FROM parquet_block WHERE epoch >= 612 GROUP BY epoch');
		expect(poolBlocksSql(POOL, 612, t)).toContain(`WHERE slot_leader = '${POOL}' AND epoch >= 612`);
		expect(poolStakeSql(POOL, 612, t)).toContain(`WHERE pool_id = '${POOL}' AND epoch >= 612`);
		expect(stakeRewardsByEpochSql(STAKE, 612, t)).toContain(`FROM reward WHERE address = '${STAKE}' AND epoch >= 612`);
		expect(stakeHistorySql(STAKE, 612, t)).toContain(`FROM parquet_epoch_stake WHERE address = '${STAKE}' AND epoch >= 612`);
	});

	it('look for the latest epoch from a lower bound', () => {
		expect(drepPowerSql(659, 10, t)).toContain('SELECT max(epoch) AS e FROM parquet_drep_dist WHERE epoch >= 659');
		expect(drepPowerSql(659, 10, t)).toMatch(/ORDER BY amount DESC LIMIT 10$/);
	});

	it.each([
		() => scriptDailySql(`${SCRIPT}' OR '1'='1`, '2026-07-21', t),
		() => scriptDailySql(SCRIPT.toUpperCase(), '2026-07-21', t),
		() => poolBlocksSql('pool1abc', 612, t),
		() => poolStakeSql(POOL, 612.5, t),
		() => stakeHistorySql("stake1x' --", 612, t),
		() => dailyTxsSql('2026-07-21; SELECT 1', t),
		() => topScriptsSql('2026-10-12', Number.NaN, t)
	])('rejects values that are not what they claim (%#)', (build) => {
		expect(build).toThrow(QueryValueError);
	});

	it('are single statements', () => {
		for (const sql of [dailyTxsSql('2026-07-21', t), drepPowerSql(659, 10, t), stakeHistorySql(STAKE, 612, t)]) expect(sql).not.toContain(';');
	});
});
