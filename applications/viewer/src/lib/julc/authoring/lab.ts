import { bech32Decode, bech32Encode } from '$lib/bech32';
import { encodePlutusData, type PlutusData } from '$lib/cbor';
import type { DataInput, MockTransaction, MockTxIn, PlutusVersion, Target, TransactionRequest } from '../types';
import { value, type RunPlan, type StoreUtxo } from '../replay/build';
import { slotToPosixMs, type SlotConfig } from '../replay/slots';

/*
 * The validator lab: run a script that isn't (necessarily) on chain against real chain state. A UTxO from the store
 * is spent as if it were locked at the script (or a policy mints, or a stake script withdraws), with the redeemer,
 * datum, signers, validity range and outputs the user sets. The result is a RunPlan, so the re-run pipeline
 * (context fixes), the debugger and fixture export work as for on-chain runs.
 */

export type LabPurpose = 'spend' | 'mint' | 'reward';

export interface LabScript {
	/** CBOR of the compiled script, before parameters are applied. */
	cbor: string;
	language: PlutusVersion;
	/** Script parameters (CBOR data), applied by julc in order. */
	params: string[];
	/** Hash with the parameters applied: the script's address, policy id or stake credential. */
	hash: string;
}

export interface LabTx {
	purpose: LabPurpose;
	/** The UTxO the script spends (spend), or a UTxO that pays the fee (mint, reward). */
	utxo: StoreUtxo | null;
	/** Spend: treat the UTxO as locked at the script, with `datum`. */
	atScript: boolean;
	/** Spend: CBOR of the datum the script sees (inline for V2/V3). */
	datum: string | null;
	redeemer: string;
	referenceInputs: StoreUtxo[];
	outputs: { address: string; lovelace: string }[];
	/** Mint: token name (hex) → quantity, under the script's policy. */
	mint: { tokenName: string; quantity: string }[];
	/** Reward: amount withdrawn from the script's stake credential (usually 0, "withdraw zero"). */
	withdrawal: string;
	/** Other withdrawals in the transaction: a stake address or "script:<hash>" / "key:<hash>", and lovelace. */
	withdrawals?: { credential: string; amount: string }[];
	signers: string[];
	validFrom: number | null;
	validTo: number | null;
	fee: string;
}

export interface LabEnv {
	params: { cost_models_raw?: Record<string, (number | string)[]> | null; protocol_major_ver?: number | null; max_tx_ex_mem?: number | string | null; max_tx_ex_steps?: number | string | null } | null;
	slots: SlotConfig | null;
}

/** A placeholder UTxO for when the chain has none to offer (10 ada at a dummy key address). */
export const SAMPLE_UTXO: StoreUtxo = { tx_hash: 'c0ffee'.padEnd(64, '0'), output_index: 0, address: '', amount: [{ unit: 'lovelace', quantity: '10000000' }] };

const hexRe = /^([0-9a-f]{2})*$/i;

/** The payment key hash of a Shelley address with a key payment part, else null. */
export function paymentKeyHash(address: string): string | null {
	const d = bech32Decode(address);
	if (!d || d.bytes.length < 29 || !/^addr/.test(d.hrp)) return null;
	const type = d.bytes[0] >> 4;
	// Header types 0, 2, 4, 6: key payment credential.
	if (type > 7 || type % 2 !== 0) return null;
	return [...d.bytes.slice(1, 29)].map((b) => b.toString(16).padStart(2, '0')).join('');
}

/** A script parameter typed as text: hex for byte types, an integer for number types, else Plutus data (JSON or CBOR). */
export function paramData(type: string | null | undefined, text: string, parseData: (t: string) => PlutusData): string {
	const t = String(type ?? '').toLowerCase();
	const v = text.trim();
	if (/byte|bytestring|hash|policyid|tokenname/.test(t)) {
		if (!hexRe.test(v)) throw new Error('expected hex');
		return encodePlutusData({ bytes: v.toLowerCase() });
	}
	if (/^(biginteger|integer|int|long|posixtime)$/.test(t)) {
		if (!/^-?\d+$/.test(v)) throw new Error('expected a whole number');
		return encodePlutusData({ int: String(BigInt(v)) });
	}
	return encodePlutusData(parseData(v));
}

