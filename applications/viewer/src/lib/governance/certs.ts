import { error } from '@sveltejs/kit';
import { api, paging } from '$lib/api/client';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

export type CertKind = 'registrations' | 'updates' | 'deregistrations';

export const CERT_KINDS: { id: CertKind; label: string; href: string }[] = [
	{ id: 'registrations', label: 'Registrations', href: '/governance/drep-registrations' },
	{ id: 'updates', label: 'Updates', href: '/governance/drep-updates' },
	{ id: 'deregistrations', label: 'Retirements', href: '/governance/drep-deregistrations' }
];

/* DRep certificates of one kind (/governance/dreps/{kind}, 1-based). */
export async function loadCerts(kind: CertKind, url: URL, f: typeof fetch) {
	const { page, count } = paging(url, 20);
	try {
		const rows = await api<Any[]>(`/governance/dreps/${kind}`, { fetch: f, params: { page, count, order: 'desc' } });
		return { kind, rows: rows ?? [], page, count };
	} catch (e) {
		error(503, (e as Error).message);
	}
}
