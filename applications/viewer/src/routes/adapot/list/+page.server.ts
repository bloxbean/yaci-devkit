import type { PageServerLoad } from './$types';
import { env } from '$env/dynamic/public';

interface ApiResponse {
    epoch: number;
    rewards_pot: number | null;
    deposits_stake: number | null;
    fees: number | null;
    treasury: number | null;
    reserves: number | null;
    circulation: number | null;
    distributed_rewards: number | null;
    undistributed_rewards: number | null;
    pool_rewards_pot: number | null;
}

function numberOrZero(value: number | null | undefined): number {
    return value ?? 0;
}

function mapAdapot(item: ApiResponse) {
    return {
        epoch: item.epoch,
        totalAmount: numberOrZero(item.rewards_pot),
        currentEpoch: item.epoch,
        details: {
            depositsStake: numberOrZero(item.deposits_stake),
            fees: numberOrZero(item.fees),
            treasury: numberOrZero(item.treasury),
            reserves: numberOrZero(item.reserves),
            circulation: numberOrZero(item.circulation),
            distributedRewards: numberOrZero(item.distributed_rewards),
            undistributedRewards: numberOrZero(item.undistributed_rewards),
            poolRewardsPot: numberOrZero(item.pool_rewards_pot)
        }
    };
}

export const load: PageServerLoad = async ({ url, fetch }) => {
    const page = url.searchParams.get('page') || '1';
    const count = url.searchParams.get('count') || '15';

    try {
        const INDEXER_BASE_URL = env.PUBLIC_INDEXER_BASE_URL;
        if (!INDEXER_BASE_URL) {
            throw new Error('PUBLIC_INDEXER_BASE_URL environment variable is not set');
        }

        const params = new URLSearchParams({ page, count });
        const response = await fetch(`${INDEXER_BASE_URL}/adapot/list?${params.toString()}`);

        if (response.status === 404) {
            return { adapots: [], page, count };
        }
        if (!response.ok) {
            throw new Error(`Failed to fetch AdaPot list (Status: ${response.status})`);
        }

        const adapots: ApiResponse[] = await response.json();
        if (!Array.isArray(adapots)) {
            throw new Error('Invalid data format received from server');
        }

        return {
            adapots: adapots.map(mapAdapot),
            page,
            count
        };
    } catch (error) {
        console.error('Error loading AdaPots:', error);
        return {
            adapots: [],
            page,
            count,
            error: error instanceof Error ? error.message : 'An error occurred while loading AdaPots'
        };
    }
};
