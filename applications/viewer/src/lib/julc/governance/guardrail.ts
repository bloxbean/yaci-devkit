import { encodePlutusData } from '$lib/cbor';
import { julc } from '../engine.svelte';
import type { RunPlan } from '../replay/build';
import { fixContext } from '../replay/fix-context';
import { vmRequest, type RunResult } from '../replay/run';
import type { MockTransaction, Target } from '../types';
import { PARAMS, ParamError, encodeParamChange, encodeProcedure, encodeWithdrawals, paramByName, type ParamChange } from './action';

/*
 * The guardrail check: run the constitution's guardrail script against a proposal that isn't on chain, as the
 * ledger would when the proposal is submitted. julc builds a proposing ScriptContext around a placeholder
 * transaction, the viewer puts the real governance action into it (fix-context.ts) and the VM runs the script.
 * Only parameter changes and treasury withdrawals carry a guardrail ("policy") in Conway.
 */

export type GuardrailAction = { type: 'PARAMETER_CHANGE_ACTION'; change: ParamChange } | { type: 'TREASURY_WITHDRAWALS_ACTION'; withdrawals: { address: string; amount: string }[] };

/** An /epochs/{n}/parameters response: cost models, protocol, limits, and current values for composite parameters. */
export type EpochParams = Record<string, unknown> & {
	cost_models_raw?: Record<string, (number | string)[]> | null;
	protocol_major_ver?: number | null;
	gov_action_deposit?: number | string | null;
	max_tx_ex_mem?: number | string | null;
	max_tx_ex_steps?: number | string | null;
};

export interface GuardrailScript {
	hash: string;
	cbor: string;
}

const RETURN_ADDRESS = 'e0' + '00'.repeat(28);

/** The action's ProposalProcedure as CBOR; throws ParamError for an invalid value. */
export function procedureOf(action: GuardrailAction, params: EpochParams | null, guardrail: string): string {
	const policy = { constructor: 0, fields: [{ bytes: guardrail }] };
	const data =
		action.type === 'PARAMETER_CHANGE_ACTION'
			? { constructor: 0, fields: [{ constructor: 1, fields: [] }, encodeParamChange(action.change, params), policy] }
			: { constructor: 2, fields: [encodeWithdrawals(action.withdrawals), policy] };
	return encodePlutusData(encodeProcedure(String(params?.gov_action_deposit ?? 100_000_000_000), RETURN_ADDRESS, data));
}

let placeholder: Promise<MockTransaction> | null = null;

/** julc's sample proposing transaction (one key input and output), fetched once. */
function placeholderTx(): Promise<MockTransaction> {
	placeholder ??= julc.run(async (c) => {
		if (!c.uplc?.defaultTransaction) throw new Error('This julc engine can’t build transactions (VM-only build).');
		return (await c.uplc.defaultTransaction({ purpose: 'propose' })) as MockTransaction;
	});
	placeholder.catch(() => (placeholder = null));
	return placeholder;
}

/** A run plan for the check, so the result can be shown and debugged like a re-run. */
export async function guardrailPlan(action: GuardrailAction, script: GuardrailScript, params: EpochParams | null): Promise<RunPlan> {
	const procedure = procedureOf(action, params, script.hash);
	const pv = params?.protocol_major_ver ?? null;
	const protocol = pv == null ? 11 : Math.min(11, Math.max(10, pv));
	const target: Target = { language: 'PlutusV3', protocol };
	const costs = params?.cost_models_raw?.PlutusV3;
	const tx = await placeholderTx();
	return {
		key: 'propose#0',
		purpose: 'propose',
		index: 0,
		scriptHash: script.hash,
		language: 'V3',
		declared: null,
		redeemer: 'd87980',
		datum: null,
		hashedDatums: [],
		witnessDatums: [],
		proposals: [procedure],
		notes: [],
		request: {
			script: { script: script.cbor, language: 'V3' },
			transaction: {
				...tx,
				purpose: { type: 'propose', index: 0 },
				redeemer: { format: 'cbor', value: 'd87980' },
				proposals: [
					{
						deposit: String(params?.gov_action_deposit ?? 100_000_000_000),
						returnCredential: 'key:' + '00'.repeat(28),
						actionType: action.type === 'PARAMETER_CHANGE_ACTION' ? 'parameterChange' : 'treasuryWithdrawals',
						guardrail: script.hash,
						withdrawals: null
					}
				]
			} as MockTransaction,
			target,
			costModel: costs?.length ? { target, parameters: costs.map((c) => BigInt(c)) } : null,
			...(params?.max_tx_ex_steps ? { maxCpu: BigInt(String(params.max_tx_ex_steps)) } : {}),
			...(params?.max_tx_ex_mem ? { maxMem: BigInt(String(params.max_tx_ex_mem)) } : {})
		}
	};
}

