import { PARAMS } from './action';
import type { GuardrailAction } from './guardrail';

/* A proposal drafted on the guardrail check page, and how it is kept in a shareable URL. */

export interface Draft {
	kind: 'param' | 'treasury';
	/** Parameter values by name (strings as typed). */
	values: Record<string, string>;
	/** Cost models of a proposal the draft started from (too long for a URL; `from` brings them back). */
	costModels?: Record<string, string>;
	withdrawals: { address: string; amount: string }[];
	/** The proposal the draft started from ("<tx>_<index>"), if any. */
	from: string | null;
}

export function actionOf(d: Draft): GuardrailAction {
	return d.kind === 'treasury'
		? { type: 'TREASURY_WITHDRAWALS_ACTION', withdrawals: d.withdrawals.filter((w) => w.address.trim() || w.amount.trim()).map((w) => ({ address: w.address.trim(), amount: w.amount.trim() })) }
		: { type: 'PARAMETER_CHANGE_ACTION', change: { values: Object.fromEntries(Object.entries(d.values).map(([k, v]) => [k, v.trim()])), ...(d.costModels ? { costModels: d.costModels } : {}) } };
}

export const isEmpty = (d: Draft) => (d.kind === 'treasury' ? !d.withdrawals.some((w) => w.address.trim()) : !Object.keys(d.values).length && !d.costModels);

/** The query string that reopens this draft (and script, when it isn't the default). */
export function queryOf(d: Draft, script: string | null): string {
	const q = new URLSearchParams();
	if (d.kind === 'treasury') {
		q.set('type', 'treasury');
		for (const w of d.withdrawals) if (w.address.trim()) q.append('w', `${w.address.trim()}:${w.amount.trim()}`);
	} else {
		// Cost models can't go into a link; a draft that still has them points at its proposal instead.
		if (d.costModels && d.from) q.set('from', d.from);
		for (const p of PARAMS) if (d.values[p.name] != null) q.set(`p.${p.name}`, d.values[p.name]);
	}
	if (script) q.set('script', script);
	return q.toString();
}
