/*
 * A small in-memory cache that serves stale answers while it refreshes them ("stale while revalidate"), for results
 * that change slowly, like charts over Yaci Store's daily exports.
 *  - Fresh (younger than `ttlMs`): returned as is.
 *  - Stale (older, but younger than `maxStaleMs`): returned at once, and refreshed in the background.
 *  - Missing or too old: loaded, and the caller waits.
 * Concurrent loads of one key share a call. A failed refresh keeps the stale answer, until it is too old.
 * At most `maxEntries` are kept; the least recently used go first, so expired entries never pile up.
 */
export class SwrCache {
	private entries = new Map<string, { value: unknown; at: number }>();
	private loading = new Map<string, Promise<unknown>>();
	private now: () => number;

	constructor(private opts: { maxEntries: number; maxStaleMs: number; now?: () => number }) {
		this.now = opts.now ?? Date.now;
	}

	get<T>(key: string, ttlMs: number, load: () => Promise<T>): Promise<T> {
		const e = this.entries.get(key);
		const age = e ? this.now() - e.at : Infinity;
		if (e && age <= this.opts.maxStaleMs) {
			this.touch(key, e);
			// A failed refresh leaves the stale answer in place; the next request tries again.
			if (age > ttlMs) this.refresh(key, load).catch(() => {});
			return Promise.resolve(e.value as T);
		}
		return this.refresh(key, load);
	}

	private refresh<T>(key: string, load: () => Promise<T>): Promise<T> {
		const pending = this.loading.get(key);
		if (pending) return pending as Promise<T>;
		const p = load()
			.then((value) => (this.touch(key, { value, at: this.now() }), value))
			.finally(() => this.loading.delete(key));
		this.loading.set(key, p);
		return p;
	}

	private touch(key: string, e: { value: unknown; at: number }) {
		this.entries.delete(key);
		this.entries.set(key, e);
		while (this.entries.size > this.opts.maxEntries) this.entries.delete(this.entries.keys().next().value!);
	}
}
