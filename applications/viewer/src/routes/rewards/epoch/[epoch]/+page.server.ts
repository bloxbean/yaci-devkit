import { error } from '@sveltejs/kit';
import type { PageServerLoad } from './$types';
import { env } from '$env/dynamic/public';

const INDEXER_BASE_URL = env.PUBLIC_INDEXER_BASE_URL;
const DEFAULT_COUNT = 15; // Number of items per page

// Define interfaces for the expected API responses
interface PoolReward {
    pool_id: string;
    amount: number;
    epoch_no: number;
}

interface RestReward {
    stake_address: string;
    amount: number;
    type: string; // e.g., "leader", "member", "reserve", "treasury"
    epoch_no: number;
}

export const load: PageServerLoad = async ({ params, url, fetch }) => {
    const epoch = params.epoch;
    // Read page and count from URL search params
    const page = parseInt(url.searchParams.get('page') || '1');
    const count = parseInt(url.searchParams.get('count') || String(DEFAULT_COUNT));
    
    // Validate epoch, page, count...
    if (!epoch || isNaN(Number(epoch))) {
        throw error(400, 'Invalid epoch number');
    }
     if (isNaN(page) || page < 1) {
         throw error(400, 'Invalid page number');
    }
    if (isNaN(count) || count < 1 || count > 100) { // Use the validated count
        throw error(400, 'Invalid count number');
    }

    const fetchPoolRewards = async (currentPage = 1) => {
        const url = `${INDEXER_BASE_URL}/epochs/${epoch}/rewards?page=${currentPage}&count=${count}`;
        try {
            console.log("Fetching Pool Rewards URL:", url);
            const response = await fetch(url);
            if (!response.ok) {
                console.error(`Failed to fetch pool rewards for epoch ${epoch}: ${response.status} ${response.statusText}`);
                const errorText = await response.text();
                console.error("Error details:", errorText);
                return { data: [], error: `Pool Rewards: ${response.statusText}` }; // Return empty data and error
            }
            const data: PoolReward[] = await response.json();
            console.log(`Fetched ${data.length} pool rewards.`);
            return { data, error: null };
        } catch (err) {
            console.error(`Error fetching pool rewards for epoch ${epoch}:`, err);
            return { data: [], error: 'Failed to fetch pool rewards' };
        }
    };

    const fetchRestRewards = async (currentPage = 1) => {
        const url = `${INDEXER_BASE_URL}/epochs/${epoch}/reward_rest?page=${currentPage}&count=${count}`;
        try {
            console.log("Fetching Rest Rewards URL:", url);
            const response = await fetch(url);
            if (!response.ok) {
                console.error(`Failed to fetch rest rewards for epoch ${epoch}: ${response.status} ${response.statusText}`);
                const errorText = await response.text();
                console.error("Error details:", errorText);
                return { data: [], error: `Rest Rewards: ${response.statusText}` };
            }
            const data: RestReward[] = await response.json();
            console.log(`Fetched ${data.length} rest rewards.`);
            return { data, error: null };
        } catch (err) {
            console.error(`Error fetching rest rewards for epoch ${epoch}:`, err);
            return { data: [], error: 'Failed to fetch rest rewards' };
        }
    };

    const fetchUnclaimedRestRewards = async (currentPage = 1) => {
        const url = `${INDEXER_BASE_URL}/epochs/${epoch}/unclaimed_reward_rest?page=${currentPage}&count=${count}`;
        try {
            console.log("Fetching Unclaimed Rest Rewards URL:", url);
            const response = await fetch(url);
            if (!response.ok) {
                console.error(`Failed to fetch unclaimed rest rewards for epoch ${epoch}: ${response.status} ${response.statusText}`);
                const errorText = await response.text();
                console.error("Error details:", errorText);
                return { data: [], error: `Unclaimed Rewards: ${response.statusText}` };
            }
            const data: RestReward[] = await response.json();
            console.log(`Fetched ${data.length} unclaimed rest rewards.`);
            return { data, error: null };
        } catch (err) {
            console.error(`Error fetching unclaimed rest rewards for epoch ${epoch}:`, err);
            return { data: [], error: 'Failed to fetch unclaimed rest rewards' };
        }
    };

    // Fetch the *requested page* (not just page 1) of each data type concurrently
    const [poolRewardsResult, restRewardsResult, unclaimedRewardsResult] = await Promise.all([
        fetchPoolRewards(page),
        fetchRestRewards(page),
        fetchUnclaimedRestRewards(page)
    ]);

    const errors = [
        poolRewardsResult.error,
        restRewardsResult.error,
        unclaimedRewardsResult.error
    ].filter(e => e !== null);

    // Combine errors if multiple fetches failed
    const combinedError = errors.length > 0 ? errors.join('; ') : null;

    return {
        epoch: Number(epoch),
        poolRewards: poolRewardsResult.data,
        restRewards: restRewardsResult.data,
        unclaimedRewards: unclaimedRewardsResult.data,
        currentPage: page, // Return the requested page number
        count: count,      // Return the used count
        error: combinedError,
    };
}; 