import { json } from '@sveltejs/kit';
import { env } from '$env/dynamic/public';
import type { RequestHandler } from './$types';

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

export const GET: RequestHandler = async ({ params }) => {
    try {
        const { epoch } = params;
        const INDEXER_BASE_URL = env.PUBLIC_INDEXER_BASE_URL;
        console.log('INDEXER_BASE_URL:', INDEXER_BASE_URL);
        
        if (!INDEXER_BASE_URL) {
            throw new Error('PUBLIC_INDEXER_BASE_URL environment variable is not set');
        }
        
        const url = `${INDEXER_BASE_URL}/adapot/epochs/${epoch}`;
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
            throw new Error(`Failed to fetch AdaPot details for the specified epoch: ${response.status} ${response.statusText}`);
        }
        
        const data: AdaPotData = await response.json();
        console.log('Fetched data:', data);
        
        // Transform the data to match our frontend expectations
        const transformedData = {
            epoch: data.epoch,
            totalAmount: data.rewards_pot,
            lastUpdated: new Date().toISOString(),
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
        
        return json(transformedData);
    } catch (error) {
        console.error('Error in GET handler:', error);
        return json(
            { error: error instanceof Error ? error.message : 'Failed to fetch AdaPot details' },
            { status: 500 }
        );
    }
}; 