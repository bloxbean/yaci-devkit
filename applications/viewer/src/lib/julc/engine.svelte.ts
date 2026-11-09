import { browser } from '$app/environment';
import { env } from '$env/dynamic/public';
import type { JulcClient, JulcError, Manifest } from './types';

/*
 * The julc engine (Script Lab), loaded on first use and shared by every page. It runs in its own worker, so a long
 * evaluation never blocks the page. Nothing is downloaded until a feature asks for the client; installs without the
 * engine (JULC_SKIP) report 'absent' and the features hide themselves.
 */

export type EngineStatus = 'idle' | 'checking' | 'absent' | 'unsupported' | 'loading' | 'ready' | 'error';

const AUTO_KEY = 'viewer:julc:auto';

/*
 * julc is a GraalVM Web Image: it needs WasmGC and the final Wasm exception handling (exnref, try_table). Both are
 * checked with tiny modules before anything is downloaded. Older browsers (e.g. Chrome before 2025) lack exnref.
 */
const WASM_GC = [0, 97, 115, 109, 1, 0, 0, 0, 1, 5, 1, 95, 1, 120, 0];
const WASM_EXNREF = [0, 97, 115, 109, 1, 0, 0, 0, 1, 4, 1, 96, 0, 0, 3, 2, 1, 0, 10, 8, 1, 6, 0, 31, 64, 0, 11, 11];

export function wasmSupported(): boolean {
	try {
		return typeof WebAssembly === 'object' && WebAssembly.validate(new Uint8Array(WASM_GC)) && WebAssembly.validate(new Uint8Array(WASM_EXNREF));
	} catch {
		return false;
	}
}

export function engineBaseUrl(): URL {
	const configured = env.PUBLIC_JULC_BASE_URL;
	const raw = (configured || '/julc/').replace(/\/?$/, '/');
	return new URL(raw, browser ? location.href : 'http://localhost/');
}

/** Workers must be same-origin; for an engine on another host, start a tiny worker that imports julc's. */
function workerFactory(url: URL): Worker {
	if (url.origin === location.origin) return new Worker(url);
	const shim = new Blob([`importScripts(${JSON.stringify(url.href)});`], { type: 'text/javascript' });
	return new Worker(URL.createObjectURL(shim));
}

class JulcEngine {
	status = $state<EngineStatus>('idle');
	error = $state<string | null>(null);
	manifest = $state<Manifest | null>(null);
	/** How long the last engine start took, in ms. */
	loadMs = $state<number | null>(null);

	#manifest: Promise<Manifest | null> | null = null;
	#client: Promise<JulcClient> | null = null;

	/** Is an engine installed with this viewer? Reads engine.json (a few hundred bytes). */
	available(): Promise<Manifest | null> {
		if (!browser) return Promise.resolve(null);
		this.#manifest ??= (async () => {
			if (this.status === 'idle') this.status = 'checking';
			try {
				const res = await fetch(new URL('engine.json', engineBaseUrl()), { cache: 'no-cache' });
				const manifest = res.ok ? ((await res.json()) as Manifest) : null;
				this.manifest = manifest;
				if (this.status === 'checking') this.status = manifest ? 'idle' : 'absent';
				return manifest;
			} catch {
				if (this.status === 'checking') this.status = 'absent';
				return null;
			}
		})();
		return this.#manifest;
	}

	/** The engine client, started on first call. A failed start can be retried. */
	client(): Promise<JulcClient> {
		this.#client ??= this.#start().catch((e) => {
			this.#client = null;
			throw e;
		});
		return this.#client;
	}

	/** Run a call against the engine. When the worker dies (a timeout disposes it), the next call starts a new one. */
	async run<T>(fn: (client: JulcClient) => Promise<T>): Promise<T> {
		const client = await this.client();
		try {
			return await fn(client);
		} catch (e) {
			const code = (e as JulcError)?.code;
			if (code === 'timeout' || /closed|worker/i.test((e as Error)?.message ?? '')) {
				this.#client = null;
				this.status = 'idle';
			}
			throw e;
		}
	}

	get auto(): boolean {
		try {
			return browser && localStorage.getItem(AUTO_KEY) === '1';
		} catch {
			return false;
		}
	}

	set auto(on: boolean) {
		try {
			if (on) localStorage.setItem(AUTO_KEY, '1');
			else localStorage.removeItem(AUTO_KEY);
		} catch {
			/* storage blocked: the choice lasts for this page only */
		}
	}

	async #start(): Promise<JulcClient> {
		if (!browser) throw new Error('The julc engine runs in the browser only.');
		const manifest = await this.available();
		if (!manifest) {
			this.status = 'absent';
			throw new Error('Script Lab is not installed with this viewer.');
		}
		if (!wasmSupported()) {
			this.status = 'unsupported';
			throw new Error('The julc engine needs WebAssembly GC and exception handling, which this browser does not support. Use a current Chrome, Edge, Firefox or Safari.');
		}
		this.status = 'loading';
		this.error = null;
		const started = performance.now();
		try {
			const baseUrl = engineBaseUrl();
			const mod = await import(/* @vite-ignore */ new URL(`julc-wasm.js?engine=${encodeURIComponent(manifest.version)}`, baseUrl).href);
			const client = (await mod.createJulc({ baseUrl, manifest, workerFactory, timeoutMs: 60_000 })) as JulcClient;
			this.loadMs = Math.round(performance.now() - started);
			this.status = 'ready';
			return client;
		} catch (e) {
			this.status = 'error';
			this.error = (e as Error)?.message || 'The julc engine failed to start.';
			throw e;
		}
	}
}

export const julc = new JulcEngine();
