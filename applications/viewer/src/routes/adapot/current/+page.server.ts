import type { PageServerLoad } from './$types';
import { env } from '$env/dynamic/public';

export const load: PageServerLoad = async () => {
    try {
        const baseUrl = env.PUBLIC_INDEXER_BASE_URL;
        const response = await fetch(`${baseUrl}/adapot`);

        if (response.status === 404) {
            return {
                adapot: null,
                error: 'No AdaPot data available yet. AdaPot is calculated at epoch boundaries — wait for the first epoch to complete.'
            };
        }

        if (!response.ok) {
            return { adapot: null, error: 'Failed to fetch current AdaPot' };
        }

        const data = await response.json();
        return { adapot: data, error: null };
    } catch (e) {
        return {
            adapot: null,
            error: e instanceof Error ? e.message : 'An error occurred while loading AdaPot'
        };
    }
};
