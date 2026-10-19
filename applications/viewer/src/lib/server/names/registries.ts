import type { ScriptName } from '$lib/names/types';
import { cleanText } from './clean';
import type { TarFile } from './tar';

/*
 * dApp labels for script hashes, from two curated registries on GitHub:
 *  - Strica's Cardano Contracts Registry (the labels Cardanoscan shows): projects/<name>.json, one entry per contract.
 *  - Cardano Fans' CRFA off-chain data registry: dApps/<name>.json, scripts with versions (spending scripts and
 *    minting policies).
 * Both are read from their repository archive (one download each). Strica's label wins when both list a hash.
 */

export const REGISTRIES = [
	{ source: 'strica', archive: 'https://codeload.github.com/StricaHQ/cardano-contracts-registry/tar.gz/refs/heads/master', dir: '/projects/' },
	{ source: 'crfa', archive: 'https://codeload.github.com/Cardano-Fans/crfa-offchain-data-registry/tar.gz/refs/heads/main', dir: '/dApps/' }
] as const;

export type RegistrySource = (typeof REGISTRIES)[number]['source'];

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

const HASH = /^[0-9a-f]{56}$/;

/**
 * The script hash in a CRFA "scriptHash", which is sometimes the whole address body: payment script hash + stake
 * credential (112 hex), with or without the address header byte (58 / 114).
 */
export function crfaScriptHash(v: unknown): string | null {
	const h = String(v ?? '').toLowerCase();
	if (!/^[0-9a-f]+$/.test(h)) return null;
	const body = h.length === 58 || h.length === 114 ? h.slice(2) : h;
	return body.length === 56 || body.length === 112 ? body.slice(0, 56) : null;
}
const str = cleanText;
const web = (v: unknown): string | undefined => {
	const s = str(v, 200);
	return s && /^https:\/\//i.test(s) ? s : undefined;
};

function json(file: TarFile): Any | null {
	try {
		return JSON.parse(new TextDecoder().decode(file.data));
	} catch {
		return null;
	}
}

/** Strica: label = labelPrefix + contract name, with the version when it is past 1. */
export function stricaNames(files: TarFile[]): Map<string, ScriptName> {
	const out = new Map<string, ScriptName>();
	for (const f of files) {
		if (!f.path.includes('/projects/') || !f.path.endsWith('.json')) continue;
		const p = json(f);
		const project = str(p?.projectName, 40);
		if (!project || !Array.isArray(p.contracts)) continue;
		const prefix = str(p.labelPrefix, 24) ?? project;
		for (const c of p.contracts) {
			const hash = String(c?.scriptHash ?? '').toLowerCase();
			if (!HASH.test(hash)) continue;
			const name = str(c.name, 60);
			const version = Number(c.version);
			const label = [prefix, name && name.toLowerCase() !== 'contract' ? name : null].filter(Boolean).join(' ');
			out.set(hash, {
				label: version > 1 ? `${label} v${version}` : label,
				project,
				category: str(p.category, 20),
				url: web(p.website),
				source: 'strica'
			});
		}
	}
	return out;
}

/** CRFA: label = project + script name (+ version), for script hashes and minting policy ids. */
export function crfaNames(files: TarFile[]): Map<string, ScriptName> {
	const out = new Map<string, ScriptName>();
	for (const f of files) {
		if (!f.path.includes('/dApps/') || !f.path.endsWith('.json')) continue;
		const p = json(f);
		const project = str(p?.projectName, 40);
		if (!project || !Array.isArray(p.scripts)) continue;
		for (const s of p.scripts) {
			const name = str(s?.name, 60);
			const versions = Array.isArray(s?.versions) ? s.versions : [];
			const many = versions.length > 1;
			for (const v of versions) {
				const label = [project, name, many && v?.version != null ? `v${v.version}` : null].filter(Boolean).join(' ');
				for (const hash of [crfaScriptHash(v?.scriptHash), crfaScriptHash(v?.mintPolicyID)]) {
					if (!hash || out.has(hash)) continue;
					out.set(hash, { label, project, category: str(p.subCategory ?? p.category, 20), url: web(p.link), source: 'crfa' });
				}
			}
		}
	}
	return out;
}

/** One map from both registries; Strica's entries replace CRFA's for the same hash. */
export function mergeScriptNames(strica: Map<string, ScriptName>, crfa: Map<string, ScriptName>): Record<string, ScriptName> {
	return Object.fromEntries([...crfa, ...strica]);
}
