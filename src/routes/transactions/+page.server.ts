import { variables } from '$lib/variables'
import type { PageLoad } from './$types'

export const load: PageLoad = async ({params}) => {
    const INDEXER_BASE_URL = import.meta.env.VITE_INDEXER_BASE_URL;
    const url = `${INDEXER_BASE_URL}/txs?page=0&count=10`;
    console.log(url);

    const res = await fetch(url);
    const data = await res.json();

    const {transaction_summaries} = data;
    console.log(transaction_summaries);

    if (res.ok) {
        return {
            txs : transaction_summaries
        }
    }

    return {
        status: 404,
        body: { error: 'Can not fetch recepie.' }
    };

}
