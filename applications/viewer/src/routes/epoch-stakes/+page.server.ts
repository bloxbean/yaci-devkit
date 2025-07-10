import type { PageServerLoad } from './$types';
import { env } from '$env/dynamic/public';

export const load: PageServerLoad = async ({ fetch, url }) => {
    const page = url.searchParams.get('page') || '1';
    const count = url.searchParams.get('count') || '15';
    const epoch = url.searchParams.get('epoch');

    try {
        // Fetch current epoch from chain tip
        const response = await fetch(`${env.PUBLIC_INDEXER_BASE_URL}/api/v1/chain/tip`);
        if (!response.ok) throw new Error('Failed to fetch chain tip');
        const data = await response.json();
        
        return {
            currentEpoch: data.epoch,
            page,
            count
        };
    } catch (error) {
        console.error('Error loading current epoch:', error);
        return {
            currentEpoch: 0,
            page,
            count
        };
    }
}; 