import type { PageServerLoad } from './$types';
import { env } from '$env/dynamic/public';

export const load: PageServerLoad = async ({ params, fetch }) => {
    const INDEXER_BASE_URL = env.PUBLIC_INDEXER_BASE_URL;
    const txHash = params.id;

    const [tx, scripts, metadata, withdrawals, mints] = await Promise.all([
        fetch(`${INDEXER_BASE_URL}/txs/${txHash}`).then(r => r.ok ? r.json() : null),
        fetch(`${INDEXER_BASE_URL}/txs/${txHash}/scripts`).then(r => r.ok ? r.json() : []),
        fetch(`${INDEXER_BASE_URL}/txs/${txHash}/metadata`).then(r => r.ok ? r.json() : null),
        fetch(`${INDEXER_BASE_URL}/txs/${txHash}/withdrawals`).then(r => r.ok ? r.json() : []),
        fetch(`${INDEXER_BASE_URL}/assets/txs/${txHash}`).then(r => r.ok ? r.json() : []),
    ]);

    if (tx) {
        return {
            tx,
            contracts: scripts,
            metadata,
            withdrawals,
            mints,
        };
    }

    return {
        status: 404,
        body: { error: 'Can not fetch transaction.' }
    };
};
