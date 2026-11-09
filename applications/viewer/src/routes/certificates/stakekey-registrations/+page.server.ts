import { loadCerts } from '$lib/staking/certs';
import type { PageServerLoad } from './$types';

export const load: PageServerLoad = ({ url, fetch }) => loadCerts('stakekey-registrations', url, fetch);
