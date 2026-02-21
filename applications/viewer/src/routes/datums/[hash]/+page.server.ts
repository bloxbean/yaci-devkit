import type { PageServerLoad } from './$types';
import { env } from '$env/dynamic/public';
import { error } from '@sveltejs/kit';

export const load: PageServerLoad = async ({ params, fetch }) => {
    const hash = params.hash;

    if (!/^[0-9a-fA-F]{64}$/.test(hash)) {
        throw error(400, 'Invalid datum hash. Expected 64 hex characters.');
    }

    const INDEXER_BASE_URL = env.PUBLIC_INDEXER_BASE_URL;

    const [datumJson, datumCbor] = await Promise.all([
        fetch(`${INDEXER_BASE_URL}/scripts/datum/${hash}`).then(r => r.ok ? r.json() : null),
        fetch(`${INDEXER_BASE_URL}/scripts/datum/${hash}/cbor`).then(r => r.ok ? r.json() : null),
    ]);

    if (!datumJson && !datumCbor) {
        throw error(404, 'Datum not found');
    }

    return {
        hash,
        datumJson,
        datumCbor,
    };
};
