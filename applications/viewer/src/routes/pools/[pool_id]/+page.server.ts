import type { PageServerLoad } from './$types';
import { env } from '$env/dynamic/public';

export const load: PageServerLoad = async ({ params }) => {
    const { pool_id } = params;
    const INDEXER_BASE_URL = env.PUBLIC_INDEXER_BASE_URL;

    // First get the latest epoch
    const epochResponse = await fetch(`${INDEXER_BASE_URL}/epochs/latest`);
    const epochData = await epochResponse.json();
    const latestEpoch = epochData.epoch;

    // Then get pool details for this epoch
    const poolResponse = await fetch(`${INDEXER_BASE_URL}/pools/pools/${pool_id}/epochs/${latestEpoch}`);
    const poolData = await poolResponse.json();

    if (poolResponse.ok) {
        return {
            pool: poolData,
            latestEpoch: latestEpoch
        };
    }

    return {
        status: 404,
        error: 'Pool not found'
    };
}; 