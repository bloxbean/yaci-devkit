// import { error } from '@sveltejs/kit';
// import { indexer_base_url } from '@sveltejs/'
//
// /** @type {import('./$types').PageLoad} */
// export function load({ params }) {
//     fetch()
//     if (params.id) {
//         return {
//             txId: params.id
//         };
//     }
//
//     throw error(404, 'Not found');
// }

// import { INDEXER_BASE_URL } from '$env/static/public'
import { variables } from '$lib/variables'
import type { PageLoad } from './$types'

export const load: PageLoad = async ({params}) => {
   const INDEXER_BASE_URL = import.meta.env.VITE_INDEXER_BASE_URL;
   const url = `${INDEXER_BASE_URL}/txs/${params.id}`;
   console.log(url);

   const res = await fetch(url);
   const tx = await res.json();

   console.log(tx);
    if (res.ok) {
        return tx;
    }

    return {
        status: 404,
        body: { error: 'Can not fetch recepie.' }
    };

}