/** Run the guardrail on an action. A RunResult like the Scripts tab's, so verdicts and the debugger apply. */
export async function checkGuardrail(action: GuardrailAction, script: GuardrailScript, params: EpochParams | null): Promise<RunResult> {
	let plan: RunPlan;
	try {
		plan = await guardrailPlan(action, script, params);
	} catch (e) {
		const stub = { key: 'propose#0', purpose: 'propose', index: 0, scriptHash: script.hash, language: 'V3', declared: null, redeemer: null, datum: null, hashedDatums: [], witnessDatums: [], proposals: [], request: null, notes: [] } as RunPlan;
		return { state: 'error', plan: stub, error: (e as Error).message };
	}
	const started = performance.now();
	try {
		const first = await julc.run((c) => c.uplc!.evaluateTransaction(plan.request!));
		if (!first.scriptContextCbor) return { state: 'error', plan, error: first.error ?? 'julc could not build the proposal’s context.' };
		const fixed = fixContext(first.scriptContextCbor, 'V3', null, [], [], plan.proposals);
		if (!fixed?.restoredProposals) return { state: 'error', plan, error: 'The proposal couldn’t be put into julc’s context.' };
		const args = [{ format: 'cbor' as const, value: fixed.cbor }];
		const evaluation = await julc.run((c) => c.vm.evaluate(vmRequest(plan, args)));
		if (evaluation.ok === false && evaluation.error) return { state: 'error', plan, error: evaluation.error };
		return { state: 'done', plan, evaluation: { ...evaluation, scriptContextCbor: fixed.cbor, language: 'PlutusV3' }, args, ms: Math.round(performance.now() - started) };
	} catch (e) {
		return { state: 'error', plan, error: (e as Error)?.message ?? String(e) };
	}
}

/**
 * Which changed parameters the guardrail rejects on their own. Values set together in the ledger (prices, ex-units,
 * a threshold group) are checked together. Returns parameter names by verdict.
 */
export async function checkEachParam(change: ParamChange, script: GuardrailScript, params: EpochParams | null): Promise<Record<string, 'ok' | 'rejected' | 'invalid'>> {
	const groups = new Map<number, string[]>();
	for (const name of Object.keys(change.values)) {
		const def = paramByName[name];
		if (def) groups.set(def.key, [...(groups.get(def.key) ?? []), name]);
	}
	const verdicts: Record<string, 'ok' | 'rejected' | 'invalid'> = {};
	for (const names of groups.values()) {
		const part: ParamChange = { values: Object.fromEntries(names.map((n) => [n, change.values[n]])) };
		let verdict: 'ok' | 'rejected' | 'invalid';
		try {
			encodeParamChange(part, params);
			const r = await checkGuardrail({ type: 'PARAMETER_CHANGE_ACTION', change: part }, script, params);
			verdict = r.state === 'done' && (r.evaluation?.accepted ?? r.evaluation?.status === 'success') ? 'ok' : 'rejected';
		} catch (e) {
			verdict = e instanceof ParamError ? 'invalid' : 'rejected';
		}
		for (const n of names) verdicts[n] = verdict;
	}
	if (change.costModels && Object.keys(change.costModels).length) {
		const r = await checkGuardrail({ type: 'PARAMETER_CHANGE_ACTION', change: { values: {}, costModels: change.costModels } }, script, params);
		verdicts.costModels = r.state === 'done' && (r.evaluation?.accepted ?? r.evaluation?.status === 'success') ? 'ok' : 'rejected';
	}
	return verdicts;
}

export { PARAMS };
