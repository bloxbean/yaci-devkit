import { error } from '@sveltejs/kit';
import type { PageServerLoad } from './$types';
import { env } from '$env/dynamic/private';

// Use the public environment variable since it's already set correctly
const INDEXER_BASE_URL = 'http://localhost:8080';
const DEFAULT_COUNT = 15;
const DEFAULT_ORDER = 'desc';

// Define interfaces for the expected API responses
interface AccountPoolReward {
    epoch_no: number;
    pool_id: string;
    amount: number;
}

interface AccountRestReward {
    epoch_no: number;
    amount: number;
    type: string; 
}

export const load: PageServerLoad = async ({ params, url, fetch }) => {
    const address = params.address;
    const page = parseInt(url.searchParams.get('page') || '1');
    const count = parseInt(url.searchParams.get('count') || String(DEFAULT_COUNT));
    const order = url.searchParams.get('order') || DEFAULT_ORDER;

    if (!address) {
        throw error(400, 'Stake address is required');
    }
    if (isNaN(page) || page < 1) {
         throw error(400, 'Invalid page number');
    }
    if (isNaN(count) || count < 1 || count > 100) { // Example limit
        throw error(400, 'Invalid count number');
    }
     if (order !== 'asc' && order !== 'desc') {
        throw error(400, 'Invalid order parameter');
    }

    const fetchAccountPoolRewards = async (currentPage = 1) => {
        const apiUrl = `${INDEXER_BASE_URL}/api/v1/accounts/${address}/rewards?page=${currentPage - 1}&count=${count}&order=${order}`;
        try {
            console.log("Fetching Account Pool Rewards URL:", apiUrl);
            const response = await fetch(apiUrl);
            if (!response.ok) {
                console.error(`Failed to fetch account pool rewards: ${response.status} ${response.statusText}`);
                return { data: [], error: `Account Pool Rewards: ${response.statusText}` };
            }
            const data: AccountPoolReward[] = await response.json();
            console.log(`Fetched ${data.length} account pool rewards.`);
            return { data, error: null };
        } catch (err) {
            console.error(`Error fetching account pool rewards:`, err);
            return { data: [], error: 'Failed to fetch account pool rewards' };
        }
    };

    const fetchAccountRestRewards = async (currentPage = 1) => {
        const apiUrl = `${INDEXER_BASE_URL}/api/v1/accounts/${address}/reward_rest?page=${currentPage - 1}&count=${count}&order=${order}`;
        try {
            console.log("Fetching Account Rest Rewards URL:", apiUrl);
            const response = await fetch(apiUrl);
            if (!response.ok) {
                console.error(`Failed to fetch account rest rewards: ${response.status} ${response.statusText}`);
                 return { data: [], error: `Account Rest Rewards: ${response.statusText}` };
            }
            const data: AccountRestReward[] = await response.json();
            console.log(`Fetched ${data.length} account rest rewards.`);
            return { data, error: null };
        } catch (err) {
            console.error(`Error fetching account rest rewards:`, err);
            return { data: [], error: 'Failed to fetch account rest rewards' };
        }
    };

    // Fetch the first page (or requested page) of each type concurrently
    const [poolRewardsResult, restRewardsResult] = await Promise.all([
        fetchAccountPoolRewards(page),
        fetchAccountRestRewards(page)
    ]);

    const errors = [
        poolRewardsResult.error,
        restRewardsResult.error
    ].filter(e => e !== null);
    const combinedError = errors.length > 0 ? errors.join('; ') : null;

    return {
        address: address,
        poolRewards: poolRewardsResult.data,
        restRewards: restRewardsResult.data,
        currentPage: page,
        count: count,
        order: order,
        error: combinedError,
    };
}; 