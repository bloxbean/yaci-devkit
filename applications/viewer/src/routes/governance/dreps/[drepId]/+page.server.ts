import type { PageServerLoad } from './$types';
import { env } from '$env/dynamic/public';

export const load: PageServerLoad = async ({ params, url }) => {
    const INDEXER_BASE_URL = env.PUBLIC_INDEXER_BASE_URL;
    const drepId = params.drepId;
    const page = url.searchParams.get('page') || '1';
    const count = url.searchParams.get('count') || '15';

    const [drepRes, delegationsRes] = await Promise.allSettled([
        fetch(`${INDEXER_BASE_URL}/governance-state/dreps/${drepId}`),
        fetch(`${INDEXER_BASE_URL}/governance/delegation-votes/drep/${drepId}?page=${page}&count=${count}`)
    ]);

    const drep = drepRes.status === 'fulfilled' && drepRes.value.ok
        ? await drepRes.value.json()
        : null;

    const delegations = delegationsRes.status === 'fulfilled' && delegationsRes.value.ok
        ? await delegationsRes.value.json()
        : [];

    return {
        drepId,
        drep,
        delegations: Array.isArray(delegations) ? delegations : [],
        page,
        count
    };
};
