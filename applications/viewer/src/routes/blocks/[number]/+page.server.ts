import type { PageLoad } from './$types'

export const load: PageLoad = async ({params}) => {
    const INDEXER_BASE_URL = import.meta.env.VITE_INDEXER_BASE_URL;
    const blockApiUrl = `${INDEXER_BASE_URL}/blocks/${params.number}`;
    const blockTxsApiUrl = `${INDEXER_BASE_URL}/blocks/${params.number}/txs`;

    const res = await fetch(blockApiUrl);
    const resTxs = await fetch(blockTxsApiUrl);
    const block = await res.json();
    const txs = await resTxs.json();

    console.log(block);

    if (res.ok) {
        return {
            block,
            txs,
        };
    }

    return {
        status: 404,
        body: { error: 'Can not fetch block.' }
    };
}
