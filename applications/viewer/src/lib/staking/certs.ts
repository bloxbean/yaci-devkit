import { api, paging } from '$lib/api/client';

/* The five certificate lists share one loader and one page; they differ in endpoint and columns. */

export type CertKind = 'stakekey-registrations' | 'stakekey-deregistrations' | 'delegations' | 'pool-registrations' | 'pool-retirements';

export const CERTS: Record<CertKind, { label: string; title: string; text: string; endpoint: string }> = {
	'stakekey-registrations': {
		label: 'Stake registrations',
		title: 'Stake key registrations',
		text: 'Stake addresses registered on chain, with the deposit paid.',
		endpoint: '/stake/registrations'
	},
	'stakekey-deregistrations': {
		label: 'Stake deregistrations',
		title: 'Stake key deregistrations',
		text: 'Stake addresses removed from the chain; the deposit goes back to the owner.',
		endpoint: '/stake/deregistrations'
	},
	delegations: {
		label: 'Delegations',
		title: 'Delegations',
		text: 'Stake addresses delegating to a stake pool.',
		endpoint: '/stake/delegations'
	},
	'pool-registrations': {
		label: 'Pool registrations',
		title: 'Pool registrations and updates',
		text: 'New stake pools and changes to existing ones: pledge, cost, margin and owners.',
		endpoint: '/pools/registrations'
	},
	'pool-retirements': {
		label: 'Pool retirements',
		title: 'Pool retirements',
		text: 'Stake pools announcing the epoch they stop operating.',
		endpoint: '/pools/retirements'
	}
};

export const CERT_ORDER: CertKind[] = ['stakekey-registrations', 'stakekey-deregistrations', 'delegations', 'pool-registrations', 'pool-retirements'];

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

/** Yaci Store pages these lists 1-based (page 0 is treated as 1). */
export async function loadCerts(kind: CertKind, url: URL, f: typeof fetch) {
	const { page, count } = paging(url, 20);
	try {
		const rows = await api<Any[]>(CERTS[kind].endpoint, { fetch: f, params: { page, count }, timeoutMs: 25000 });
		return { kind, rows: (rows ?? []) as Any[], page, count, error: null as string | null };
	} catch (e) {
		return { kind, rows: [] as Any[], page, count, error: (e as Error).message };
	}
}
