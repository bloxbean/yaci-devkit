import { ApiError, type ApiOptions } from '$lib/api/client';
import { GUARD, guardEnabled, guardedApi, indexerGuard } from './heavy-guard';
import { HeavyError } from './indexer-guard';

/*
 * Yaci Store's Blockfrost-compatible address endpoints, where the store has them (not the devkit's own store, which
 * answers 404 "No endpoint"). They are preferred for addresses because the store runs their queries under a 15 s
 * statement timeout: on a busy address they fail fast with a 500 instead of leaving a query running after the viewer
 * gives up, as native /addresses/{a}/transactions does (bloxbean/yaci-store#1269). And /blockfrost/addresses/{a}
 * gives an exact balance where summing UTxO pages can only give a lower bound.
 *
 * The store is checked again 10 minutes after it answered "No endpoint". A 500 (the store's timeout) skips the key for a
 * minute at most: the query has already stopped, so trying again is cheap, and often fast once the data is cached.
 */

const ABSENT_MS = 10 * 60_000;
let absentUntil = 0;
let seen = false;

/** The store answered a Blockfrost-compatible call (any answer but "No endpoint") and hasn't said otherwise since. */
export const blockfrostPresent = () => seen && Date.now() >= absentUntil;

/** The store has no Blockfrost-compatible API: use the native endpoint. */
export class BlockfrostAbsent extends Error {
	constructor() {
		super('Yaci Store has no Blockfrost-compatible API');
	}
}

/**
 * GET /blockfrost{path} through the guard under `key`. Throws BlockfrostAbsent when the store doesn't have it; a 500
 * (the store's own timeout) marks the key heavy, like a timeout, and is thrown as HeavyError.
 */
export async function bfApi<T>(key: string, path: string, opts: ApiOptions & { cacheMs?: number; heavyMs?: number } = {}): Promise<T> {
	if (Date.now() < absentUntil) throw new BlockfrostAbsent();
	try {
		const value = await guardedApi<T>(key, `/blockfrost${path}`, opts);
		seen = true;
		return value;
	} catch (e) {
		if (e instanceof ApiError && e.status === 404 && /no endpoint/i.test(e.message)) {
			absentUntil = Date.now() + ABSENT_MS;
			seen = false;
			throw new BlockfrostAbsent();
		}
		if (e instanceof ApiError) seen = true;
		if (guardEnabled() && e instanceof ApiError && e.status >= 500) {
			indexerGuard.markHeavy(key, Math.min(opts.heavyMs ?? GUARD.heavyMs, GUARD.blockfrostHeavyMs));
			throw new HeavyError(key);
		}
		throw e;
	}
}

/** For tests. */
export function resetBlockfrost() {
	absentUntil = 0;
	seen = false;
}
