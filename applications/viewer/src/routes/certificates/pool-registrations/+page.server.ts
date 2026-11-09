import { loadCerts } from '$lib/staking/certs';
import type { PageServerLoad } from './$types';

export const load: PageServerLoad = ({ url, fetch }) => loadCerts('pool-registrations', url, fetch);
