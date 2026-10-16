import { api, tryApi, wsUrl } from '$lib/api/client';

/*
 * Live chain state shared by the whole app: tip, recent blocks and transactions, network and epoch.
 * Uses Yaci Store's WebSocket when there is one, reconnecting with backoff, and falls back to polling
 * /blocks/latest when there isn't (for example a public Yaci Store without the live module).
 */

export interface LiveBlock {
	number: number;
	hash?: string;
	slot: number;
	epoch: number;
	epochSlot?: number;
	time: number; // seconds
	txCount: number;
	size: number;
	leader?: string;
	fees?: number;
	receivedAt: number; // ms, when the viewer first saw it
}

export interface LiveTx {
	hash: string;
	block: number;
	slot: number;
	output: number;
	fee?: number;
	outputAddresses: string[];
	receivedAt: number;
}

export interface NetworkInfo {
	magic: number | null;
	name: string;
	kind: 'mainnet' | 'preprod' | 'preview' | 'sanchonet' | 'devnet' | 'custom';
	era?: string;
	synced?: boolean;
	syncPercent?: number;
	lagBlocks?: number;
}

export interface EpochInfo {
	epoch: number;
	start: number; // seconds
	end: number; // seconds
}

export type LiveStatus = 'idle' | 'connecting' | 'live' | 'polling' | 'offline';

const MAX_BLOCKS = 40;
const MAX_TXS = 40;
const POLL_MS = 4000;

