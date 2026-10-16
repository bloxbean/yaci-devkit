import { ApiError, api } from '$lib/api/client';

export interface Supply {
	value: number | string | null;
	/** The store said 404: no such asset/policy. */
	notFound: boolean;
	error: string | null;
}

/* Supply for a unit, policy or fingerprint. Big policies can take a while, so it never blocks the page. */
export async function loadSupply(path: string, f: typeof fetch, timeoutMs = 10000): Promise<Supply> {
	try {
		const r = await api<{ supply: number | string }>(path, { fetch: f, timeoutMs });
		return { value: r?.supply ?? null, notFound: false, error: null };
	} catch (e) {
		const notFound = e instanceof ApiError && (e.status === 404 || e.status === 400);
		return { value: null, notFound, error: notFound ? null : (e as Error).message };
	}
}

/* A list for a tab: never throws, so one slow query only affects its own tab. */
export async function loadList<T>(path: string, f: typeof fetch, params: Record<string, string | number>, timeoutMs = 15000) {
	try {
		const rows = await api<T[]>(path, { fetch: f, params, timeoutMs });
		return { rows: rows ?? [], error: null as string | null };
	} catch (e) {
		return { rows: [] as T[], error: (e as Error).message };
	}
}
