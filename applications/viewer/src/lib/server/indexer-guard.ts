import { ApiError } from '$lib/api/client';

/*
 * Protects a large Yaci Store from the viewer's slowest queries (bloxbean/yaci-store#1269). When the viewer gives up
 * on a request, Yaci Store keeps running its database query, for minutes or hours, so asking again is the most
 * harmful thing the viewer can do. Per endpoint key (the path without paging):
 *  - a timeout marks the key heavy for a while, and calls for it fail at once without reaching the store;
 *  - identical requests in flight share one call;
 *  - successful answers can be kept briefly;
 *  - only a few guarded calls run against the store at once, so many different heavy keys can't pile up.
 * Server-side and in memory, for the single viewer process.
 */

/** The store is known to be too slow for this key: the call wasn't made. */
export class HeavyError extends Error {
	constructor(public key: string) {
		super('Yaci Store is too slow for this request right now');
	}
}

/** All guarded slots stayed busy for the whole timeout: the call wasn't made. */
export class BusyError extends Error {
	constructor() {
		super('Yaci Store is busy with other slow requests');
	}
}

export interface GuardOptions {
	/** How long a timed-out key stays heavy. */
	heavyMs: number;
	/** Most entries kept (heavy keys and cached answers together); the least recently used go first. */
	maxEntries: number;
	/** Most guarded calls running against the store at once. */
	maxConcurrent: number;
	now?: () => number;
}

export interface CallOptions {
	/** Keep a successful answer this long. */
	cacheMs?: number;
	/** The longest to wait for a free slot before giving up with BusyError. */
	waitMs?: number;
	/** How long a timeout marks the key heavy, instead of the guard's default. */
	heavyMs?: number;
}

type Entry = { kind: 'heavy'; until: number } | { kind: 'value'; until: number; value: unknown };

export class IndexerGuard {
	private entries = new Map<string, Entry>();
	private inFlight = new Map<string, Promise<unknown>>();
	private running = 0;
	private waiting: (() => void)[] = [];
	private now: () => number;

	constructor(private opts: GuardOptions) {
		this.now = opts.now ?? Date.now;
	}

	/** Whether calls for this key are being skipped because one timed out recently. */
	isHeavy(key: string): boolean {
		return this.get(`heavy ${key}`)?.kind === 'heavy';
	}

	markHeavy(key: string, heavyMs = this.opts.heavyMs) {
		this.set(`heavy ${key}`, { kind: 'heavy', until: this.now() + heavyMs });
	}

	/** Stop skipping this key (a visitor asked to try again). */
	forget(key: string) {
		this.entries.delete(`heavy ${key}`);
	}

	/**
	 * Runs `run` for `url` (the full request, paging included) under `key` (the endpoint without paging), unless the
	 * key is heavy (HeavyError), an answer is cached, or the same url is already in flight. A timeout (ApiError with
	 * timedOut) marks the key heavy and is rethrown as HeavyError.
	 */
	call<T>(key: string, url: string, run: () => Promise<T>, opts: CallOptions = {}): Promise<T> {
		if (this.isHeavy(key)) return Promise.reject(new HeavyError(key));
		const cached = this.get(`value ${url}`);
		if (cached?.kind === 'value') return Promise.resolve(cached.value as T);
		const pending = this.inFlight.get(url);
		if (pending) return pending as Promise<T>;
		const p = (async () => {
			await this.acquire(opts.waitMs);
			try {
				const value = await run();
				if (opts.cacheMs) this.set(`value ${url}`, { kind: 'value', until: this.now() + opts.cacheMs, value });
				return value;
			} catch (e) {
				if (e instanceof ApiError && e.timedOut) {
					this.markHeavy(key, opts.heavyMs);
					throw new HeavyError(key);
				}
				throw e;
			} finally {
				this.release();
			}
		})().finally(() => this.inFlight.delete(url));
		this.inFlight.set(url, p);
		return p;
	}

	private get(k: string): Entry | undefined {
		const e = this.entries.get(k);
		if (!e) return undefined;
		this.entries.delete(k);
		if (e.until <= this.now()) return undefined;
		this.entries.set(k, e); // most recently used last
		return e;
	}

	private set(k: string, e: Entry) {
		this.entries.delete(k);
		this.entries.set(k, e);
		while (this.entries.size > this.opts.maxEntries) this.entries.delete(this.entries.keys().next().value!);
	}

	private acquire(waitMs?: number): Promise<void> {
		if (this.running < this.opts.maxConcurrent) {
			this.running++;
			return Promise.resolve();
		}
		return new Promise((resolve, reject) => {
			const go = () => {
				clearTimeout(timer);
				this.running++;
				resolve();
			};
			const timer = waitMs
				? setTimeout(() => {
						this.waiting = this.waiting.filter((w) => w !== go);
						reject(new BusyError());
					}, waitMs)
				: undefined;
			this.waiting.push(go);
		});
	}

	private release() {
		this.running--;
		this.waiting.shift()?.();
	}
}
