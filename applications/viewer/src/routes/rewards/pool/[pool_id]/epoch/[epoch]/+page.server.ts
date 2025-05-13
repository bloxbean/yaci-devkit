import { error } from '@sveltejs/kit';
import type { PageServerLoad } from './$types';
import { env } from '$env/dynamic/public';

const INDEXER_BASE_URL = env.PUBLIC_INDEXER_BASE_URL;
const DEFAULT_COUNT = 15;

// Updated interface to match API response
interface PoolEpochReward {
    address: string;
    reward: number; // Changed from amount
    type: string;   // Added
    earned_epoch: number; // Changed from epoch_no
}

export const load: PageServerLoad = async ({ params, url, fetch }) => {
    const poolId = params.pool_id;
    const epoch = params.epoch;
    
    // Read 1-based page from URL, default to 1
    const page = parseInt(url.searchParams.get('page') || '1');
    const count = parseInt(url.searchParams.get('count') || String(DEFAULT_COUNT));

    if (!poolId) {
        throw error(400, 'Pool ID is required');
    }
    if (!epoch || isNaN(Number(epoch))) {
        throw error(400, 'Invalid epoch number');
    }
    if (isNaN(page) || page < 1) {
         throw error(400, 'Invalid page number');
    }
    if (isNaN(count) || count < 1 || count > 100) { // Example limit
        throw error(400, 'Invalid count number');
    }

    const fetchPoolEpochRewards = async (requestedPage = 1) => {
        // API expects 0-based page index
        const zeroBasedPage = requestedPage - 1;
        const apiUrl = `${INDEXER_BASE_URL}/pools/${poolId}/epochs/${epoch}/rewards?page=${zeroBasedPage}&count=${count}`;
        try {
            console.log("Fetching Pool Epoch Rewards URL:", apiUrl);
            const response = await fetch(apiUrl);
            if (!response.ok) {
                console.error(`Failed to fetch pool epoch rewards: ${response.status} ${response.statusText}`);
                return { data: [], error: `Pool Epoch Rewards: ${response.statusText}` };
            }
            const data: PoolEpochReward[] = await response.json();
            console.log(`Fetched ${data.length} pool epoch rewards for requested UI page ${requestedPage} (API page ${zeroBasedPage}).`);
            return { data, error: null };
        } catch (err) {
            console.error(`Error fetching pool epoch rewards:`, err);
            return { data: [], error: 'Failed to fetch pool epoch rewards' };
        }
    };

    // Fetch using 1-based page number
    const rewardsResult = await fetchPoolEpochRewards(page);

    return {
        poolId: poolId,
        epoch: Number(epoch),
        rewards: rewardsResult.data,
        currentPage: page, // Return 1-based page
        count: count,
        error: rewardsResult.error,
        // Add total_pages from API if available, otherwise calculate
        // total_pages: rewardsResult.total_pages || Math.ceil(rewardsResult.total / count) 
        // Assuming the API doesn't return total/total_pages for this endpoint
        // The client will rely on rewards.length === count for hasMore
    };
}; 