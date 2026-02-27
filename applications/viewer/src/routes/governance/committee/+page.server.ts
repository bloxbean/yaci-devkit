import type { PageServerLoad } from './$types';
import { env } from '$env/dynamic/public';

export const load: PageServerLoad = async () => {
    const INDEXER_BASE_URL = env.PUBLIC_INDEXER_BASE_URL;

    try {
        const [committeeRes, epochRes] = await Promise.allSettled([
            fetch(`${INDEXER_BASE_URL}/governance/committees/current`),
            fetch(`${INDEXER_BASE_URL}/epochs/latest`)
        ]);

        const committee = committeeRes.status === 'fulfilled' && committeeRes.value.ok
            ? await committeeRes.value.json()
            : null;

        const epochData = epochRes.status === 'fulfilled' && epochRes.value.ok
            ? await epochRes.value.json()
            : null;

        const currentEpoch = epochData?.epoch ?? 0;

        if (!committee) {
            throw new Error('Failed to fetch committee data');
        }

        return { committee, currentEpoch };
    } catch (error) {
        console.error('Error loading committee:', error);
        return {
            committee: null,
            currentEpoch: 0,
            error: error instanceof Error ? error.message : 'An error occurred while loading committee'
        };
    }
};
