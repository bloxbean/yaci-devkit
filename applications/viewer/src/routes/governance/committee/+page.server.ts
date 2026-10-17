import { loadCommittee } from '$lib/governance/data';
import type { PageServerLoad } from './$types';

export const load: PageServerLoad = async ({ fetch }) => ({ committee: await loadCommittee(fetch) });
