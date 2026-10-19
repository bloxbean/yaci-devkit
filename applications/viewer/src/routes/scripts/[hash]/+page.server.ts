import { error } from '@sveltejs/kit';
import { tryApi } from '$lib/api/client';
import { must } from '$lib/api/load';
import { isPlutus } from '$lib/assets/script';
import { loadScriptActivity } from '$lib/julc/activity.server';
import { scriptInsights } from '$lib/server/insights';
import type { PageServerLoad } from './$types';

/* A script by hash: its type and size, the JSON for native scripts, and the CBOR. */
export const load: PageServerLoad = async ({ params, fetch }) => {
	const hash = params.hash.trim().toLowerCase();
	if (!/^[0-9a-f]{56}$/.test(hash)) error(404, 'That is not a script hash (56 hex characters).');

	const info = await must<{ script_hash: string; type?: string; serialised_size?: number | null }>(
		`/scripts/${hash}`,
		{ fetch },
		'No script with that hash on this chain. Scripts are stored once a transaction uses or publishes them.'
	);
	const [json, cbor] = await Promise.all([
		tryApi<{ json: unknown }>(`/scripts/${hash}/json`, { fetch }),
		tryApi<{ cbor: string | null }>(`/scripts/${hash}/cbor`, { fetch })
	]);

	return {
		hash,
		type: info.type ?? null,
		size: info.serialised_size ?? null,
		json: json?.json ?? null,
		cbor: cbor?.cbor ?? null,
		// Streamed: the page renders without waiting for the execution history.
		activity: isPlutus(info.type) ? loadScriptActivity(hash, fetch) : null,
		// Runs and ex-units over 90 days, from Yaci Store analytics where the store has it (null otherwise). Streamed.
		usage: isPlutus(info.type) ? scriptInsights(hash, fetch).catch(() => null) : null
	};
};
