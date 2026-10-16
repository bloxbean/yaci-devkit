/* Ada pot fields, in the order they read best, with a colour per pot. */
export interface AdaPot {
	epoch: number;
	treasury?: number | null;
	reserves?: number | null;
	circulation?: number | null;
	rewards_pot?: number | null;
	pool_rewards_pot?: number | null;
	distributed_rewards?: number | null;
	undistributed_rewards?: number | null;
	fees?: number | null;
	deposits_stake?: number | null;
}

export const POTS: { key: keyof AdaPot; label: string; hint: string; color: string }[] = [
	{ key: 'treasury', label: 'Treasury', hint: 'Funds governance can spend', color: 'cyan' },
	{ key: 'reserves', label: 'Reserves', hint: 'ADA not yet in circulation', color: 'violet' },
	{ key: 'circulation', label: 'Circulation', hint: 'ADA held by everyone else', color: 'brand' },
	{ key: 'rewards_pot', label: 'Rewards pot', hint: 'Reserves expansion plus fees, for this epoch', color: 'mint' },
	{ key: 'pool_rewards_pot', label: 'Pool rewards pot', hint: 'Rewards pot after the treasury cut', color: 'mint' },
	{ key: 'distributed_rewards', label: 'Distributed rewards', hint: 'Paid to pools and delegators', color: 'brand' },
	{ key: 'undistributed_rewards', label: 'Undistributed rewards', hint: 'Returned to the reserves', color: 'amber' },
	{ key: 'fees', label: 'Fees', hint: 'Transaction fees collected', color: 'amber' },
	{ key: 'deposits_stake', label: 'Stake deposits', hint: 'Key, pool and DRep deposits held', color: 'blue' }
];

/** Lovelace → ADA as a float, for charts. */
export const toAda = (v: number | string | null | undefined) => (v == null ? NaN : Number(v) / 1_000_000);
