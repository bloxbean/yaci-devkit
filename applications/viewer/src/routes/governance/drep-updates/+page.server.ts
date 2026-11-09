import { loadCerts } from '$lib/governance/certs';
import type { PageServerLoad } from './$types';

export const load: PageServerLoad = ({ url, fetch }) => loadCerts('updates', url, fetch);
