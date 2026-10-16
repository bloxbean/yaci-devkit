import { redirect } from '@sveltejs/kit';
import type { PageServerLoad } from './$types';

/* The governance actions list now lives at /governance/proposals; keep old links working. */
export const load: PageServerLoad = ({ url }) => {
	redirect(301, `/governance/proposals${url.search}`);
};
