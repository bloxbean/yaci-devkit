import { error } from '@sveltejs/kit';
import type { PageServerLoad } from './$types';
import { env } from '$env/dynamic/public';

const INDEXER_BASE_URL = env.PUBLIC_INDEXER_BASE_URL;
const DEFAULT_COUNT = 15;

export const load: PageServerLoad = async ({ params, url, fetch }) => {
    const policy = params.policy;
    const page = parseInt(url.searchParams.get('page') || '1');
    const count = parseInt(url.searchParams.get('count') || String(DEFAULT_COUNT));

    if (!policy || !/^[0-9a-fA-F]{56}$/.test(policy)) {
        throw error(400, 'Invalid policy ID. Must be exactly 56 hex characters.');
    }
    if (isNaN(page) || page < 1) {
        throw error(400, 'Invalid page number');
    }
    if (isNaN(count) || count < 1 || count > 100) {
        throw error(400, 'Invalid count');
    }

    const fetchSupply = async () => {
        try {
            const res = await fetch(`${INDEXER_BASE_URL}/assets/policy/${policy}/supply`);
            if (res.ok) return await res.json();
        } catch { /* ignore */ }
        return null;
    };

    const fetchHistory = async () => {
        try {
            const res = await fetch(`${INDEXER_BASE_URL}/assets/policy/${policy}/history?page=${page - 1}&count=${count}`);
            if (res.ok) return await res.json();
        } catch { /* ignore */ }
        return [];
    };

    const [supply, history] = await Promise.all([
        fetchSupply(),
        fetchHistory()
    ]);

    return {
        policy,
        supply,
        history,
        currentPage: page,
        count
    };
};
