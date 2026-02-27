import type { PageServerLoad } from './$types';
import { env } from '$env/dynamic/public';

export const load: PageServerLoad = async () => {
    const INDEXER_BASE_URL = env.PUBLIC_INDEXER_BASE_URL;

    const [proposalsRes, committeeRes, constitutionRes, epochRes] = await Promise.allSettled([
        fetch(`${INDEXER_BASE_URL}/governance-state/proposals?page=1&count=5&order=desc`),
        fetch(`${INDEXER_BASE_URL}/governance/committees/current`),
        fetch(`${INDEXER_BASE_URL}/governance/constitution`),
        fetch(`${INDEXER_BASE_URL}/epochs/latest`)
    ]);

    const proposals = proposalsRes.status === 'fulfilled' && proposalsRes.value.ok
        ? await proposalsRes.value.json()
        : [];

    const committee = committeeRes.status === 'fulfilled' && committeeRes.value.ok
        ? await committeeRes.value.json()
        : null;

    const constitution = constitutionRes.status === 'fulfilled' && constitutionRes.value.ok
        ? await constitutionRes.value.json()
        : null;

    const epochData = epochRes.status === 'fulfilled' && epochRes.value.ok
        ? await epochRes.value.json()
        : null;

    const currentEpoch = epochData?.epoch ?? 0;

    // Compute active committee member count: deduplicate by hash, keep only active (not expired)
    let activeCommitteeCount = 0;
    if (committee?.members) {
        const uniqueHashes = new Set<string>();
        for (const member of committee.members) {
            if (!uniqueHashes.has(member.hash) && member.expired_epoch >= currentEpoch) {
                uniqueHashes.add(member.hash);
            }
        }
        activeCommitteeCount = uniqueHashes.size;
    }

    return {
        proposals,
        committee,
        constitution,
        activeCommitteeCount,
        currentEpoch
    };
};
