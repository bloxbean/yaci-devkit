import { tryApi } from '$lib/api/client';
import type { ScriptActivity, ScriptExecution } from './activity';

/*
 * A script's recent executions (newest first) from Yaci Store, with the redeemers' data, for the Activity panel on
 * the script page. Runs on the server, as some stores don't allow these endpoints from the browser.
 */

export async function loadScriptActivity(hash: string, fetch: typeof globalThis.fetch, count = 100): Promise<ScriptActivity | null> {
	const [executions, params] = await Promise.all([
		tryApi<ScriptExecution[]>(`/blockfrost/scripts/${hash}/redeemers`, { fetch, params: { count, order: 'desc' }, timeoutMs: 15000 }),
		tryApi<{ max_tx_ex_mem?: number; max_tx_ex_steps?: number }>('/epochs/latest/parameters', { fetch })
	]);
	if (!Array.isArray(executions)) return null;
	const wanted = [...new Set(executions.map((e) => e.redeemer_data_hash).filter((h): h is string => !!h))].slice(0, 40);
	const found = await Promise.all(wanted.map(async (h) => [h, (await tryApi<{ cbor: string }>(`/scripts/datum/${h}/cbor`, { fetch, timeoutMs: 8000 }))?.cbor] as const));
	const redeemers: Record<string, string> = {};
	for (const [h, c] of found) if (c) redeemers[h] = c;
	return {
		executions,
		redeemers,
		limits: { mem: Number(params?.max_tx_ex_mem) || 14_000_000, steps: Number(params?.max_tx_ex_steps) || 10_000_000_000 }
	};
}
