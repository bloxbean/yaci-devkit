import type { ForkEdits } from './fork';
import type { RunResult } from './run';

/*
 * A script run as a test fixture: everything needed to run it again elsewhere (any Plutus VM, a unit test, a
 * benchmark), in one JSON file. Data is CBOR hex; integers that can exceed 2^53 are strings.
 */
export function fixtureOf(txHash: string, result: RunResult, fork: { edits: ForkEdits; epoch: number | null } | null = null) {
	const { plan, evaluation, args } = result;
	const req = plan.request!;
	return {
		format: 'yaci-viewer/script-run@1',
		source: { tx: txHash, purpose: plan.purpose, index: plan.index, ...(fork ? { fork } : {}) },
		script: { hash: plan.scriptHash, language: plan.language, cbor: req.script.script, ...(req.script.params?.length ? { params: req.script.params.map((p) => p.value) } : {}) },
		target: req.target,
		costModel: req.costModel?.parameters.map(String) ?? null,
		limits: { cpu: req.maxCpu?.toString() ?? null, mem: req.maxMem?.toString() ?? null },
		/** What the script is applied to, in order: V3 the ScriptContext; V1/V2 (datum,) redeemer, ScriptContext. */
		args: (args ?? []).map((a) => a.value),
		scriptContext: evaluation?.scriptContextCbor ?? null,
		expected: evaluation ? { status: evaluation.status, accepted: evaluation.accepted, cpu: evaluation.cpu.toString(), mem: evaluation.mem.toString(), traces: evaluation.traces ?? [] } : null,
		declared: plan.declared ? { cpu: plan.declared.steps.toString(), mem: plan.declared.mem.toString() } : null,
		notes: plan.notes,
		/** cbor: replayed from the transaction's stored CBOR; store: rebuilt from Yaci Store's JSON. */
		transactionSource: plan.source ?? 'store'
	};
}

export function downloadJson(name: string, value: unknown) {
	const url = URL.createObjectURL(new Blob([JSON.stringify(value, (_, v) => (typeof v === 'bigint' ? v.toString() : v), 2)], { type: 'application/json' }));
	const a = Object.assign(document.createElement('a'), { href: url, download: name });
	a.click();
	setTimeout(() => URL.revokeObjectURL(url), 1000);
}
