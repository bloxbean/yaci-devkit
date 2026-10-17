import { loadTransaction } from '$lib/tx/load.server';
import type { PageServerLoad } from './$types';

/* The what-if lab for a transaction: /transactions/{hash}/fork. */
export const load: PageServerLoad = ({ params, fetch }) => loadTransaction(params.id, fetch);
