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

/** A transaction described as JSON; julc builds the ScriptContext from it (MockTransaction in julc-models.d.ts). */
export interface MockTxIn {
	txId: string;
	index: bigint;
	address: { payment: string; stake?: string };
	value: MockValue;
	datum: MockDatum;
	referenceScript?: string | null;
}
export interface MockTxOut {
	address: { payment: string; stake?: string };
	value: MockValue;
	datum: MockDatum;
	referenceScript?: string | null;
}
export interface MockValue {
	lovelace: string;
	assets: { policyId: string; tokenName: string; quantity: string }[];
}
export type MockDatum = { kind: 'none' } | { kind: 'inline'; data: DataInput } | { kind: 'hash'; hash: string; data?: DataInput };
export interface MockCertificate {
	type: 'regStaking' | 'unregStaking' | 'delegStaking' | 'regDRep' | 'unregDRep' | 'updateDRep';
	credential: string;
	deposit?: string | null;
	poolId?: string | null;
}
export interface MockVote {
	voterType: 'drep' | 'committee' | 'pool';
	voter: string;
	actionTxId: string;
	actionIndex: bigint;
	vote: 'yes' | 'no' | 'abstain';
}
export interface MockPurpose {
	type: 'spend' | 'mint' | 'reward' | 'certify' | 'vote' | 'propose';
	index?: number;
	target?: string;
	voterType?: 'drep' | 'committee' | 'pool';
}
export interface MockTransaction {
	purpose: MockPurpose;
	redeemer: DataInput;
	inputs: MockTxIn[];
	referenceInputs: MockTxIn[];
	outputs: MockTxOut[];
	fee: string;
	mint: { policyId: string; tokenName: string; quantity: string }[];
	certificates: MockCertificate[];
	withdrawals: { credential: string; amount: string }[];
	validRange: { from: bigint | null; fromInclusive: boolean; to: bigint | null; toInclusive: boolean };
	signatories: string[];
	datums: DataInput[];
	txId: string;
	votes: MockVote[];
	proposals: [];
}

export interface Target {
	language: 'PlutusV1' | 'PlutusV2' | 'PlutusV3';
	protocol: number;
}

export interface TransactionRequest {
	script: ScriptInput;
	transaction: MockTransaction;
	target: Target;
	costModel?: { target: Target; parameters: bigint[] } | null;
	maxCpu?: bigint;
	maxMem?: bigint;
}

export interface Span {
	startLine: number;
	startColumn: number;
	endLine: number;
	endColumn: number;
}

export interface Evaluation {
	ok: boolean;
	error: string | null;
	status: 'success' | 'failure' | 'budgetExhausted' | string | null;
	accepted: boolean;
	message: string | null;
	cpu: bigint;
	mem: bigint;
	traces: string[] | null;
	failedSpan: Span | null;
	result: string | null;
	lastBuiltins: string[] | null;
	scriptContext: string | null;
	scriptContextCbor: string | null;
	scriptHash: string | null;
	language: string | null;
	costModelId?: string;
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
		/** Apply the script to raw arguments (V3: the context; V1/V2: datum, redeemer, context). */
		evaluate(request: Omit<TransactionRequest, 'transaction'> & { args: DataInput[] }): Promise<Evaluation>;
	};
	/** Full engine only. */
	uplc?: {
		evaluateTransaction(request: TransactionRequest): Promise<Evaluation>;
	};
}

/** Errors thrown by the engine for bad requests and timeouts (a failing script is a result, not an error). */
export interface JulcError extends Error {
	code?: string;
	status?: number;
}
