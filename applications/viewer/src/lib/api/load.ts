import { error } from '@sveltejs/kit';
import { ApiError, api, type ApiOptions } from './client';

/*
 * For a page's main resource: 404 becomes a "not found" page, an unreachable store becomes a 503 with a clear
 * message, anything else a 502.
 */
export async function must<T>(path: string, opts: ApiOptions, notFound: string): Promise<T> {
	try {
		const data = await api<T>(path, opts);
		if (data === null || data === undefined) error(404, notFound);
		return data;
	} catch (e) {
		if (e instanceof ApiError) {
			if (e.status === 404 || e.status === 400) error(404, notFound);
			if (e.status === 0) error(503, `${e.message}. Check that Yaci Store is running.`);
			error(502, `Yaci Store: ${e.message}`);
		}
		throw e;
	}
}
