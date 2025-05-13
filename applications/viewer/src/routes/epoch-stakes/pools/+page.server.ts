import { error } from '@sveltejs/kit';
import { env } from '$env/dynamic/public';

export const load = async ({ url }) => {
    const poolId = url.searchParams.get('id');
    const epoch = url.searchParams.get('epoch');
    const page = Number(url.searchParams.get('page')) || 1;
    const itemsPerPage = 15;

    if (!poolId || !epoch) {
        // Return default structure if required params are missing
        return {
            stakes: [],
            // totalItems: 0, // We don't have a reliable total count here
            currentPage: 1,
            itemsPerPage,
            poolId: null,
            epoch: null,
            totalStake: 0
        };
    }

    try {
        // Fetch Pool's Total Stake for the Epoch
        let totalStake = 0;
        try {
            const totalStakeUrl = `${env.PUBLIC_INDEXER_BASE_URL}/epochs/${epoch}/pools/${poolId}/stake`;
            console.log('Fetching pool total stake from:', totalStakeUrl);
            const totalStakeResponse = await fetch(totalStakeUrl);
            if (totalStakeResponse.ok) {
                const stakeData = await totalStakeResponse.json();
                totalStake = stakeData.active_stake || 0;
                console.log('Pool total stake response:', stakeData);
            } else {
                console.error('Failed to fetch pool total stake:', {
                    status: totalStakeResponse.status,
                    statusText: totalStakeResponse.statusText,
                    url: totalStakeUrl
                });
            }
        } catch (stakeErr) {
            console.error('Error fetching pool total stake:', stakeErr);
        }

        // Fetch Delegators for the current page
        const delegatorsUrl = `${env.PUBLIC_INDEXER_BASE_URL}/epochs/${epoch}/pools/${poolId}/delegators?count=${itemsPerPage}&page=${page}`;
        console.log('Fetching pool delegators from:', delegatorsUrl);
        
        const response = await fetch(delegatorsUrl);

        if (!response.ok) {
            console.error('Failed to fetch pool delegators:', {
                status: response.status,
                statusText: response.statusText,
                url: response.url
            });
            const errorText = await response.text();
            console.error('Error response:', errorText);
            // Throw error only if delegator fetch fails, as it's crucial
            throw error(response.status, `Failed to fetch pool delegators data: ${errorText}`);
        }

        const delegatorsData = await response.json();
        console.log('Pool delegators response:', delegatorsData);

        // Ensure delegatorsData is an array
        const stakesArray = Array.isArray(delegatorsData) ? delegatorsData : [];

        return {
            stakes: stakesArray,
            // totalItems: stakesArray.length, // Keep client logic based on stakes.length < itemsPerPage
            currentPage: page,
            itemsPerPage,
            poolId,
            epoch,
            totalStake  // This is now from the separate /stake endpoint
        };
    } catch (err) {
        console.error('Error in load function for pool stakes:', err);
        // Use a more specific error message if possible
        const message = err instanceof Error ? err.message : 'Failed to load pool stake data';
         if (err && typeof err === 'object' && 'status' in err && typeof err.status === 'number') {
             throw error(err.status, message);
         } else {
             throw error(500, message);
         }
    }
}; 