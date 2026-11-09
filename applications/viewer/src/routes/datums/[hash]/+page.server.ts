import { error } from '@sveltejs/kit';
import { tryApi } from '$lib/api/client';
import type { PageServerLoad } from './$types';

/* A datum by hash: the store keeps its JSON and CBOR once a transaction includes it in its witnesses. */
export const load: PageServerLoad = async ({ params, fetch }) => {
	const hash = params.hash.trim().toLowerCase();
	if (!/^[0-9a-f]{64}$/.test(hash)) error(404, 'That is not a datum hash (64 hex characters).');

	const [json, cbor] = await Promise.all([
		tryApi<{ json_value?: unknown }>(`/scripts/datum/${hash}`, { fetch }),
		tryApi<{ cbor?: string }>(`/scripts/datum/${hash}/cbor`, { fetch })
	]);
	if (!json && !cbor) error(404, 'No datum with that hash on this chain. Only datums included in a transaction are stored.');

	return { hash, json: json?.json_value ?? null, cbor: cbor?.cbor ?? null };
};
