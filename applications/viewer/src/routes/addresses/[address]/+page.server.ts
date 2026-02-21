import { error, redirect } from '@sveltejs/kit';
import type { PageServerLoad } from './$types';
import { env } from '$env/dynamic/public';

const INDEXER_BASE_URL = env.PUBLIC_INDEXER_BASE_URL;
const DEFAULT_COUNT = 15;
const DEFAULT_ORDER = 'desc';

interface Amount {
    unit: string;
    quantity: number;
}

interface Utxo {
    tx_hash: string;
    output_index: number;
    address: string;
    amount: Array<{ unit: string; quantity: string }>;
    inline_datum?: string;
    data_hash?: string;
    script_ref?: string;
}

interface Transaction {
    tx_hash: string;
    block_height: number;
    block_time: number;
}

export const load: PageServerLoad = async ({ params, url, fetch }) => {
    const address = params.address;

    // Redirect stake addresses to dedicated stake page
    if (address.startsWith('stake')) {
        throw redirect(302, `/stake/${address}`);
    }

    const page = parseInt(url.searchParams.get('page') || '1');
    const count = parseInt(url.searchParams.get('count') || String(DEFAULT_COUNT));
    const order = url.searchParams.get('order') || DEFAULT_ORDER;
    const tab = url.searchParams.get('tab') || 'utxos';
    const balanceApi = url.searchParams.get('balanceApi') || 'auto';

    if (!address) {
        throw error(400, 'Address is required');
    }
    if (isNaN(page) || page < 1) {
        throw error(400, 'Invalid page number');
    }
    if (isNaN(count) || count < 1 || count > 100) {
        throw error(400, 'Invalid count number');
    }
    if (order !== 'asc' && order !== 'desc') {
        throw error(400, 'Invalid order parameter');
    }

    const fetchAmounts = async (): Promise<{ data: Amount[]; balanceApiUsed: 'balance' | 'amounts' }> => {
        // Try /balance first if auto or balance
        if (balanceApi === 'auto' || balanceApi === 'balance') {
            try {
                const balanceUrl = `${INDEXER_BASE_URL}/addresses/${address}/balance`;
                const response = await fetch(balanceUrl);
                if (response.ok) {
                    const data: Amount[] = await response.json();
                    return { data, balanceApiUsed: 'balance' };
                }
            } catch {
                // Fall through to /amounts
            }
        }

        // Fall back to /amounts
        try {
            const amountsUrl = `${INDEXER_BASE_URL}/addresses/${address}/amounts`;
            const response = await fetch(amountsUrl);
            if (response.ok) {
                const data: Amount[] = await response.json();
                return { data, balanceApiUsed: 'amounts' };
            }
        } catch {
            // Return empty
        }

        return { data: [], balanceApiUsed: 'amounts' };
    };

    const fetchUtxos = async () => {
        const apiUrl = `${INDEXER_BASE_URL}/addresses/${address}/utxos?page=${page - 1}&count=${count}&order=${order}`;
        try {
            const response = await fetch(apiUrl);
            if (!response.ok) {
                return { data: [], error: `UTXOs: ${response.statusText}` };
            }
            const data: Utxo[] = await response.json();
            // Transform amount entries: map {unit, quantity} to {asset_name, quantity}
            // so they are compatible with the AmountBadges component
            const transformed = data.map(utxo => ({
                ...utxo,
                amount: utxo.amount.map(a => ({ asset_name: a.unit, quantity: a.quantity, unit: a.unit }))
            }));
            return { data: transformed, error: null };
        } catch (err) {
            return { data: [], error: 'Failed to fetch UTXOs' };
        }
    };

    const fetchTransactions = async () => {
        const apiUrl = `${INDEXER_BASE_URL}/addresses/${address}/transactions?page=${page - 1}&count=${count}&order=${order}`;
        try {
            const response = await fetch(apiUrl);
            if (!response.ok) {
                return { data: [], error: `Transactions: ${response.statusText}` };
            }
            const data: Transaction[] = await response.json();
            return { data, error: null };
        } catch (err) {
            return { data: [], error: 'Failed to fetch transactions' };
        }
    };

    const [amountsResult, utxosResult, transactionsResult] = await Promise.all([
        fetchAmounts(),
        fetchUtxos(),
        fetchTransactions()
    ]);

    const errors = [
        utxosResult.error,
        transactionsResult.error
    ].filter(e => e !== null);
    const combinedError = errors.length > 0 ? errors.join('; ') : null;

    return {
        address,
        amounts: amountsResult.data,
        balanceApiUsed: amountsResult.balanceApiUsed,
        utxos: utxosResult.data,
        transactions: transactionsResult.data,
        currentPage: page,
        count,
        order,
        tab,
        error: combinedError
    };
};
