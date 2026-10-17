/*
 * Which anchor URLs the viewer's server may fetch. Anchors are user-controlled (anyone can put a URL on chain, and
 * anyone can type one into /governance/anchor?url=), so the server must never be turned into a proxy for the local
 * network: only http(s) on standard ports and ipfs://, and never private, loopback, link-local or other special
 * addresses. Hostnames are checked again after DNS resolution (see $lib/server/safe-fetch.ts).
 */

import { checkPublicUrl } from '$lib/net/public-url';

export { isBlockedIp, ipKind } from '$lib/net/public-url';

/** Public gateways, tried in order (they rate-limit). A gateway set with IPFS_GATEWAY goes first. */
export const IPFS_GATEWAYS = ['https://ipfs.io/ipfs/', 'https://dweb.link/ipfs/', 'https://gateway.pinata.cloud/ipfs/', 'https://ipfs.blockfrost.dev/ipfs/'];

export type AnchorCheck = { ok: true; url: URL; display: string; alternates?: URL[] } | { ok: false; reason: string };

export function checkAnchorUrl(raw: string | null | undefined, gateways: string[] = IPFS_GATEWAYS): AnchorCheck {
	const input = (raw ?? '').trim();
	if (!input) return { ok: false, reason: 'No anchor URL given.' };
	if (input.length > 2048) return { ok: false, reason: 'That URL is too long.' };

	if (/^ipfs:\/\//i.test(input)) {
		// ipfs://<cid>/<path> → public gateways, tried in order.
		const rest = input.slice('ipfs://'.length).replace(/^ipfs\//, '');
		if (!/^[a-zA-Z0-9]{20,}(\/[^?#\s]*)?$/.test(rest)) return { ok: false, reason: 'That IPFS link is not valid.' };
		const [first, ...others] = gateways.map((g) => new URL(g.replace(/\/?$/, '/') + rest));
		return { ok: true, url: first, display: input, alternates: others };
	}
	const checked = checkPublicUrl(input);
	if (!checked.ok) return checked.reason === 'Only http and https URLs can be opened.' ? { ok: false, reason: 'Only http, https and ipfs anchors can be opened.' } : checked;
	return { ok: true, url: checked.url, display: input };
}
