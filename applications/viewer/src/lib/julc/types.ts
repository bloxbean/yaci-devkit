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
	proposals: MockProposal[];
}
/** A governance proposal; julc builds its action roughly (governance/action.ts encodes the real one). */
export interface MockProposal {
	deposit: string;
	/** "key:<hash>" or "script:<hash>". */
	returnCredential: string;
	actionType: string;
	/** The guardrail (policy) script hash. */
	guardrail: string | null;
	withdrawals: { credential: string; amount: string }[] | null;
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

/** The whole run, known after the first session action. Steps are 0-based machine transitions. */
export interface Timeline {
	totalSteps: bigint;
	traceSteps: bigint[] | null;
	errorStep: bigint | null;
	status: string | null;
	truncated: boolean;
	cpu: bigint;
	mem: bigint;
}

export interface Snapshot {
	step: bigint;
	/** 'compute' (about to evaluate a term) or 'return' (returning a value to a frame). */
	phase: string | null;
	/** Position in the formatted UPLC (decode / prettyPrint text), 1-based lines and columns. */
	span: Span | null;
	termKind: string | null;
	value: string | null;
	environment: { name: string | null; value: string | null }[] | null;
	frames: { kind: string | null; detail: string | null; span: Span | null }[] | null;
	stackDepth: number;
	cpu: bigint;
	mem: bigint;
	cpuDelta: bigint;
	memDelta: bigint;
	traces: string[] | null;
	finished: boolean;
	status: string | null;
	error: string | null;
	stopReason: string | null;
}

export interface Breakpoints {
	lines?: number[];
	builtins?: string[];
	onTrace?: boolean;
	onError?: boolean;
}

export interface DebugReply {
	ok: boolean;
	error: string | null;
	timeline: Timeline | null;
	snapshot: Snapshot | null;
}

/** A debug session in the engine's worker. Going back replays from step 0, which is fast (milliseconds). */
export interface DebugSession {
	sessionId: string;
	isOpen(): boolean;
	step(): Promise<DebugReply>;
	goto(step: bigint): Promise<DebugReply>;
	continue(breakpoints?: Breakpoints): Promise<DebugReply>;
	over(breakpoints?: Breakpoints): Promise<DebugReply>;
	out(breakpoints?: Breakpoints): Promise<DebugReply>;
	snapshot(): Promise<DebugReply>;
	close(): Promise<{ closed: boolean }>;
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
		/** Same request as evaluate; the script isn't run until the first action. */
		debug(request: Omit<TransactionRequest, 'transaction'> & { args: DataInput[] }): Promise<DebugSession & { ok?: boolean; error?: string | null }>;
	};
	/** Low-level session protocol; the `timeline` action runs the whole script once and returns its timeline. */
	debug?: {
		act(request: { sessionId: string; action: 'timeline'; step?: bigint }): Promise<DebugReply>;
	};
	/** Full engine only. */
	uplc?: {
		evaluateTransaction(request: TransactionRequest): Promise<Evaluation>;
		/** julc's sample transaction for a purpose (one key input and output). */
		defaultTransaction?(request: { purpose: MockPurpose['type'] }): Promise<MockTransaction>;
	};
}

/** Errors thrown by the engine for bad requests and timeouts (a failing script is a result, not an error). */
export interface JulcError extends Error {
	code?: string;
	status?: number;
}
