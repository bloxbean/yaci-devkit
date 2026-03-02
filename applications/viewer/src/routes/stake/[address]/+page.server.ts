import { error } from '@sveltejs/kit';
import type { PageServerLoad } from './$types';
import { env } from '$env/dynamic/public';

const INDEXER_BASE_URL = env.PUBLIC_INDEXER_BASE_URL;
const DEFAULT_COUNT = 15;
const DEFAULT_ORDER = 'desc';

export const load: PageServerLoad = async ({ params, url, fetch }) => {
    const address = params.address;

    if (!address || !address.startsWith('stake')) {
        throw error(400, 'Invalid stake address');
    }

    const page = parseInt(url.searchParams.get('page') || '1');
    const count = parseInt(url.searchParams.get('count') || String(DEFAULT_COUNT));
    const order = url.searchParams.get('order') || DEFAULT_ORDER;
    const tab = url.searchParams.get('tab') || 'rewards';

    if (isNaN(page) || page < 1) {
        throw error(400, 'Invalid page number');
    }
    if (isNaN(count) || count < 1 || count > 100) {
        throw error(400, 'Invalid count number');
    }
    if (order !== 'asc' && order !== 'desc') {
        throw error(400, 'Invalid order parameter');
    }

    const [rewards, rewardRest, withdrawals] = await Promise.all([
        fetch(`${INDEXER_BASE_URL}/accounts/${address}/rewards?page=${page - 1}&count=${count}&order=${order}`)
            .then(r => r.ok ? r.json() : [])
            .catch(() => []),
        fetch(`${INDEXER_BASE_URL}/accounts/${address}/reward_rest?page=${page - 1}&count=${count}&order=${order}`)
            .then(r => r.ok ? r.json() : [])
            .catch(() => []),
        fetch(`${INDEXER_BASE_URL}/accounts/${address}/withdrawals?page=${page - 1}&count=${count}&order=${order}`)
            .then(r => r.ok ? r.json() : [])
            .catch(() => []),
    ]);

    return {
        address,
        rewards,
        rewardRest,
        withdrawals,
        currentPage: page,
        count,
        order,
        tab
    };
};