/** A placeholder value for a field type from julc's `check` (Integer, ByteString, records…). */
function placeholder(type: string | null | undefined): PlutusData {
	const t = String(type ?? '').toLowerCase();
	if (/^(integer|biginteger|int|long|posixtime|lovelace)$/.test(t)) return { int: '0' };
	if (/byte|hash|policyid|tokenname|txid/.test(t)) return { bytes: '' };
	if (t === 'boolean') return { constructor: 0, fields: [] };
	if (/list/.test(t)) return { list: [] };
	if (/map/.test(t)) return { map: [] };
	return { constructor: 0, fields: [] };
}

/** Constructor `tag` with a placeholder per field: a starting point for a datum or redeemer. */
export function templateOf(tag: number, fields: { type: string | null }[] | null | undefined): string {
	return encodePlutusData({ constructor: tag, fields: (fields ?? []).map((f) => placeholder(f.type)) });
}

/** julc's purpose names (SPENDING, MINTING, …) as the lab's. */
export function labPurposeOf(purpose: string | null | undefined): LabPurpose | null {
	const p = String(purpose ?? '').toUpperCase();
	if (p.startsWith('SPEND')) return 'spend';
	if (p.startsWith('MINT')) return 'mint';
	if (p.startsWith('WITHDRAW') || p.startsWith('REWARD')) return 'reward';
	return null;
}

/** The enterprise address of a script hash (header 0x70 / 0x71). */
export function scriptAddress(hash: string, mainnet: boolean): string {
	const bytes = new Uint8Array([mainnet ? 0x71 : 0x70, ...hash.match(/../g)!.map((h) => parseInt(h, 16))]);
	return bech32Encode(mainnet ? 'addr' : 'addr_test', bytes);
}

/** Ledger order of credentials: script hashes before key hashes, then by hash. */
function credentialKey(credential: string): string {
	const m = credential.match(/^(script|key):([0-9a-f]{56})$/i);
	if (m) return (m[1].toLowerCase() === 'script' ? '0' : '1') + m[2].toLowerCase();
	const d = bech32Decode(credential);
	if (!d || d.bytes.length !== 29 || !/^stake/.test(d.hrp)) throw new Error(`“${credential}” isn’t a stake address or script:/key: hash.`);
	return ((d.bytes[0] & 0x10) !== 0 ? '0' : '1') + [...d.bytes.slice(1)].map((b) => b.toString(16).padStart(2, '0')).join('');
}

function withdrawalsOf(script: LabScript, tx: LabTx): { credential: string; amount: string }[] {
	const list = (tx.withdrawals ?? []).filter((w) => w.credential.trim()).map((w) => ({ credential: w.credential.trim(), amount: w.amount.trim() || '0' }));
	if (tx.purpose === 'reward') list.push({ credential: `script:${script.hash}`, amount: tx.withdrawal.trim() || '0' });
	for (const w of list) if (!/^\d+$/.test(w.amount)) throw new Error('Withdrawal amounts must be whole numbers of lovelace.');
	const keyed = list.map((w) => ({ w, k: credentialKey(w.credential) }));
	return keyed.sort((a, b) => (a.k < b.k ? -1 : a.k > b.k ? 1 : 0)).map((x) => x.w);
}

const cbor = (value: string): DataInput => ({ format: 'cbor', value });
const byRef = (a: { txId: string; index: bigint }, b: { txId: string; index: bigint }) => (a.txId < b.txId ? -1 : a.txId > b.txId ? 1 : Number(a.index - b.index));

