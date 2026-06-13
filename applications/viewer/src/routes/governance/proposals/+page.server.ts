import type { PageServerLoad } from './$types';
import { env } from '$env/dynamic/public';
import type { Proposal } from './types';

export const load: PageServerLoad = async ({ url }) => {
    const page = url.searchParams.get('page') || '1';
    const count = url.searchParams.get('count') || '10';
    const order = url.searchParams.get('order') || 'desc';

    try {
        const INDEXER_BASE_URL = env.PUBLIC_INDEXER_BASE_URL;
        if (!INDEXER_BASE_URL) {
            throw new Error('PUBLIC_INDEXER_BASE_URL environment variable is not set');
        }

        const response = await fetch(`${INDEXER_BASE_URL}/governance-state/proposals?page=${page}&count=${count}&order=${order}`);
        if (!response.ok) {
            throw new Error(`Failed to fetch proposals (Status: ${response.status})`);
        }

        const proposals: Proposal[] = await response.json();
        if (!Array.isArray(proposals)) {
            throw new Error('Invalid data format received from server');
        }

        return {
            proposals,
            page,
            count,
            order
        };
    } catch (error) {
        console.error('Error loading proposals:', error);
        return {
            proposals: [],
            page,
            count,
            order,
            error: error instanceof Error ? error.message : 'An error occurred while loading proposals'
        };
    }
};
