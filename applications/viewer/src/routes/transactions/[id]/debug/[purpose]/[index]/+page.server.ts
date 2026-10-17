import { error } from '@sveltejs/kit';
import { loadTransaction } from '$lib/tx/load.server';
import type { PageServerLoad } from './$types';

const PURPOSES = ['spend', 'mint', 'cert', 'reward', 'vote', 'propose'];

/* The debugger for one script run of a transaction: /transactions/{hash}/debug/{purpose}/{index}. */
export const load: PageServerLoad = async ({ params, fetch }) => {
	const purpose = params.purpose.toLowerCase();
	if (!PURPOSES.includes(purpose)) error(404, `Unknown script purpose “${params.purpose}”. Use one of ${PURPOSES.join(', ')}.`);
	if (!/^\d{1,4}$/.test(params.index)) error(404, 'The redeemer index must be a number.');
	return { ...(await loadTransaction(params.id, fetch)), purpose, index: Number(params.index) };
};