export function networkFromMagic(magic: number | null | undefined): Pick<NetworkInfo, 'name' | 'kind'> {
	switch (magic) {
		case 764824073:
			return { name: 'Mainnet', kind: 'mainnet' };
		case 1:
			return { name: 'Preprod', kind: 'preprod' };
		case 2:
			return { name: 'Preview', kind: 'preview' };
		case 4:
			return { name: 'SanchoNet', kind: 'sanchonet' };
		case 42:
			return { name: 'Devnet', kind: 'devnet' };
		default:
			return { name: magic == null ? 'Unknown network' : `Network ${magic}`, kind: 'custom' };
	}
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

export function toBlock(b: Any, receivedAt = Date.now()): LiveBlock {
	const time = b.block_time ?? (b.time > 1e12 ? Math.round(b.time / 1000) : b.time);
	return {
		number: Number(b.number ?? b.height),
		hash: b.hash ?? undefined,
		slot: Number(b.slot),
		epoch: Number(b.epoch),
		epochSlot: b.epoch_slot ?? undefined,
		time: Number(time),
		txCount: Number(b.ntx ?? b.tx_count ?? 0),
		size: Number(b.size ?? 0),
		leader: b.slot_leader ?? undefined,
		fees: b.fees != null ? Number(b.fees) : b.fee != null ? Number(b.fee) : undefined,
		receivedAt
	};
}

export function toTx(t: Any, receivedAt = Date.now()): LiveTx {
	return {
		hash: t.hash ?? t.tx_hash,
		block: Number(t.block ?? t.block_number ?? t.block_height),
		slot: Number(t.slot),
		output: Number(t.output ?? t.total_output ?? 0),
		fee: t.fee != null ? Number(t.fee) : undefined,
		outputAddresses: t.output_addresses ?? [],
		receivedAt
	};
}

class Chain {
	status = $state<LiveStatus>('idle');
	blocks = $state<LiveBlock[]>([]);
	txs = $state<LiveTx[]>([]);
	network = $state<NetworkInfo | null>(null);
	epoch = $state<EpochInfo | null>(null);
	/** Ticks every second so relative times ("12s ago") stay current. */
	now = $state(Date.now());
	/** Set when the store can't be reached at all. */
	error = $state<string | null>(null);

	tip = $derived(this.blocks[0] ?? null);

	/** Average seconds between the recent blocks. */
	blockTime = $derived.by(() => {
		const b = this.blocks.slice(0, 20);
		if (b.length < 2) return null;
		const span = b[0].time - b[b.length - 1].time;
		return span > 0 ? span / (b.length - 1) : null;
	});

	private started = false;
	private ws: WebSocket | null = null;
	private wsOpened = false;
	private wsAttempts = 0;
	private pollTimer: ReturnType<typeof setTimeout> | null = null;
	private retryTimer: ReturnType<typeof setTimeout> | null = null;
	private pingTimer: ReturnType<typeof setInterval> | null = null;

	start() {
		if (this.started || typeof window === 'undefined') return;
		this.started = true;
		this.status = 'connecting';
		setInterval(() => (this.now = Date.now()), 1000);
		setInterval(() => this.refreshNetwork(), 30000);
		document.addEventListener('visibilitychange', () => {
			if (!document.hidden && this.status === 'polling') this.poll();
		});
		this.refreshNetwork();
		this.seed().finally(() => this.connect());
	}

	/** Initial recent blocks and txs over REST, so the UI isn't empty while the socket connects. */
	private async seed() {
		try {
			const [blocks, txs] = await Promise.all([
				api<Any>('/blocks', { params: { page: 1, count: 16 } }),
				tryApi<Any>('/txs', { params: { page: 1, count: 16 } })
			]);
			const seeded = (blocks?.blocks ?? []).map((b: Any) => toBlock(b, 0));
			this.mergeBlocks(seeded);
			this.mergeTxs((txs?.transaction_summaries ?? []).map((t: Any) => toTx(t, 0)));
			this.error = null;
		} catch (e) {
			this.error = (e as Error).message;
		}
	}

	async refreshNetwork() {
		const [sync, epoch] = await Promise.all([tryApi<Any>('/sync/status'), tryApi<Any>('/epochs/latest')]);
		if (sync) {
			const magic = sync.protocol_magic ?? null;
			this.network = {
				magic,
				...networkFromMagic(magic),
				era: sync.era,
				synced: sync.synced,
				syncPercent: sync.sync_percentage,
				lagBlocks: sync.lag_blocks
			};
		} else if (!this.network) {
			const genesis = await tryApi<Any>('/genesis');
			const magic = genesis?.network_magic ?? null;
			this.network = { magic, ...networkFromMagic(magic) };
		}
		if (epoch?.start_time) this.epoch = { epoch: epoch.epoch, start: epoch.start_time, end: epoch.end_time };
	}

	private connect() {
		const url = wsUrl();
		if (!url) return this.startPolling();
		try {
			this.ws = new WebSocket(url);
		} catch {
			return this.startPolling();
		}
		this.wsAttempts++;
		this.ws.onopen = () => {
			this.wsOpened = true;
			this.wsAttempts = 0;
			this.status = 'live';
			this.error = null;
			this.stopPolling();
			this.pingTimer = setInterval(() => this.ws?.readyState === 1 && this.ws.send('PING'), 30000);
		};
		this.ws.onmessage = (e) => this.onMessage(e.data);
		this.ws.onclose = () => {
			if (this.pingTimer) clearInterval(this.pingTimer);
			this.ws = null;
			// No socket on this store (never opened after two tries): poll, and look for a socket now and then.
			if (!this.wsOpened && this.wsAttempts >= 2) {
				this.startPolling();
				this.retryTimer = setTimeout(() => this.connect(), 60000);
				return;
			}
			this.wsOpened = false;
			this.status = this.status === 'polling' ? 'polling' : 'connecting';
			const delay = Math.min(15000, 1000 * 2 ** Math.min(this.wsAttempts, 4));
			this.retryTimer = setTimeout(() => this.connect(), delay);
		};
	}

	private onMessage(raw: string) {
		let d: Any;
		try {
			d = JSON.parse(raw);
		} catch {
			return;
		}
		switch (d.res_type) {
			case 'INIT_DATA':
				this.mergeBlocks((d.blocks ?? []).map((b: Any) => toBlock(b, 0)));
				this.mergeTxs((d.recent_txs ?? []).map((t: Any) => toTx(t, 0)));
				break;
			case 'BLOCK_DATA':
				this.addBlock(toBlock(d));
				break;
			case 'RECENT_TXS_DATA':
				this.mergeTxs((d.recent_txs ?? []).map((t: Any) => toTx(t)));
				break;
		}
	}

	private addBlock(b: LiveBlock) {
		const prevEpoch = this.tip?.epoch;
		this.mergeBlocks([b]);
		if (prevEpoch != null && b.epoch !== prevEpoch) this.refreshNetwork();
	}

	private mergeBlocks(list: LiveBlock[]) {
		if (!list.length) return;
		const byNumber = new Map<number, LiveBlock>();
		for (const b of [...this.blocks, ...list]) {
			const prev = byNumber.get(b.number);
			// Keep the first-seen time, but fill in fields (like hash) a later message has.
			byNumber.set(b.number, prev ? { ...prev, ...b, hash: b.hash ?? prev.hash, receivedAt: prev.receivedAt || b.receivedAt } : b);
		}
		this.blocks = [...byNumber.values()].sort((a, b) => b.number - a.number).slice(0, MAX_BLOCKS);
	}

	private mergeTxs(list: LiveTx[]) {
		if (!list.length) return;
		const seen = new Map<string, LiveTx>();
		for (const t of [...this.txs, ...list]) if (t.hash && !seen.has(t.hash)) seen.set(t.hash, t);
		this.txs = [...seen.values()].sort((a, b) => b.slot - a.slot).slice(0, MAX_TXS);
	}

	private startPolling() {
		if (this.status !== 'polling') this.status = 'polling';
		this.poll();
	}

	private stopPolling() {
		if (this.pollTimer) clearTimeout(this.pollTimer);
		this.pollTimer = null;
	}

	private async poll() {
		this.stopPolling();
		if (this.status !== 'polling') return;
		if (!document.hidden) {
			try {
				const latest = await api<Any>('/blocks/latest', { timeoutMs: 8000 });
				const block = toBlock(latest);
				const tip = this.tip?.number ?? 0;
				if (block.number > tip) {
					// Catch up a short gap so the conveyor doesn't skip blocks.
					if (tip && block.number - tip > 1 && block.number - tip < 16) {
						const gap = await tryApi<Any>('/blocks', { params: { page: 1, count: block.number - tip } });
						this.mergeBlocks((gap?.blocks ?? []).filter((b: Any) => b.number > tip).map((b: Any) => toBlock(b)));
					}
					this.addBlock(block);
					if (block.txCount > 0) {
						const txs = await tryApi<Any[]>(`/blocks/${block.number}/txs`);
						this.mergeTxs((txs ?? []).map((t) => toTx(t)));
					}
				}
				this.error = null;
			} catch (e) {
				this.error = (e as Error).message;
			}
		}
		this.pollTimer = setTimeout(() => this.poll(), POLL_MS);
	}
}

export const chain = new Chain();
