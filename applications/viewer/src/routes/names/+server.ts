import { json, type RequestHandler } from '@sveltejs/kit';
import { KEY } from '$lib/names/keys';
import type { NameKind, NamesQuery } from '$lib/names/types';
import { names } from '$lib/server/names';

/*
 * Names for the ids a page shows: POST { scripts?, dreps?, pools?, actions?, tokens? } (lists of ids, any of the
 * forms keys.ts accepts) → the names known for them, keyed by their normalised key. At most MAX ids per kind.
 * Empty everywhere but Mainnet.
 */

const MAX = 300;
const KINDS: NameKind[] = ['scripts', 'dreps', 'pools', 'actions', 'tokens'];

export const POST: RequestHandler = async ({ request }) => {
	let body: NamesQuery;
	try {
		body = await request.json();
	} catch {
		return json({ error: 'Send a JSON body.' }, { status: 400 });
	}
	const q = Object.fromEntries(
		KINDS.map((kind) => {
			const ids = Array.isArray(body?.[kind]) ? body[kind]!.slice(0, MAX) : [];
			const keys = new Set(ids.map((id) => (typeof id === 'string' ? KEY[kind](id) : null)).filter((k): k is string => !!k));
			return [kind, [...keys]];
		})
	) as Required<NamesQuery>;
	return json(await names.lookup(q), { headers: { 'cache-control': 'no-store' } });
};
