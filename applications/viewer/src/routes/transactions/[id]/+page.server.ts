import { loadTransaction } from '$lib/tx/load.server';
import type { PageServerLoad } from './$types';

export const load: PageServerLoad = ({ params, fetch }) => loadTransaction(params.id, fetch);
