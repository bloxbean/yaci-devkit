import { json } from '@sveltejs/kit';
import { env } from '$env/dynamic/public';
import type { RequestHandler } from './$types';

interface AdaPotData {
    epoch: number;
    rewards_pot: number;
    deposits_stake: number;
    fees: number;
    treasury: number;
    reserves: number;
    circulation: number;
    distributed_rewards: number;
    undistributed_rewards: number;
    pool_rewards_pot: number;
}

export const GET: RequestHandler = async ({ url, fetch }) => {
    try {
        const INDEXER_BASE_URL = env.PUBLIC_INDEXER_BASE_URL;
        console.log('INDEXER_BASE_URL:', INDEXER_BASE_URL);
        
        if (!INDEXER_BASE_URL) {
            throw new Error('PUBLIC_INDEXER_BASE_URL environment variable is not set');
        }

        // Get pagination parameters
        const page = Math.max(1, parseInt(url.searchParams.get('page') || '1'));
        const count = Math.min(100, Math.max(1, parseInt(url.searchParams.get('count') || '20')));
        console.log('Pagination params:', { page, count });

        // Calculate the starting epoch
        const startEpoch = (page - 1) * count + 1;
        const epochs = Array.from({ length: count }, (_, i) => startEpoch + i);
        console.log('Fetching epochs:', epochs);

        // Fetch AdaPot details for each epoch
        const adapots = await Promise.all(
            epochs.map(async (epoch) => {
                try {
                    const apiUrl = `${INDEXER_BASE_URL}/api/v1/adapot/epochs/${epoch}`;
                    console.log('Fetching:', apiUrl);
                    const response = await fetch(apiUrl);
                    if (!response.ok) {
                        console.log(`Failed to fetch epoch ${epoch}:`, response.status, response.statusText);
                        return null;
                    }
                    const data: AdaPotData = await response.json();
                    console.log(`Successfully fetched epoch ${epoch}:`, data);
                    return data;
                } catch (error) {
                    console.error(`Error fetching epoch ${epoch}:`, error);
                    return null;
                }
            })
        );

        // Filter out any failed requests
        const validAdapots = adapots.filter((adapot): adapot is NonNullable<typeof adapot> => adapot !== null);
        console.log('Valid adapots:', validAdapots.length);

        if (validAdapots.length === 0) {
            console.log('No valid adapots found for the requested epochs');
        }

        return json(validAdapots);
    } catch (error) {
        console.error('Error in GET handler:', error);
        return json(
            { error: error instanceof Error ? error.message : 'Failed to fetch AdaPot list' },
            { status: 500 }
        );
    }
}; 