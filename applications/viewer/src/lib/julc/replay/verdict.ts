import type { RunResult } from './run';

/* How a re-run compares with the chain, shared by the Scripts tab, the monitor and the what-if lab. */

export const passed = (r?: RunResult | null) => !!r && r.state === 'done' && !!r.evaluation && (r.evaluation.accepted ?? r.evaluation.status === 'success');

/** CPU and memory match the declared ex-units to the unit. */
export const exact = (r?: RunResult | null) =>
	passed(r) && r!.plan.declared != null && r!.evaluation!.cpu === r!.plan.declared.steps && r!.evaluation!.mem === r!.plan.declared.mem;

/**
 * Transaction builders often declare the evaluated budget plus a safety margin (e.g. +20%). When memory and CPU are
 * both scaled by the same factor, the run is a reproduction too; returns the margin in percent.
 */
export function margin(r?: RunResult | null): number | null {
	if (!passed(r) || !r!.plan.declared || exact(r) || !r!.evaluation!.mem || !r!.evaluation!.cpu) return null;
	const m = Number(r!.plan.declared.mem) / Number(r!.evaluation!.mem);
	const c = Number(r!.plan.declared.steps) / Number(r!.evaluation!.cpu);
	return m > 1.001 && Math.abs(m - c) < 0.0005 ? Math.round((m - 1) * 1000) / 10 : null;
}

export type Outcome = 'passed' | 'failed' | 'out of budget' | 'couldn’t run' | 'running';

export function outcome(r?: RunResult | null): Outcome {
	if (!r || r.state === 'running') return 'running';
	if (r.state !== 'done') return 'couldn’t run';
	if (passed(r)) return 'passed';
	return r.evaluation?.status === 'budgetExhausted' ? 'out of budget' : 'failed';
}
