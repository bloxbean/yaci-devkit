import { api, tryApi } from '$lib/api/client';
import type { LiveBlock } from '$lib/live/chain.svelte';
import { purposeOf, type StoreScriptRun } from '../replay/build';
import { loadPageTx } from '../replay/load';
import { prepareRuns, runAll, type RunResult } from '../replay/run';
import { exact, margin, outcome, type Outcome } from '../replay/verdict';

/*
 * The live script monitor: for every new block, find the transactions that ran scripts and re-run them in the
 * browser, one transaction at a time. On public networks only a few script transactions per block are re-run
 * (`cap`), as each needs several store requests.
 */

export interface MonitoredRun {
	key: string;
	purpose: string;
	index: number;
	scriptHash: string;
	language: string | null;
	outcome: Outcome;
	verdict: 'exact' | 'margin' | null;
	cpu: bigint | null;
	mem: bigint | null;
	/** Share of the per-transaction limits, in percent. */
	cpuPct: number;
	memPct: number;
	error: string | null;
}

export interface MonitoredTx {
	hash: string;
	block: number;
	time: number;
	runs: MonitoredRun[];
	ms: number;
}

/** A run using at least this share of a per-transaction limit is flagged. */
export const NEAR_LIMIT = 70;

const hasRedeemer = (s: StoreScriptRun) => !!s.redeemer;

function summarize(r: RunResult, limits: { mem: bigint; steps: bigint }): MonitoredRun {
	const ev = r.evaluation;
	const m = margin(r);
	const pct = (a: bigint | null | undefined, b: bigint) => (a == null || !b ? 0 : Number((a * 10000n) / b) / 100);
	return {
		key: r.plan.key,
		purpose: r.plan.purpose,
		index: r.plan.index,
		scriptHash: r.plan.scriptHash,
		language: r.plan.language,
		outcome: outcome(r),
		verdict: exact(r) ? 'exact' : m != null ? 'margin' : null,
		cpu: ev?.cpu ?? null,
		mem: ev?.mem ?? null,
		cpuPct: pct(ev?.cpu, limits.steps),
		memPct: pct(ev?.mem, limits.mem),
		error: r.error ?? (ev && outcome(r) !== 'passed' ? (ev.message ?? ev.error) : null)
	};
}

export class ScriptMonitor {
	running = $state(false);
	entries = $state.raw<MonitoredTx[]>([]);
	queued = $state(0);
	busy = $state(false);
	lastBlock = $state<number | null>(null);
	failure = $state<string | null>(null);
	/** Script transactions re-run per block; Infinity for all. */
	cap = $state(Infinity);
	stats = $state({ blocks: 0, scriptTxs: 0, skipped: 0, runs: 0, reproduced: 0, failed: 0, couldnt: 0, near: 0, peak: 0 });

	#seen = new Set<number>();
	#queue: { hash: string; block: LiveBlock; scripts: StoreScriptRun[] }[] = [];
	#limits: { mem: bigint; steps: bigint } | null = null;

	start() {
		this.running = true;
		this.failure = null;
	}

	stop() {
		this.running = false;
		this.#queue = [];
		this.queued = 0;
	}

	clear() {
		this.entries = [];
		this.stats = { blocks: 0, scriptTxs: 0, skipped: 0, runs: 0, reproduced: 0, failed: 0, couldnt: 0, near: 0, peak: 0 };
	}

	/** A new block arrived (or a recent one, to fill the feed when starting). */
	async block(b: LiveBlock) {
		if (!this.running || this.#seen.has(b.number)) return;
		this.#seen.add(b.number);
		this.lastBlock = Math.max(this.lastBlock ?? 0, b.number);
		this.stats.blocks++;
		if (!b.txCount) return;
		const txs = (await tryApi<{ tx_hash: string }[]>(`/blocks/${b.number}/txs`)) ?? [];
		// Which transactions ran scripts: a few requests at a time.
		const withScripts: { hash: string; scripts: StoreScriptRun[] }[] = [];
		for (let i = 0; i < txs.length; i += 4) {
			const batch = await Promise.all(txs.slice(i, i + 4).map(async (t) => ({ hash: t.tx_hash, scripts: (await tryApi<StoreScriptRun[]>(`/txs/${t.tx_hash}/scripts`)) ?? [] })));
			for (const t of batch) if (t.scripts.some(hasRedeemer)) withScripts.push(t);
			if (!this.running) return;
		}
		this.stats.scriptTxs += withScripts.length;
		const picked = withScripts.slice(0, this.cap);
		this.stats.skipped += withScripts.length - picked.length;
		for (const t of picked) this.#queue.push({ ...t, block: b });
		this.queued = this.#queue.length;
		this.#drain();
	}

	async #drain() {
		if (this.busy) return;
		this.busy = true;
		try {
			while (this.running && this.#queue.length) {
				const job = this.#queue.shift()!;
				this.queued = this.#queue.length;
				await this.#replay(job);
			}
		} finally {
			this.busy = false;
		}
	}

	async #replay(job: { hash: string; block: LiveBlock; scripts: StoreScriptRun[] }) {
		const started = performance.now();
		try {
			this.#limits ??= await api<{ max_tx_ex_mem: number; max_tx_ex_steps: number }>('/epochs/latest/parameters').then((p) => ({
				mem: BigInt(p?.max_tx_ex_mem ?? 14_000_000),
				steps: BigInt(p?.max_tx_ex_steps ?? 10_000_000_000)
			}));
			const page = await loadPageTx(job.hash, job.block.epoch, job.scripts);
			const plans = await prepareRuns(page);
			const results: Record<string, RunResult> = {};
			const engineFailure = await runAll(plans, (k, r) => (results[k] = r));
			if (engineFailure) {
				this.failure = engineFailure;
				this.stop();
				return;
			}
			const runs = plans.map((p) => summarize(results[p.key], this.#limits!));
			for (const r of runs) {
				this.stats.runs++;
				if (r.verdict) this.stats.reproduced++;
				if (r.outcome === 'failed' || r.outcome === 'out of budget') this.stats.failed++;
				if (r.outcome === 'couldn’t run') this.stats.couldnt++;
				const top = Math.max(r.cpuPct, r.memPct);
				if (top >= NEAR_LIMIT) this.stats.near++;
				this.stats.peak = Math.max(this.stats.peak, top);
			}
			const entry: MonitoredTx = { hash: job.hash, block: job.block.number, time: job.block.time, runs, ms: Math.round(performance.now() - started) };
			this.entries = [entry, ...this.entries].slice(0, 100);
		} catch (e) {
			const entry: MonitoredTx = {
				hash: job.hash,
				block: job.block.number,
				time: job.block.time,
				ms: Math.round(performance.now() - started),
				runs: job.scripts.filter(hasRedeemer).map((s) => {
					const r = typeof s.redeemer === 'string' ? JSON.parse(s.redeemer) : s.redeemer;
					return { key: '', purpose: purposeOf(r?.tag) ?? String(r?.tag), index: Number(r?.index ?? 0), scriptHash: s.script_hash, language: null, outcome: 'couldn’t run' as const, verdict: null, cpu: null, mem: null, cpuPct: 0, memPct: 0, error: (e as Error)?.message ?? String(e) };
				})
			};
			this.stats.couldnt += entry.runs.length;
			this.entries = [entry, ...this.entries].slice(0, 100);
		}
	}
}
