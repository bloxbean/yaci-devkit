import { error } from '@sveltejs/kit';
import { env } from '$env/dynamic/public';
import type { PageServerLoad } from './$types';

interface AdaPotData {
    epoch: number;
    deposits_stake: number;
    fees: number;
    treasury: number;
    reserves: number;
    circulation: number;
    distributed_rewards: number;
    undistributed_rewards: number;
    rewards_pot: number;
    pool_rewards_pot: number;
}

export const load: PageServerLoad = async () => {
    try {
        const INDEXER_BASE_URL = env.PUBLIC_INDEXER_BASE_URL;
        console.log('INDEXER_BASE_URL:', INDEXER_BASE_URL);
        
        if (!INDEXER_BASE_URL) {
            throw new Error('PUBLIC_INDEXER_BASE_URL environment variable is not set');
        }
        
        const url = `${INDEXER_BASE_URL}/adapot/list`;
        console.log('Fetching URL:', url);
        
        const response = await fetch(url, {
            headers: {
                'Accept': 'application/json'
            }
        });
        
        console.log('Response status:', response.status);
        console.log('Response headers:', Object.fromEntries(response.headers.entries()));
        
        if (!response.ok) {
            const errorText = await response.text();
            console.error('Error response:', errorText);
            throw new Error(`Failed to fetch latest AdaPot details: ${response.status} ${response.statusText}`);
        }
        
        const data: AdaPotData = await response.json();
        console.log('Fetched data:', data);
        
        // Transform the data to match our frontend expectations
        const transformedData = {
            epoch: data.epoch,
            totalAmount: data.rewards_pot,
            currentEpoch: data.epoch,
            details: {
                depositsStake: data.deposits_stake,
                fees: data.fees,
                treasury: data.treasury,
                reserves: data.reserves,
                circulation: data.circulation,
                distributedRewards: data.distributed_rewards,
                undistributedRewards: data.undistributed_rewards,
                poolRewardsPot: data.pool_rewards_pot
            }
        };
        
        return {
            adapot: transformedData
        };
    } catch (e) {
        console.error('Error in load function:', e);
        return {
            error: e instanceof Error ? e.message : 'Failed to fetch AdaPot details'
        };
    }
};
