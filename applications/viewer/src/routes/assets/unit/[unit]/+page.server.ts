import { error } from '@sveltejs/kit';
import type { PageServerLoad } from './$types';
import { env } from '$env/dynamic/public';

const INDEXER_BASE_URL = env.PUBLIC_INDEXER_BASE_URL;
const DEFAULT_COUNT = 15;
const DEFAULT_ORDER = 'desc';

export const load: PageServerLoad = async ({ params, url, fetch }) => {
    const unit = params.unit;
    const page = parseInt(url.searchParams.get('page') || '1');
    const count = parseInt(url.searchParams.get('count') || String(DEFAULT_COUNT));
    const order = url.searchParams.get('order') || DEFAULT_ORDER;
    const tab = url.searchParams.get('tab') || 'history';

    if (!unit || !/^[0-9a-fA-F]{57,}$/.test(unit)) {
        throw error(400, 'Invalid asset unit. Must be 57+ hex characters (policy ID + asset name).');
    }
    if (isNaN(page) || page < 1) {
        throw error(400, 'Invalid page number');
    }
    if (isNaN(count) || count < 1 || count > 100) {
        throw error(400, 'Invalid count');
    }

    const fetchSupply = async () => {
        try {
            const res = await fetch(`${INDEXER_BASE_URL}/assets/${unit}/supply`);
            if (res.ok) return await res.json();
        } catch { /* ignore */ }
        return null;
    };

    const fetchHistory = async () => {
        try {
            const res = await fetch(`${INDEXER_BASE_URL}/assets/${unit}/history?page=${page - 1}&count=${count}`);
            if (res.ok) return await res.json();
        } catch { /* ignore */ }
        return [];
    };

    const fetchTransactions = async () => {
        try {
            const res = await fetch(`${INDEXER_BASE_URL}/assets/${unit}/transactions?page=${page - 1}&count=${count}&order=${order}`);
            if (res.ok) return await res.json();
        } catch { /* ignore */ }
        return [];
    };

    const fetchUtxos = async () => {
        try {
            const res = await fetch(`${INDEXER_BASE_URL}/assets/${unit}/utxos?page=${page - 1}&count=${count}&order=${order}`);
            if (res.ok) {
                const data = await res.json();
                return data.map((utxo: any) => ({
                    ...utxo,
                    amount: utxo.amount?.map((a: any) => ({
                        asset_name: a.unit,
                        quantity: a.quantity,
                        unit: a.unit
                    })) || []
                }));
            }
        } catch { /* ignore */ }
        return [];
    };

    const [supply, history, transactions, utxos] = await Promise.all([
        fetchSupply(),
        fetchHistory(),
        fetchTransactions(),
        fetchUtxos()
    ]);

    return {
        unit,
        supply,
        history,
        transactions,
        utxos,
        currentPage: page,
        count,
        order,
        tab
    };
};