/** Build the run plan for the lab's transaction; throws with a readable message when an input is invalid. */
export function labPlan(script: LabScript, tx: LabTx, env: LabEnv): RunPlan {
	const notes: string[] = [];
	if (!hexRe.test(tx.redeemer) || !tx.redeemer) throw new Error('The redeemer isn’t valid Plutus data.');
	const utxo = tx.utxo ?? SAMPLE_UTXO;
	const spending = tx.purpose === 'spend';
	const own = spending && tx.atScript;
	if (own && script.language !== 'V3' && !tx.datum) throw new Error(`A Plutus ${script.language} spending script needs a datum.`);

	const inputOf = (u: StoreUtxo, mine: boolean): MockTxIn => ({
		txId: u.tx_hash,
		index: BigInt(u.output_index),
		address: { payment: mine || !u.address ? (mine ? '$self' : 'key:' + '00'.repeat(28)) : u.address },
		value: value(u.amount),
		datum: mine && tx.datum ? (script.language === 'V1' ? { kind: 'hash', hash: '', data: cbor(tx.datum) } : { kind: 'inline', data: cbor(tx.datum) }) : u.inline_datum ? { kind: 'inline', data: cbor(u.inline_datum) } : { kind: 'none' },
		referenceScript: u.reference_script_hash || null
	});
	const inputs = [inputOf(utxo, own)].sort(byRef);
	const refs = tx.referenceInputs.map((u) => inputOf(u, false)).sort(byRef);
	if (!tx.utxo) notes.push('No UTxO was chosen, so a sample one (10 ada) is spent.');

	const toPosix = (slot: number | null) => {
		if (slot == null) return null;
		if (!env.slots) throw new Error('Slot times for this network are unknown, so a validity range can’t be set.');
		return slotToPosixMs(slot, env.slots);
	};

	let purpose: MockTransaction['purpose'];
	if (spending) purpose = { type: 'spend', index: inputs.findIndex((i) => i.txId === utxo.tx_hash && i.index === BigInt(utxo.output_index)) };
	else if (tx.purpose === 'mint') purpose = { type: 'mint', target: script.hash };
	else purpose = { type: 'reward', target: `script:${script.hash}` };

	const mint = tx.purpose === 'mint' ? tx.mint.filter((m) => m.quantity.trim()) : [];
	for (const m of mint) {
		if (!hexRe.test(m.tokenName) || m.tokenName.length > 64) throw new Error(`Token name “${m.tokenName}” must be hex (up to 32 bytes).`);
		if (!/^-?\d+$/.test(m.quantity.trim()) || BigInt(m.quantity.trim()) === 0n) throw new Error('Mint quantities must be non-zero whole numbers.');
	}
	if (!/^\d+$/.test(tx.fee.trim())) throw new Error('The fee must be a whole number of lovelace.');
	for (const s of tx.signers) if (!/^[0-9a-f]{56}$/i.test(s)) throw new Error(`Signer “${s}” isn’t a 28-byte key hash.`);
	for (const o of tx.outputs) if (!/^\d+$/.test(o.lovelace.trim())) throw new Error('Output amounts must be whole numbers of lovelace.');

	const transaction: MockTransaction = {
		purpose,
		redeemer: cbor(tx.redeemer),
		inputs,
		referenceInputs: refs,
		outputs: tx.outputs.map((o) => ({ address: { payment: o.address.trim() || 'key:' + '00'.repeat(28) }, value: { lovelace: o.lovelace.trim(), assets: [] }, datum: { kind: 'none' as const } })),
		fee: tx.fee.trim(),
		mint: mint.map((m) => ({ policyId: script.hash, tokenName: m.tokenName.toLowerCase(), quantity: m.quantity.trim() })).sort((a, b) => (a.tokenName < b.tokenName ? -1 : 1)),
		certificates: [],
		withdrawals: withdrawalsOf(script, tx),
		validRange: { from: toPosix(tx.validFrom), fromInclusive: true, to: toPosix(tx.validTo), toInclusive: false },
		signatories: [...new Set(tx.signers.map((s) => s.toLowerCase()))].sort(),
		datums: [],
		txId: '7ab0'.padEnd(64, '0'),
		votes: [],
		proposals: []
	};

	const pv = env.params?.protocol_major_ver ?? null;
	const protocol = pv == null ? 11 : Math.min(11, Math.max(10, pv));
	const target: Target = { language: `Plutus${script.language}`, protocol };
	const costs = env.params?.cost_models_raw?.[`Plutus${script.language}`];
	const request: TransactionRequest = {
		script: { script: script.cbor, language: script.language, ...(script.params.length ? { params: script.params.map(cbor) } : {}) },
		transaction,
		target,
		costModel: costs?.length ? { target, parameters: costs.map((c) => BigInt(c)) } : null,
		...(env.params?.max_tx_ex_steps ? { maxCpu: BigInt(env.params.max_tx_ex_steps) } : {}),
		...(env.params?.max_tx_ex_mem ? { maxMem: BigInt(env.params.max_tx_ex_mem) } : {})
	};
	return {
		key: `${tx.purpose}#${purpose.index ?? 0}`,
		purpose: tx.purpose,
		index: purpose.index ?? 0,
		scriptHash: script.hash,
		language: script.language,
		declared: null,
		redeemer: tx.redeemer,
		datum: own ? tx.datum : null,
		hashedDatums: [],
		witnessDatums: [],
		proposals: [],
		request,
		notes
	};
}
