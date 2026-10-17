import { tryApi } from '$lib/api/client';
import type { Genesis } from '$lib/julc/replay/slots';
import { utxoOf } from '$lib/julc/authoring/utxo';
import type { PageServerLoad } from './$types';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

/*
 * The validator lab. ?script=<hash> starts from a script on this chain, ?utxo=<tx>#<index> (or _) from a UTxO.
 * Parameters, genesis (slot times) and the tip slot come along for building the transaction.
 */
export const load: PageServerLoad = async ({ fetch, url }) => {
	const hash = url.searchParams.get('script')?.toLowerCase() ?? null;
	const ref = url.searchParams.get('utxo')?.match(/^([0-9a-f]{64})[#_](\d+)$/i);
	const [params, genesis, tip, scriptInfo, scriptBody, utxo] = await Promise.all([
		tryApi<Any>('/epochs/latest/parameters', { fetch }),
		tryApi<Genesis>('/genesis', { fetch }),
		tryApi<{ slot?: number }>('/blocks/latest', { fetch }),
		hash && /^[0-9a-f]{56}$/.test(hash) ? tryApi<{ type?: string }>(`/scripts/${hash}`, { fetch }) : null,
		hash && /^[0-9a-f]{56}$/.test(hash) ? tryApi<{ cbor?: string }>(`/scripts/${hash}/cbor`, { fetch }) : null,
		ref ? tryApi<Any>(`/utxos/${ref[1].toLowerCase()}/${ref[2]}`, { fetch }) : null
	]);
	return {
		params,
		genesis,
		tipSlot: tip?.slot ?? null,
		script: hash && scriptBody?.cbor ? { hash, type: scriptInfo?.type ?? null, cbor: scriptBody.cbor } : null,
		scriptMissing: !!hash && !scriptBody?.cbor,
		utxo: utxoOf(utxo)
	};
};
