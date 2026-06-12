import type { PageServerLoad } from './$types';
import { env } from '$env/dynamic/public';

export const load: PageServerLoad = async ({ fetch }) => {
    const INDEXER_BASE_URL = env.PUBLIC_INDEXER_BASE_URL;

    try {
        const response = await fetch(`${INDEXER_BASE_URL}/governance/constitution`);
        if (response.ok) {
            const constitution = await response.json();
            return { constitution };
        }

        if (response.status !== 404) {
            throw new Error(`Failed to fetch constitution (Status: ${response.status})`);
        }

        const liveResponse = await fetch(`${INDEXER_BASE_URL}/governance/live/constitution`);
        if (liveResponse.status === 404) {
            return { constitution: null };
        }
        if (!liveResponse.ok) {
            throw new Error(`Failed to fetch live constitution (Status: ${liveResponse.status})`);
        }

        const liveConstitution = await liveResponse.json();
        return {
            constitution: {
                active_epoch: liveConstitution.active_epoch ?? liveConstitution.epoch,
                anchor_url: liveConstitution.anchor_url,
                anchor_hash: liveConstitution.anchor_hash,
                script: liveConstitution.script,
                slot: liveConstitution.slot
            }
        };
    } catch (error) {
        console.error('Error loading constitution:', error);
        return {
            constitution: null,
            error: error instanceof Error ? error.message : 'An error occurred while loading constitution'
        };
    }
};
