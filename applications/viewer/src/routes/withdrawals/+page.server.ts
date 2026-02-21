import type { PageServerLoad } from './$types';
import { env } from '$env/dynamic/public';

export const load: PageServerLoad = async ({ url }) => {
    const page = parseInt(url.searchParams.get('page') || '1');
    const count = 15;

    const INDEXER_BASE_URL = env.PUBLIC_INDEXER_BASE_URL;

    let withdrawals: any[] = [];
    try {
        const res = await fetch(`${INDEXER_BASE_URL}/txs/withdrawals?page=${page}&count=${count}`);
        if (res.ok) {
            withdrawals = await res.json();
        }
    } catch {
        // API may not be available
    }

    return { withdrawals, currentPage: page, count };
};
