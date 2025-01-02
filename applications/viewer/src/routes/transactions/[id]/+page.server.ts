import type { PageLoad } from './$types'
import { env } from '$env/dynamic/public';

export const load: PageLoad = async ({params}) => {
   const INDEXER_BASE_URL = env.PUBLIC_INDEXER_BASE_URL;
   const txApiUrl = `${INDEXER_BASE_URL}/txs/${params.id}`;
   const scriptApiUrl = `${INDEXER_BASE_URL}/txs/${params.id}/scripts`;
   const metadataApiUrl = `${INDEXER_BASE_URL}/txs/${params.id}/metadata`;

   const res = await fetch(txApiUrl);
   const scriptRes = await fetch(scriptApiUrl);
   const metadataRes = await fetch(metadataApiUrl);
   const tx = await res.json();
   const scripts = await scriptRes.json();
   const metadata = await metadataRes.json();

   console.log(tx);
   console.log(scripts);
   console.log(metadata);

    if (res.ok) {
        return {
            "tx": tx,
            "contracts": scripts,
            "metadata": metadata
        };
    }

    return {
        status: 404,
        body: { error: 'Can not fetch transaction.' }
    };

}
