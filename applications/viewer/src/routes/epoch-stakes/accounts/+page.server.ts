import { error } from '@sveltejs/kit';
import { env } from '$env/dynamic/public';

export const load = async ({ url }) => {
    const address = url.searchParams.get('address');
    const epoch = url.searchParams.get('epoch');

    if (!address || !epoch) {
        return {
            stakeData: null,
            address: null,
            epoch: null
        };
    }

    try {
        const stakeUrl = `${env.PUBLIC_INDEXER_BASE_URL}/epochs/${epoch}/accounts/${address}/stake`;
        console.log('Fetching stake data from:', stakeUrl);
        
        const response = await fetch(stakeUrl);

        if (!response.ok) {
            console.error('Failed to fetch stake data:', {
                status: response.status,
                statusText: response.statusText,
                url: response.url
            });
            const errorText = await response.text();
            console.error('Error response:', errorText);
            throw error(response.status, `Failed to fetch stake data: ${errorText}`);
        }

        const data = await response.json();
        console.log('Stake data response:', data);

        return {
            stakeData: data,
            address,
            epoch
        };
    } catch (err) {
        console.error('Error in load function for stake data:', err);
        const message = err instanceof Error ? err.message : 'Failed to load stake data';
        if (err && typeof err === 'object' && 'status' in err && typeof err.status === 'number') {
            throw error(err.status, message);
        } else {
            throw error(500, message);
        }
    }
}; 