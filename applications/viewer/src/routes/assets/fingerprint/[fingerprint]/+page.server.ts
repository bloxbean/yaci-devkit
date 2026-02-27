import { error } from '@sveltejs/kit';
import type { PageServerLoad } from './$types';
import { env } from '$env/dynamic/public';

const INDEXER_BASE_URL = env.PUBLIC_INDEXER_BASE_URL;
const DEFAULT_COUNT = 15;

export const load: PageServerLoad = async ({ params, url, fetch }) => {
    const fingerprint = params.fingerprint;
    const page = parseInt(url.searchParams.get('page') || '1');
    const count = parseInt(url.searchParams.get('count') || String(DEFAULT_COUNT));

    if (!fingerprint || !fingerprint.startsWith('asset1')) {
        throw error(400, 'Invalid fingerprint. Must start with "asset1".');
    }
    if (isNaN(page) || page < 1) {
        throw error(400, 'Invalid page number');
    }
    if (isNaN(count) || count < 1 || count > 100) {
        throw error(400, 'Invalid count');
    }

    const fetchSupply = async () => {
        try {
            const res = await fetch(`${INDEXER_BASE_URL}/assets/fingerprint/${fingerprint}/supply`);
            if (res.ok) return await res.json();
        } catch { /* ignore */ }
        return null;
    };

    const fetchHistory = async () => {
        try {
            const res = await fetch(`${INDEXER_BASE_URL}/assets/fingerprint/${fingerprint}/history?page=${page - 1}&count=${count}`);
            if (res.ok) return await res.json();
        } catch { /* ignore */ }
        return [];
    };

    const [supply, history] = await Promise.all([
        fetchSupply(),
        fetchHistory()
    ]);

    // Derive unit from the first history entry for linking to full detail
    const unit = history.length > 0 ? history[0].unit : null;

    return {
        fingerprint,
        supply,
        history,
        unit,
        currentPage: page,
        count
    };
};
