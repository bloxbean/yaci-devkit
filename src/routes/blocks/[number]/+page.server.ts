import type { PageLoad } from './$types'

export const load: PageLoad = async ({params}) => {
    const INDEXER_BASE_URL = import.meta.env.VITE_INDEXER_BASE_URL;
    const blockApiUrl = `${INDEXER_BASE_URL}/blocks/${params.number}`;

    const res = await fetch(blockApiUrl);
    const block = await res.json();

    console.log(block);

    if (res.ok) {
        return {
            block
        };
    }

    return {
        status: 404,
        body: { error: 'Can not fetch block.' }
    };
}
