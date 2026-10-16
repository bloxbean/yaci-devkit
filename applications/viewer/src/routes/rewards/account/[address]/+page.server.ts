import { redirect } from '@sveltejs/kit';
import type { PageServerLoad } from './$types';

/* The stake account page now shows reward history; keep old links working. */
export const load: PageServerLoad = ({ params }) => {
	redirect(301, `/stake/${params.address.trim()}?tab=rewards`);
};
