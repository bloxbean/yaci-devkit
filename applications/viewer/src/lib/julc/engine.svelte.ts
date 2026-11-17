import { browser } from '$app/environment';
import type { JulcClient, JulcError, Manifest } from './types';

/*
 * The julc engine (Script Lab), loaded on first use and shared by every page. It comes from the @julclang/julc-wasm
 * package, which the build bundles: its worker, launcher and WebAssembly module are emitted as assets and served by
 * this viewer (the engine only loads them from the page's own origin). It runs in its own worker, so a long evaluation
 * never blocks the page. Nothing is downloaded until a feature asks for the client. The package is imported in the
 * browser only, never during server rendering.
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

class JulcEngine {
	status = $state<EngineStatus>('idle');
	error = $state<string | null>(null);
	manifest = $state<Manifest | null>(null);
	/** How long the last engine start took, in ms. */
	loadMs = $state<number | null>(null);

	#manifest: Promise<Manifest | null> | null = null;
	#client: Promise<JulcClient> | null = null;

	/**
	 * The engine this viewer was built with: its manifest, from the package's small `engine` module (the engine itself
	 * is not downloaded). Null during server rendering, or when that module can't be loaded (e.g. the network failed).
	 */
	available(): Promise<Manifest | null> {
		if (!browser) return Promise.resolve(null);
		this.#manifest ??= (async () => {
			if (this.status === 'idle') this.status = 'checking';
			try {
				const { engineFiles } = await import('@julclang/julc-wasm/engine');
				const manifest: Manifest = engineFiles().manifest;
				this.manifest = manifest;
				if (this.status === 'checking') this.status = 'idle';
				return manifest;
			} catch {
				if (this.status === 'checking') this.status = 'absent';
				this.#manifest = null;
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
			const { createJulc } = await import('@julclang/julc-wasm');
			// The package names its own engine files and checks they are of its release; rest is off in package mode.
			const client: JulcClient = await createJulc({ timeoutMs: 60_000 });
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
