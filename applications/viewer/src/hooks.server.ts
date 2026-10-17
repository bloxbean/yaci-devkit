import type { Handle } from '@sveltejs/kit';
import { linkHeaderToTags, injectIntoHead } from '$lib/server/preloads';

/*
 * SvelteKit sends a page's CSS and JS preloads as one `Link` response header (it only writes them into <head> when
 * prerendering). On larger pages that header passes 4 KB, the default header buffer of nginx (proxy_buffer_size), so
 * a viewer behind a default reverse proxy fails with 502 "upstream sent too big header". The same preloads go into
 * the page's <head> as <link> tags instead, while the HTML streams through, and the header is dropped.
 */
export const handle: Handle = async ({ event, resolve }) => {
	const response = await resolve(event);
	const link = response.headers.get('link');
	if (!link) return response;
	const headers = new Headers(response.headers);
	headers.delete('link');
	const tags = linkHeaderToTags(link);
	const html = response.headers.get('content-type')?.startsWith('text/html');
	if (!html || !response.body || !tags) return new Response(response.body, { status: response.status, statusText: response.statusText, headers });
	headers.delete('content-length');
	return new Response(injectIntoHead(response.body, tags), { status: response.status, statusText: response.statusText, headers });
};
