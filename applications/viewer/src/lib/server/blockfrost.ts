import { ApiError, type ApiOptions } from '$lib/api/client';
import { guardEnabled, guardedApi, indexerGuard } from './heavy-guard';
import { HeavyError } from './indexer-guard';

/*
 * Yaci Store's Blockfrost-compatible address endpoints, where the store has them (not the devkit's own store, which
 * answers 404 "No endpoint"). They are preferred for addresses because the store runs their queries under a 15 s
 * statement timeout: on a busy address they fail fast with a 500 instead of leaving a query running after the viewer
 * gives up, as native /addresses/{a}/transactions does (bloxbean/yaci-store#1269). And /blockfrost/addresses/{a}
 * gives an exact balance where summing UTxO pages can only give a lower bound.
 *
 * The store is checked again 10 minutes after it answered "No endpoint".
 */

const ABSENT_MS = 10 * 60_000;
let absentUntil = 0;

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
		return await guardedApi<T>(key, `/blockfrost${path}`, opts);
	} catch (e) {
		if (e instanceof ApiError && e.status === 404 && /no endpoint/i.test(e.message)) {
			absentUntil = Date.now() + ABSENT_MS;
			throw new BlockfrostAbsent();
		}
		if (guardEnabled() && e instanceof ApiError && e.status >= 500) {
			indexerGuard.markHeavy(key, opts.heavyMs);
			throw new HeavyError(key);
		}
		throw e;
	}
}

/** For tests. */
export function resetBlockfrost() {
	absentUntil = 0;
}
