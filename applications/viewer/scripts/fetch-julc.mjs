#!/usr/bin/env node
/*
 * Installs the julc WebAssembly engine (Script Lab) into static/julc/ from the julc GitHub release pinned in
 * julc.lock.json. julc is never built here: the release archive is downloaded, checked against the pinned sha256
 * and unpacked (plain files only).
 *
 *   node scripts/fetch-julc.mjs              required: fails the build if the engine can't be installed
 *   node scripts/fetch-julc.mjs --optional   dev: warns and carries on; Script Lab hides itself
 *
 * JULC_SKIP=1           skip entirely (Script Lab hidden)
 * JULC_ARCHIVE=<path>   use a local copy of the release archive (offline builds); it is still checked
 */
import { createHash } from 'node:crypto';
import { existsSync, mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { gunzipSync } from 'node:zlib';
import { untar } from './lib/untar.mjs';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const optional = process.argv.includes('--optional');
const lock = JSON.parse(readFileSync(join(root, 'julc.lock.json'), 'utf8'));
const target = join(root, 'static', 'julc');
const marker = join(root, 'node_modules', '.cache', 'julc', 'installed');
const cacheFile = join(root, 'node_modules', '.cache', 'julc', lock.file);
const url = `https://github.com/${lock.repo}/releases/download/${lock.tag}/${lock.file}`;

const log = (msg) => console.log(`[julc] ${msg}`);
const sha256 = (buf) => createHash('sha256').update(buf).digest('hex');

async function archive() {
	if (process.env.JULC_ARCHIVE) return readFileSync(process.env.JULC_ARCHIVE);
	if (existsSync(cacheFile)) {
		const cached = readFileSync(cacheFile);
		if (sha256(cached) === lock.sha256) return cached;
	}
	log(`downloading ${url}`);
	const res = await fetch(url, { signal: AbortSignal.timeout(120_000) });
	if (!res.ok) throw new Error(`download failed: HTTP ${res.status}`);
	const buf = Buffer.from(await res.arrayBuffer());
	mkdirSync(dirname(cacheFile), { recursive: true });
	writeFileSync(cacheFile, buf);
	return buf;
}

async function install() {
	if (existsSync(marker) && existsSync(join(target, 'engine.json')) && readFileSync(marker, 'utf8').trim() === lock.sha256) {
		log(`engine ${lock.version} already installed`);
		return;
	}
	const buf = await archive();
	const actual = sha256(buf);
	if (actual !== lock.sha256) throw new Error(`checksum mismatch for ${lock.file}: expected ${lock.sha256}, got ${actual}`);

	const files = untar(gunzipSync(buf));
	const engine = files.get('engine.json');
	if (!engine) throw new Error('archive has no engine.json');
	const manifest = JSON.parse(engine.toString('utf8'));
	if (manifest.julcVersion !== lock.version) throw new Error(`archive is julc ${manifest.julcVersion}, lock says ${lock.version}`);
	if (!files.has(manifest.launcher) || !files.has(`${manifest.launcher}.wasm`)) throw new Error('archive is missing the engine launcher');

	rmSync(target, { recursive: true, force: true });
	mkdirSync(target, { recursive: true });
	// Type declarations aren't needed at runtime (the viewer has its own, src/lib/julc/types.ts).
	for (const [name, body] of files) if (!name.endsWith('.d.ts')) writeFileSync(join(target, name), body);
	mkdirSync(dirname(marker), { recursive: true });
	writeFileSync(marker, lock.sha256 + '\n');
	log(`installed engine ${lock.version} (${manifest.variant}, ${manifest.version}) into static/julc`);
}

if (process.env.JULC_SKIP === '1') {
	log('JULC_SKIP=1: skipping; Script Lab will be hidden');
} else {
	try {
		await install();
	} catch (e) {
		const msg = e instanceof Error ? e.message : String(e);
		if (!optional) {
			console.error(`[julc] ${msg}\n[julc] Set JULC_ARCHIVE to a local copy of ${lock.file}, or JULC_SKIP=1 to build without Script Lab.`);
			process.exit(1);
		}
		log(`${msg}; continuing without Script Lab`);
	}
}
