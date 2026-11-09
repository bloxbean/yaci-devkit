<script lang="ts">
	import { onMount } from 'svelte';
	import { tryApi } from '$lib/api/client';
	import { ada, num, shorten } from '$lib/format';
	import Panel from '$lib/ui/Panel.svelte';
	import Badge from '$lib/ui/Badge.svelte';
	import { julc, wasmSupported } from '../engine.svelte';
	import { passed } from '../replay/verdict';
	import type { RunResult } from '../replay/run';
	import { PARAMS, paramChangeOf, type ParamDef } from './action';
	import { checkEachParam, checkGuardrail, type EpochParams, type GuardrailAction } from './guardrail';
	import ShieldCheck from '@lucide/svelte/icons/shield-check';
	import ShieldX from '@lucide/svelte/icons/shield-x';
	import Play from '@lucide/svelte/icons/play';
	import LoaderCircle from '@lucide/svelte/icons/loader-circle';
	import FlaskConical from '@lucide/svelte/icons/flask-conical';

	/*
	 * On a parameter-change or treasury-withdrawal proposal: what it changes (against today's values), and its
	 * guardrail run in the browser, with a verdict per parameter. Runs on request, or by itself when the user opted
	 * in to automatic Script Lab runs.
	 */
	type Proposal = { tx_hash: string; index: number; type: string; details?: unknown };
	let { proposal }: { proposal: Proposal } = $props();

	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	const details = $derived((proposal.details ?? {}) as Record<string, any>);
	const policy = $derived<string | null>(details.policyHash ?? null);
	const change = $derived(paramChangeOf(details.protocolParamUpdate));
	const rows = $derived(PARAMS.filter((p) => change.values[p.name] != null));
	const isParam = $derived(proposal.type === 'PARAMETER_CHANGE_ACTION');
	const id = $derived(`${proposal.tx_hash}_${proposal.index}`);

	let params = $state.raw<EpochParams | null>(null);
	let phase = $state<'idle' | 'running' | 'done' | 'error'>('idle');
	let failure = $state<string | null>(null);
	let result = $state.raw<RunResult | null>(null);
	let each = $state.raw<Record<string, 'ok' | 'rejected' | 'invalid'>>({});
	let canRun = $state(false);

	onMount(async () => {
		params = await tryApi<EpochParams>('/epochs/latest/parameters');
		canRun = !!policy && wasmSupported() && !!(await julc.available());
		if (canRun && julc.auto) run();
	});

	async function run() {
		if (!policy) return;
		phase = 'running';
		failure = null;
		try {
			const body = await tryApi<{ cbor?: string }>(`/scripts/${policy}/cbor`);
			if (!body?.cbor) throw new Error(`The store has no code for guardrail ${shorten(policy, 8, 6)}.`);
			const script = { hash: policy, cbor: body.cbor };
			const action: GuardrailAction = isParam
				? { type: 'PARAMETER_CHANGE_ACTION', change }
				: { type: 'TREASURY_WITHDRAWALS_ACTION', withdrawals: Object.entries((details.withdrawals ?? {}) as Record<string, number | string>).map(([address, amount]) => ({ address, amount: String(amount) })) };
			const r = await checkGuardrail(action, script, params);
			if (r.state !== 'done') throw new Error(r.error ?? 'The guardrail couldn’t be run.');
			result = r;
			if (isParam && rows.length + (change.costModels ? 1 : 0) > 1) each = await checkEachParam(change, script, params);
			else if (isParam) each = Object.fromEntries([...rows.map((d) => d.name), ...(change.costModels ? ['costModels'] : [])].map((n) => [n, passed(r) ? 'ok' : 'rejected']));
			phase = 'done';
		} catch (e) {
			failure = (e as Error)?.message ?? String(e);
			phase = 'error';
		}
	}

	function show(def: ParamDef, v: unknown) {
		if (v == null) return '—';
		if (def.unit === 'lovelace') return `${ada(String(v), 'auto')} ada`;
		if (typeof v === 'number' && !Number.isInteger(v)) return String(v);
		return /^\d+$/.test(String(v)) ? num(String(v)) : String(v);
	}
	const accepted = $derived(passed(result));
</script>

