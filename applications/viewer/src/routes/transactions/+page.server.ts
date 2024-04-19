import { variables } from '$lib/variables'
import type { PageLoad } from './$types'

export const load: PageLoad = async ({params, url}) => {
    let page = url.searchParams.get('page');
    if (!page) page = 0;
    const count = 20;

    const INDEXER_BASE_URL = import.meta.env.VITE_INDEXER_BASE_URL;
    const apiUrl = `${INDEXER_BASE_URL}/txs?page=${page}&count=${count}`;
    console.log(apiUrl);

    const res = await fetch(apiUrl);
    const data = await res.json();

    const {total, total_pages, transaction_summaries} = data;
    console.log(transaction_summaries);

    if (res.ok) {
        return {
            txs : transaction_summaries,
            total: total,
            total_pages: total_pages,
            page: page,
            count: count
        }
    }

    return {
        status: 404,
        body: { error: 'Can not fetch recepie.' }
    };

}
