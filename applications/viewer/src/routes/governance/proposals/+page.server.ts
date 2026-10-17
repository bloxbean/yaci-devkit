import { error } from '@sveltejs/kit';
import { api, paging } from '$lib/api/client';
import { ACTION_LABEL, toProposal } from '$lib/governance/labels';
import type { PageServerLoad } from './$types';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

/*
 * All governance actions (with status), only the ones being voted on now, or one action type.
 * /governance-state/proposals and /governance/proposals/gov-action-type are 1-based; /current is not paged.
 */
export const load: PageServerLoad = async ({ url, fetch }) => {
	const view = url.searchParams.get('view') === 'live' ? 'live' : 'all';
	const type = url.searchParams.get('type') ?? '';
	const validType = type in ACTION_LABEL ? type : '';
	const { page, count } = paging(url, 20);
	const order = url.searchParams.get('order') === 'asc' ? 'asc' : 'desc';
	try {
		let rows: Any[];
		if (view === 'live') rows = (await api<Any[]>('/governance-state/proposals/current', { fetch })) ?? [];
		else if (validType) rows = (await api<Any[]>(`/governance/proposals/gov-action-type/${validType}`, { fetch, params: { page, count, order } })) ?? [];
		else rows = (await api<Any[]>('/governance-state/proposals', { fetch, params: { page, count, order } })) ?? [];
		return { view, type: validType, page, count, order, paged: view !== 'live', proposals: rows.map(toProposal) };
	} catch (e) {
		error(503, (e as Error).message);
	}
};
