import type { PageServerLoad } from './$types';
import { env } from '$env/dynamic/public';

export const load: PageServerLoad = async () => {
    const INDEXER_BASE_URL = env.PUBLIC_INDEXER_BASE_URL;

    try {
        const response = await fetch(`${INDEXER_BASE_URL}/governance/constitution`);
        if (!response.ok) {
            throw new Error(`Failed to fetch constitution (Status: ${response.status})`);
        }
        const constitution = await response.json();
        return { constitution };
    } catch (error) {
        console.error('Error loading constitution:', error);
        return {
            constitution: null,
            error: error instanceof Error ? error.message : 'An error occurred while loading constitution'
        };
    }
};