{#if isParam || proposal.type === 'TREASURY_WITHDRAWALS_ACTION'}
	<Panel title={isParam ? 'Proposed changes' : 'Guardrail'} sub={policy ? `Checked by guardrail ${shorten(policy, 8, 6)} when it was submitted` : 'Submitted without a guardrail script'} class="mb-6">
		{#snippet icon()}<ShieldCheck class="h-4 w-4" />{/snippet}
		{#snippet actions()}
			<a class="y-btn h-8 text-xs" href="/governance/guardrail?from={id}" title="Change the values and check them against the guardrail"><FlaskConical class="h-3.5 w-3.5" />Try changes</a>
			{#if policy && canRun && phase !== 'running'}<button class="y-btn y-btn-primary h-8 text-xs" onclick={run}><Play class="h-3.5 w-3.5" />{phase === 'done' ? 'Run again' : 'Run the guardrail'}</button>{/if}
		{/snippet}

		{#if isParam}
			<div class="overflow-x-auto">
				<table class="w-full text-sm">
					<thead><tr class="border-b border-line text-left"><th class="y-label py-2 pr-4">Parameter</th><th class="y-label py-2 pr-4">Now</th><th class="y-label py-2 pr-4">Proposed</th><th class="py-2"></th></tr></thead>
					<tbody>
						{#each rows as def (def.name)}
							<tr class="border-b border-line/60 last:border-0">
								<td class="py-2 pr-4"><span class="font-medium">{def.label}</span> <span class="mono text-[11px] text-faint">{def.name.replace(/^.*\./, '')}</span></td>
								<td class="mono py-2 pr-4 text-xs text-muted tabular-nums">{show(def, params?.[def.current])}</td>
								<td class="mono py-2 pr-4 text-xs tabular-nums">{show(def, change.values[def.name])}</td>
								<td class="py-2 text-right">{#if each[def.name] === 'ok'}<Badge tone="ok">allowed</Badge>{:else if each[def.name] === 'rejected'}<Badge tone="bad">rejected</Badge>{/if}</td>
							</tr>
						{/each}
						{#if change.costModels}
							<tr class="border-b border-line/60 last:border-0">
								<td class="py-2 pr-4"><span class="font-medium">Cost models</span></td>
								<td class="py-2 pr-4 text-xs text-muted">—</td>
								<td class="py-2 pr-4 text-xs">Plutus {Object.keys(change.costModels).map((l) => `V${Number(l) + 1}`).join(', ')}</td>
								<td class="py-2 text-right">{#if each.costModels === 'ok'}<Badge tone="ok">allowed</Badge>{:else if each.costModels === 'rejected'}<Badge tone="bad">rejected</Badge>{/if}</td>
							</tr>
						{/if}
					</tbody>
				</table>
			</div>
		{/if}

		<div class="{isParam ? 'mt-4 border-t border-line pt-4' : ''} text-sm">
			{#if !policy}
				<p class="text-muted">The constitution had no guardrail script, so nothing checked this proposal’s values. “Try changes” checks them against any guardrail script.</p>
			{:else if phase === 'running'}
				<p class="flex items-center gap-2 text-muted" role="status"><LoaderCircle class="h-4 w-4 animate-spin text-brand" />{julc.status === 'loading' ? 'Starting the julc engine…' : 'Running the guardrail…'}</p>
			{:else if phase === 'error'}
				<p class="text-danger">{failure}</p>
			{:else if phase === 'done' && result}
				<div class="flex items-center gap-2.5" data-verdict={accepted ? 'accepted' : 'rejected'}>
					{#if accepted}<ShieldCheck class="h-5 w-5 text-ok" />{:else}<ShieldX class="h-5 w-5 text-danger" />{/if}
					<span class="font-medium">{accepted ? 'The guardrail accepts it' : 'The guardrail rejects it'}</span>
					<span class="text-xs text-muted tabular-nums">· {num(result.evaluation!.cpu)} CPU · {num(result.evaluation!.mem)} memory · in your browser</span>
				</div>
				<p class="mt-2 text-xs text-faint">Re-run with today’s cost model. The proposal transaction’s own run (with its declared budget) is on its <a class="y-link" href="/transactions/{proposal.tx_hash}?tab=scripts">Scripts tab</a>.</p>
			{:else if canRun}
				<p class="text-muted">Run the guardrail script on this proposal in your browser, to see which values it checks.</p>
			{:else}
				<p class="text-muted">This browser or viewer can’t run scripts (Script Lab).</p>
			{/if}
		</div>
	</Panel>
{/if}
