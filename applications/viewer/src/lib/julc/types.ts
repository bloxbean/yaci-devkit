/*
 * The parts of the julc WebAssembly API (julc-wasm.d.ts, API version 0) that the viewer uses. The engine itself is
 * downloaded at build time (scripts/fetch-julc.mjs), so its own typings aren't available to the type checker.
 * Long and BigInteger fields are bigint.
 */

export type PlutusVersion = 'V1' | 'V2' | 'V3';

export interface ScriptInput {
	script: string;
	language?: PlutusVersion;
	params?: DataInput[];
}

export type DataInput = { format: 'cbor' | 'json' | 'uplc' | 'auto'; value: string };

export interface ScriptInfo {
	inputFormat: string | null;
	wrapping: string | null;
	programVersion: string | null;
	language: string | null;
	languageSource: string | null;
	scriptHash: string | null;
	flatBytes: number;
	termCount: number;
	builtins: string[] | null;
	paramsApplied: number;
	warnings: string[] | null;
}

export interface DecodeResponse {
	ok: boolean;
	error: string | null;
	info: ScriptInfo | null;
	uplcText: string | null;
}

export type FeatureGroup = 'vm' | 'debug' | 'compiler' | 'uplc' | 'sourceDebug' | 'sourceDebugLocals';

export interface Features {
	api: 0;
	variant: 'full' | 'vm';
	groups: FeatureGroup[];
	defaultProtocol: number;
}

export interface Manifest {
	api: 0;
	version: string;
	launcher: string;
	variant: 'full' | 'vm';
	groups?: FeatureGroup[];
	julcVersion: string;
}

export interface JulcClient {
	version(): string;
	features(): Features;
	dispose(): void;
	vm: {
		decode(request: { script: ScriptInput }): Promise<DecodeResponse>;
		hash(request: { script: ScriptInput }): Promise<{ scriptHash: string }>;
		prettyPrint(request: { script: ScriptInput; width?: number }): Promise<{ uplcText: string }>;
	};
}

/** Errors thrown by the engine for bad requests and timeouts (a failing script is a result, not an error). */
export interface JulcError extends Error {
	code?: string;
	status?: number;
}
