import { AnchorError, fetchAnchor } from '$lib/governance/anchor.server';
import type { PageServerLoad } from './$types';

/*
 * Fetches the document behind an on-chain anchor. All the safety rules (allowed schemes and ports, no private or
 * local addresses, checked again after DNS and on redirects, size and time limits) live in anchor.server.ts.
 */
export const load: PageServerLoad = async ({ url }) => {
	const target = url.searchParams.get('url') ?? '';
	const expected = (url.searchParams.get('hash') ?? '').toLowerCase();
	if (!target) return { target, expected, result: null, error: 'No anchor URL given.' };
	try {
		const result = await fetchAnchor(target);
		return { target, expected, result, error: null };
	} catch (e) {
		return { target, expected, result: null, error: e instanceof AnchorError ? e.message : 'Could not fetch the document.' };
	}
};
