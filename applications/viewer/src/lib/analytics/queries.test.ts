import { describe, expect, it } from 'vitest';
import { addDays, addressLiteral, dateLiteral, QueryValueError, stakeAccountSql, stakeLiteral, stakeRewardsSql, type Tables } from './queries';

const A = 'addr1z84q0denmyep98ph3tmzwsmw0j7zau9ljmsqx6a4rvaau66j2c79gy9l76sdg0xwhd7r0c0kna0tycz4y5s6mlenh8pq777e2a';
const S = 'stake1u9f9v0z5zzlldgx58n8tklphu8mf7h4jvp2j2gddluemnssjfnkzz';
const tables: Tables = {
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

describe('values written into SQL', () => {
	it('accepts addresses, stake addresses and dates', () => {
		expect(addressLiteral(A)).toBe(`'${A}'`);
		expect(addressLiteral('DdzFFzCqrhsw3prhfMFDNFowbzUku3QmrMwarfjUbWXRisodn97R436SHc1rimp4MhPNmbdYb1aTdqtGSJixMVMi5MkArDQJ6Sc1n3Ez')).toMatch(/^'Ddz/);
		expect(stakeLiteral(S)).toBe(`'${S}'`);
		expect(dateLiteral('2026-10-18')).toBe("DATE '2026-10-18'");
	});

	it.each([
		["addr1x' OR 1=1 --"],
		['addr1qq; DROP TABLE x'],
		["Ddz'"],
		['']
	])('rejects %j as an address', (bad) => {
		expect(() => addressLiteral(bad)).toThrow(QueryValueError);
	});

	it('rejects anything else as a stake address or date', () => {
		expect(() => stakeLiteral(`${S}'`)).toThrow(QueryValueError);
		expect(() => stakeLiteral(A)).toThrow(QueryValueError);
		expect(() => dateLiteral("2026-10-18' OR '1")).toThrow(QueryValueError);
		expect(() => dateLiteral('2026-13-45')).toThrow(QueryValueError);
	});

	it('adds days across months and years', () => {
		expect(addDays('2026-10-18', -6)).toBe('2026-10-12');
		expect(addDays('2026-01-03', -7)).toBe('2025-12-27');
		expect(addDays('2024-02-28', 1)).toBe('2024-02-29');
	});
});

describe('stake queries', () => {
	it('reads rewards for a range of epochs', () => {
		expect(stakeRewardsSql({ stake: S, fromEpoch: 643, toEpoch: 662, tables })).toBe(
			`SELECT epoch, type, pool_id, amount, spendable_epoch FROM reward WHERE address = '${S}' AND epoch >= 643 AND epoch <= 662 ORDER BY epoch DESC, type`
		);
		expect(stakeRewardsSql({ stake: S, fromEpoch: 643, toEpoch: 662, rest: true, tables })).toContain('FROM parquet_reward_rest');
	});

	it('reads the account from the latest balance and delegation', () => {
		const sql = stakeAccountSql(S, tables);
		expect(sql).toContain(`FROM parquet_stake_address_balance WHERE address = '${S}' ORDER BY slot DESC LIMIT 1`);
		expect(sql).toContain(`FROM parquet_delegation WHERE address = '${S}' ORDER BY slot DESC LIMIT 1`);
	});

	it('rejects a range that is not whole epochs', () => {
		expect(() => stakeRewardsSql({ stake: S, fromEpoch: 1.5, toEpoch: 2, tables })).toThrow(QueryValueError);
	});
});
