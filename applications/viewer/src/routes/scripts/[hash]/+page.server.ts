import type { PageServerLoad } from './$types';
import { env } from '$env/dynamic/public';
import { error } from '@sveltejs/kit';

export const load: PageServerLoad = async ({ params, fetch }) => {
    const hash = params.hash;

    if (!/^[0-9a-fA-F]{56}$/.test(hash)) {
        throw error(400, 'Invalid script hash. Expected 56 hex characters.');
    }

    const INDEXER_BASE_URL = env.PUBLIC_INDEXER_BASE_URL;

    const [scriptInfo, scriptJson, scriptCbor] = await Promise.all([
        fetch(`${INDEXER_BASE_URL}/scripts/${hash}`).then(r => r.ok ? r.json() : null),
        fetch(`${INDEXER_BASE_URL}/scripts/${hash}/json`).then(r => r.ok ? r.json() : null),
        fetch(`${INDEXER_BASE_URL}/scripts/${hash}/cbor`).then(r => r.ok ? r.json() : null),
    ]);

    if (!scriptInfo) {
        throw error(404, 'Script not found');
    }

    return {
        hash,
        scriptInfo,
        scriptJson,
        scriptCbor,
    };
};
