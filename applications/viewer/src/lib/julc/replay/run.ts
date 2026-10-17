import { julc } from '../engine.svelte';
import type { DataInput, Evaluation, JulcClient, TransactionRequest } from '../types';
import { buildRuns, type RunPlan } from './build';
import { TAG_ORDER, fixContext, ownRedeemer } from './fix-context';
import { loadReplayInput, type PageTx } from './load';

/*
 * The re-run pipeline shared by the Scripts tab and the debugger: rebuild the transaction, let julc build and
 * evaluate each redeemer's context, then apply the context corrections (fix-context.ts) and evaluate again with the
 * VM. Every result keeps the arguments the script was finally applied to, so the debugger replays exactly the run
 * the user saw.
 */

export interface RunResult {
	state: 'running' | 'done' | 'error' | 'skipped';
	plan: RunPlan;
	evaluation?: Evaluation;
	error?: string;
	ms?: number;
	/** The context was corrected and the script run again (julc#244 workarounds). */
	fixed?: boolean;
	/** What the script was applied to: V3 the context; V1/V2 (datum,) redeemer, context. */
	args?: DataInput[];
}

export type VmRequest = Omit<TransactionRequest, 'transaction'> & { args: DataInput[] };

export function prepareRuns(page: PageTx): Promise<RunPlan[]> {
	return loadReplayInput(page).then(buildRuns);
}

/** The VM arguments for a plan, given its ScriptContext (CBOR); null for a V1/V2 spend without a known datum. */
export function argsFor(plan: RunPlan, contextCbor: string): DataInput[] | null {
	const cbor = (value: string): DataInput => ({ format: 'cbor', value });
	if (plan.language === 'V3') return [cbor(contextCbor)];
	if (plan.purpose === 'spend') return plan.datum ? [cbor(plan.datum), cbor(plan.redeemer!), cbor(contextCbor)] : null;
	return [cbor(plan.redeemer!), cbor(contextCbor)];
}

/** The request to apply a plan's script to `args` with the VM (evaluate or debug). */
export function vmRequest(plan: RunPlan, args: DataInput[]): VmRequest {
	const req = plan.request!;
	return { script: req.script, args, target: req.target, costModel: req.costModel, maxCpu: req.maxCpu, maxMem: req.maxMem };
}

const usesUplc = (c: JulcClient) => {
	if (!c.uplc) throw new Error('This julc engine can’t evaluate transactions (VM-only build).');
	return c.uplc;
};

/**
 * Run every plan, reporting each result through `update` as it comes. Resolves with an error message when the
 * engine itself can't run (unsupported browser, failed start), otherwise null.
 */
export async function runAll(plans: RunPlan[], update: (key: string, result: RunResult) => void): Promise<string | null> {
	const results: Record<string, RunResult> = {};
	const set = (key: string, r: RunResult) => {
		results[key] = r;
		update(key, r);
	};
	for (const plan of plans) set(plan.key, plan.request ? { state: 'running', plan } : { state: 'skipped', plan, error: plan.skip });

	for (const plan of plans) {
		if (!plan.request) continue;
		const started = performance.now();
		try {
			const evaluation = await julc.run((c) => usesUplc(c).evaluateTransaction(plan.request!));
			const ms = Math.round(performance.now() - started);
			if (evaluation.ok === false && evaluation.error) set(plan.key, { state: 'error', plan, error: evaluation.error, ms });
			else {
				const args = evaluation.scriptContextCbor ? (argsFor(plan, evaluation.scriptContextCbor) ?? undefined) : undefined;
				set(plan.key, { state: 'done', plan, evaluation, ms, args });
			}
		} catch (e) {
			set(plan.key, { state: 'error', plan, error: (e as Error)?.message ?? String(e) });
			if (julc.status === 'unsupported' || julc.status === 'error') return julc.error ?? (e as Error)?.message ?? 'The julc engine failed.';
		}
	}
	await fixContexts(plans, results, set);
	return null;
}

/*
 * julc#244 workarounds (see fix-context.ts): put values in ledger order, restore datums given by hash, fix V1 shapes
 * and, when every run produced its context, give each context the complete redeemer map. Runs whose context changed
 * are evaluated again with the VM. Redeemers are only merged when all scripts share one Plutus version (V2 or V3):
 * purposes are encoded differently per version.
 */
async function fixContexts(plans: RunPlan[], results: Record<string, RunResult>, set: (key: string, r: RunResult) => void) {
	const withContext = plans.filter((p) => p.request && results[p.key]?.evaluation?.scriptContextCbor);
	const language = plans[0]?.language;
	let entries: NonNullable<ReturnType<typeof ownRedeemer>>[] | null = null;
	if (plans.length > 1 && withContext.length === plans.length && language !== 'V1' && plans.every((p) => p.language === language)) {
		const ordered = [...plans].sort((a, b) => TAG_ORDER[a.purpose] - TAG_ORDER[b.purpose] || a.index - b.index);
		const found = ordered.map((p) => ownRedeemer(results[p.key].evaluation!.scriptContextCbor!));
		if (found.every((e) => e)) entries = found as NonNullable<(typeof found)[number]>[];
	}
	for (const plan of withContext) {
		const first = results[plan.key];
		const fixed = fixContext(first.evaluation!.scriptContextCbor!, plan.language!, entries, plan.hashedDatums, plan.witnessDatums, plan.proposals);
		if (!fixed) continue;
		const args = argsFor(plan, fixed.cbor);
		if (!args) continue;
		const started = performance.now();
		try {
			const evaluation = await julc.run((c) => c.vm.evaluate(vmRequest(plan, args)));
			if (evaluation.ok === false && evaluation.error) continue;
			const did = [
				fixed.sortedValues && 'put asset values in ledger order',
				fixed.completedRedeemers && `added the other ${plans.length - 1} redeemer${plans.length > 2 ? 's' : ''}`,
				fixed.restoredDatums && 'added the datums given by hash under their on-chain hashes',
				fixed.v1Shape && 'gave V1 withdrawals and datums their list shape',
				fixed.restoredProposals && 'put the proposal’s governance action'
			].filter((d): d is string => !!d);
			const what = did.length > 1 ? `${did.slice(0, -1).join(', ')} and ${did.at(-1)}` : did[0];
			set(plan.key, {
				...first,
				evaluation: { ...evaluation, scriptContextCbor: fixed.cbor, language: first.evaluation!.language },
				ms: Math.round(performance.now() - started),
				fixed: true,
				args,
				plan: {
					...plan,
					notes: [`The viewer ${what} in the context, which julc doesn’t do yet (julc#244).`, ...plan.notes.filter((n) => !(fixed.completedRedeemers && n.includes('julc#244')))]
				}
			});
		} catch {
			/* keep the first result */
		}
	}
}
