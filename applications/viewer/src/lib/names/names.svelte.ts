import { browser } from '$app/environment';
import { SvelteMap } from 'svelte/reactivity';
import { KEY, scriptKeyOfAddress } from './keys';
import type { NameKind, NamesResult, PoolName, ScriptName, TokenName } from './types';

/*
 * Names in the browser. Components ask for the name of an id while they render; the ids asked for within a few
 * milliseconds go to /names in one request, and the answers fill reactive maps, so the names appear when they
 * arrive. An id without a name is asked again after a few minutes (the server may still be loading). When the
 * server answers that names are off (not Mainnet), nothing more is asked for in this session.
 */

type Value = { scripts: ScriptName; dreps: string; pools: PoolName; actions: string; tokens: TokenName };

const KINDS: NameKind[] = ['scripts', 'dreps', 'pools', 'actions', 'tokens'];
const RETRY_MS = 5 * 60_000;
const BATCH = 300;

const known = {
	scripts: new SvelteMap<string, ScriptName>(),
	dreps: new SvelteMap<string, string>(),
	pools: new SvelteMap<string, PoolName>(),
	actions: new SvelteMap<string, string>(),
	tokens: new SvelteMap<string, TokenName>()
};
const missedAt = new Map<string, number>();
const pending = new Set<string>();
const queue = new Map<NameKind, Set<string>>(KINDS.map((k) => [k, new Set()]));
let timer: ReturnType<typeof setTimeout> | null = null;
let off = false;

function want(kind: NameKind, key: string) {
	if (!browser || off) return;
	const id = `${kind}:${key}`;
	if (known[kind].has(key) || pending.has(id) || Date.now() - (missedAt.get(id) ?? -Infinity) < RETRY_MS) return;
	pending.add(id);
	queue.get(kind)!.add(key);
	timer ??= setTimeout(flush, 30);
}

async function flush() {
	timer = null;
	const asked = new Map(KINDS.map((k) => [k, [...queue.get(k)!].slice(0, BATCH)]));
	for (const [k, keys] of asked) for (const key of keys) queue.get(k)!.delete(key);
	if (KINDS.some((k) => queue.get(k)!.size)) timer = setTimeout(flush, 30);
	let res: (NamesResult & { enabled?: boolean | null }) | null = null;
	try {
		const r = await fetch('/names', { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify(Object.fromEntries(asked)) });
		if (r.ok) res = await r.json();
	} catch {
		/* the viewer's server is unreachable: the ids are asked again later */
	}
	if (res?.enabled === false) off = true;
	const now = Date.now();
	for (const [kind, keys] of asked) {
		for (const key of keys) {
			const id = `${kind}:${key}`;
			pending.delete(id);
			const v = res?.[kind]?.[key];
			if (v) (known[kind] as SvelteMap<string, unknown>).set(key, v);
			else missedAt.set(id, now);
		}
	}
}

function lookup<K extends NameKind>(kind: K, id: string | null | undefined): Value[K] | null {
	const key = id ? (KEY[kind] as (s: string) => string | null)(id) : null;
	if (!key) return null;
	const v = known[kind].get(key) as Value[K] | undefined;
	if (v === undefined) want(kind, key);
	return v ?? null;
}

/** The dApp label for a script hash, or for the script behind an address or a stake address. */
export const scriptName = (hashOrAddress: string | null | undefined): ScriptName | null =>
	lookup('scripts', /^(addr|stake)/.test(hashOrAddress ?? '') ? scriptKeyOfAddress(hashOrAddress) : hashOrAddress);
export const drepName = (id: string | null | undefined) => lookup('dreps', id);
export const poolName = (id: string | null | undefined) => lookup('pools', id);
/** A governance action's title: by "<tx>#<index>", "<tx>_<index>" or gov_action1…. */
export const actionTitle = (id: string | null | undefined) => lookup('actions', id);
export const tokenName = (unit: string | null | undefined) => lookup('tokens', unit);
