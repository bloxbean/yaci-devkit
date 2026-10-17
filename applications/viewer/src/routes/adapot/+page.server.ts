import { redirect } from '@sveltejs/kit';
import type { PageServerLoad } from './$types';

/* /adapot is kept as an alias for the current ada pots page. */
export const load: PageServerLoad = async ({ url }) => {
	redirect(308, `/adapot/current${url.search}`);
};
