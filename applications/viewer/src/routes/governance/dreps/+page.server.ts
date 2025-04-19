import type { PageServerLoad } from './$types';
import { env } from '$env/dynamic/public';

export const load: PageServerLoad = async ({ url }) => {
    const page = url.searchParams.get('page') || '1';
    const count = url.searchParams.get('count') || '10';
    const order = url.searchParams.get('order') || 'desc';

    try {
        const INDEXER_BASE_URL = env.PUBLIC_INDEXER_BASE_URL;
        const response = await fetch(`${INDEXER_BASE_URL}/governance-state/dreps?page=${page}&count=${count}&order=${order}`);
        
        if (!response.ok) {
            throw new Error(`Failed to fetch dreps (Status: ${response.status})`);
        }

        const dreps = await response.json();

        return {
            dreps,
            page,
            count,
            order
        };
    } catch (error) {
        console.error('Error loading dreps:', error);
        return {
            dreps: [],
            page,
            count,
            order,
            error: error instanceof Error ? error.message : 'An error occurred while loading dreps'
        };
    }
}; 