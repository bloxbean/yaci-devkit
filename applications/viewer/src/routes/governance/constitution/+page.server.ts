import { loadConstitution } from '$lib/governance/data';
import type { PageServerLoad } from './$types';

export const load: PageServerLoad = async ({ fetch }) => ({ constitution: await loadConstitution(fetch) });
