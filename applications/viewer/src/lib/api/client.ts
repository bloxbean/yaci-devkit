import { env } from '$env/dynamic/public';
import { browser } from '$app/environment';

/*
 * One way to call Yaci Store, from server loads and from the browser.
 * In Docker the server reaches the store by its service name, while the browser needs
 * PUBLIC_INDEXER_CLIENT_BASE_URL (localhost), so the base depends on where the code runs.
 */

const DEFAULT_BASE = 'http://localhost:8080/api/v1';

export function apiBase(): string {
	const base = browser
		? env.PUBLIC_INDEXER_CLIENT_BASE_URL || env.PUBLIC_INDEXER_BASE_URL
		: env.PUBLIC_INDEXER_BASE_URL;
	return (base || DEFAULT_BASE).replace(/\/+$/, '');
}

export function wsUrl(): string | null {
	return env.PUBLIC_INDEXER_WS_URL || null;
}

export class ApiError extends Error {
	constructor(
		message: string,
		public status: number,
		public path: string
	) {
		super(message);
	}
}

type Params = Record<string, string | number | boolean | null | undefined>;

export interface ApiOptions {
	fetch?: typeof fetch;
	params?: Params;
	timeoutMs?: number;
	signal?: AbortSignal;
}

function buildUrl(path: string, params?: Params): string {
	const url = new URL(apiBase() + (path.startsWith('/') ? path : `/${path}`));
	for (const [k, v] of Object.entries(params ?? {})) {
		if (v !== undefined && v !== null && v !== '') url.searchParams.set(k, String(v));
	}
	return url.toString();
}

/** GET a JSON resource. Throws ApiError on HTTP errors and timeouts. */
export async function api<T = unknown>(path: string, opts: ApiOptions = {}): Promise<T> {
	const f = opts.fetch ?? fetch;
	const ctrl = new AbortController();
	const timer = setTimeout(() => ctrl.abort(), opts.timeoutMs ?? 20000);
	opts.signal?.addEventListener('abort', () => ctrl.abort());
	const url = buildUrl(path, opts.params);
	try {
		const res = await f(url, { signal: ctrl.signal, headers: { accept: 'application/json' } });
		if (!res.ok) {
			let detail = res.statusText;
			try {
				const body = await res.json();
				detail = body?.detail || body?.message || body?.error || detail;
			} catch {
				/* not JSON */
			}
			throw new ApiError(detail || `HTTP ${res.status}`, res.status, path);
		}
		const text = await res.text();
		if (!text) return null as T;
		try {
			return JSON.parse(text) as T;
		} catch {
			return text as unknown as T;
		}
	} catch (e) {
		if (e instanceof ApiError) throw e;
		const aborted = (e as Error)?.name === 'AbortError';
		throw new ApiError(aborted ? 'The request timed out' : 'Yaci Store is not reachable', 0, path);
	} finally {
		clearTimeout(timer);
	}
}

/** Like api(), but returns null instead of throwing. For optional sections of a page. */
export async function tryApi<T = unknown>(path: string, opts: ApiOptions = {}): Promise<T | null> {
	try {
		return await api<T>(path, opts);
	} catch {
		return null;
	}
}

/** Read ?page=&count= from a URL. Pages are 1-based in the UI. */
export function paging(url: URL, defaultCount = 20, maxCount = 100): { page: number; count: number } {
	const page = Math.max(1, parseInt(url.searchParams.get('page') ?? '1', 10) || 1);
	const count = Math.min(maxCount, Math.max(1, parseInt(url.searchParams.get('count') ?? '', 10) || defaultCount));
	return { page, count };
}

/** Normalise list responses: plain arrays, or objects wrapping the list under a known key. */
export function listOf<T = any>(data: unknown, ...keys: string[]): T[] {
	if (Array.isArray(data)) return data as T[];
	if (data && typeof data === 'object') {
		for (const k of keys) {
			const v = (data as Record<string, unknown>)[k];
			if (Array.isArray(v)) return v as T[];
		}
	}
	return [];
}
