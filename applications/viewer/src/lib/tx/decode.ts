import { bech32Encode } from '$lib/bech32';
import { blake2b } from '$lib/governance/blake2b';
import { bytesToHex, decodeCbor, rawOf, untag, type CborItem } from './cbor-item';

/*
 * Decode a Cardano transaction (Alonzo to Conway encodings) from its CBOR: [body, witness set, is valid, auxiliary
 * data]. Hashes come from the exact bytes: the transaction id from the body, datum and script hashes from the
 * witnesses, so the result can be checked against the chain. Fields follow the ledger's CDDL keys.
 */

export type Language = 'native' | 'V1' | 'V2' | 'V3';
export type RedeemerTag = 'spend' | 'mint' | 'cert' | 'reward' | 'vote' | 'propose';

export interface TxRef {
	txHash: string;
	index: number;
}
export interface Asset {
	policyId: string;
	name: string;
	quantity: bigint;
}
export interface TxValue {
	lovelace: bigint;
	assets: Asset[];
}
export type TxDatum = { kind: 'none' } | { kind: 'hash'; hash: string } | { kind: 'inline'; cbor: string; hash: string };
export interface Script {
	language: Language;
	/** Native: the script's CBOR. Plutus: the script bytes as witnessed (CBOR-wrapped flat), as julc takes them. */
	cbor: string;
	hash: string;
}
export interface TxOutput {
	address: string;
	addressHex: string;
	value: TxValue;
	datum: TxDatum;
	scriptRef: Script | null;
}
export interface Credential {
	script: boolean;
	hash: string;
}
export type DRep = { kind: 'key' | 'script'; hash: string } | { kind: 'abstain' | 'noConfidence' };
export interface Anchor {
	url: string;
	hash: string;
}
export interface Certificate {
	index: number;
	kind:
		| 'stakeRegistration'
		| 'stakeDeregistration'
		| 'stakeDelegation'
		| 'poolRegistration'
		| 'poolRetirement'
		| 'registration'
		| 'unregistration'
		| 'voteDelegation'
		| 'stakeVoteDelegation'
		| 'stakeRegDelegation'
		| 'voteRegDelegation'
		| 'stakeVoteRegDelegation'
		| 'authCommitteeHot'
		| 'resignCommitteeCold'
		| 'drepRegistration'
		| 'drepUnregistration'
		| 'drepUpdate'
		| 'genesisDelegation'
		| 'moveInstantaneousRewards'
		| 'unknown';
	/** The stake, DRep or committee credential the certificate is about (the one a script would witness). */
	credential: Credential | null;
	poolId?: string;
	drep?: DRep;
	deposit?: bigint;
	epoch?: bigint;
	hotCredential?: Credential;
	anchor?: Anchor | null;
	item: CborItem;
}
export interface Withdrawal {
	rewardAccount: string;
	address: string;
	credential: Credential;
	amount: bigint;
}
export type VoterKind = 'committeeKey' | 'committeeScript' | 'drepKey' | 'drepScript' | 'pool';
export interface Vote {
	voter: { kind: VoterKind; hash: string };
	action: TxRef;
	vote: 'no' | 'yes' | 'abstain';
	anchor: Anchor | null;
}
export type GovActionKind = 'parameterChange' | 'hardFork' | 'treasuryWithdrawals' | 'noConfidence' | 'updateCommittee' | 'newConstitution' | 'info';
export interface Proposal {
	deposit: bigint;
	rewardAccount: string;
	returnAddress: string;
	kind: GovActionKind;
	/** The guardrail (policy) script of a parameter change or treasury withdrawal. */
	policy: string | null;
	action: CborItem;
	anchor: Anchor | null;
}
export interface Redeemer {
	tag: RedeemerTag;
	index: number;
	data: string;
	exUnits: { mem: bigint; steps: bigint };
}
export interface DecodedTx {
	hash: string;
	size: number;
	isValid: boolean;
	inputs: TxRef[];
	referenceInputs: TxRef[];
	collateral: TxRef[];
	outputs: TxOutput[];
	collateralReturn: TxOutput | null;
	totalCollateral: bigint | null;
	fee: bigint;
	ttl: bigint | null;
	validityStart: bigint | null;
	mint: Asset[];
	certificates: Certificate[];
	withdrawals: Withdrawal[];
	requiredSigners: string[];
	networkId: number | null;
	auxiliaryDataHash: string | null;
	scriptDataHash: string | null;
	votes: Vote[];
	proposals: Proposal[];
	currentTreasury: bigint | null;
	donation: bigint | null;
	witnesses: {
		vkeys: { vkey: string; signature: string; keyHash: string }[];
		bootstrap: number;
		scripts: Script[];
		datums: { hash: string; cbor: string }[];
		redeemers: Redeemer[];
	};
	/** Transaction metadata (auxiliary data label map), if any. */
	metadata: CborItem | null;
	/** The decoded CBOR, for a byte-level view. */
	root: CborItem;
	bytes: Uint8Array;
}

const TAGS: RedeemerTag[] = ['spend', 'mint', 'cert', 'reward', 'vote', 'propose'];
const LANG_TAG: Record<Language, number> = { native: 0, V1: 1, V2: 2, V3: 3 };

class TxDecodeError extends Error {}
const fail = (msg: string): never => {
	throw new TxDecodeError(msg);
};

const hex = (b: Uint8Array) => bytesToHex(b);
const hash224 = (b: Uint8Array) => hex(blake2b(b, 28));
const hash256 = (b: Uint8Array) => hex(blake2b(b, 32));

function uint(i: CborItem | undefined, what: string): bigint {
	if (i?.type === 'uint') return i.value;
	return fail(`${what}: expected an unsigned integer`);
}
function int(i: CborItem | undefined, what: string): bigint {
	if (i?.type === 'uint' || i?.type === 'nint') return i.value;
	return fail(`${what}: expected an integer`);
}
function bytes(i: CborItem | undefined, what: string): Uint8Array {
	if (i?.type === 'bytes') return i.value;
	return fail(`${what}: expected bytes`);
}
function arr(i: CborItem | undefined, what: string): CborItem[] {
	const x = untag(i);
	if (x?.type === 'array') return x.items;
	return fail(`${what}: expected an array`);
}
function map(i: CborItem | undefined, what: string): { k: CborItem; v: CborItem }[] {
	const x = untag(i);
	if (x?.type === 'map') return x.entries;
	return fail(`${what}: expected a map`);
}
/** Fields of a map keyed by small unsigned integers. */
function fields(i: CborItem | undefined, what: string): Map<number, CborItem> {
	return new Map(map(i, what).map((e) => [Number(uint(e.k, `${what} key`)), e.v]));
}

// --- addresses ---

const B58 = '123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz';
function base58(b: Uint8Array) {
	let n = 0n;
	for (const x of b) n = (n << 8n) | BigInt(x);
	let out = '';
	while (n > 0n) {
		out = B58[Number(n % 58n)] + out;
		n /= 58n;
	}
	for (const x of b) {
		if (x !== 0) break;
		out = '1' + out;
	}
	return out;
}

/** A Shelley address in bech32, a Byron address in base58. */
export function addressText(b: Uint8Array): string {
	const type = b[0] >> 4;
	if (type === 8) return base58(b);
	const testnet = (b[0] & 0x0f) === 0;
	if (type === 14 || type === 15) return bech32Encode(testnet ? 'stake_test' : 'stake', b);
	return bech32Encode(testnet ? 'addr_test' : 'addr', b);
}

/** The payment credential of a Shelley address (null for Byron and reward addresses). */
export function paymentCredential(addressHex: string): Credential | null {
	const header = parseInt(addressHex.slice(0, 2), 16);
	const type = header >> 4;
	if (type > 7 || addressHex.length < 58) return null;
	return { script: (type & 1) === 1, hash: addressHex.slice(2, 58) };
}

function rewardAccount(b: Uint8Array): { address: string; credential: Credential } {
	return { address: addressText(b), credential: { script: (b[0] & 0x10) !== 0, hash: hex(b.subarray(1, 29)) } };
}

// --- values, outputs, scripts ---

function multiAsset(i: CborItem, what: string, signed = false): Asset[] {
	const out: Asset[] = [];
	for (const p of map(i, what)) {
		const policyId = hex(bytes(p.k, `${what} policy`));
		for (const a of map(p.v, `${what} assets`)) out.push({ policyId, name: hex(bytes(a.k, `${what} asset name`)), quantity: signed ? int(a.v, what) : uint(a.v, what) });
	}
	return out;
}

function value(i: CborItem | undefined): TxValue {
	if (i?.type === 'uint') return { lovelace: i.value, assets: [] };
	const [coin, ma] = arr(i, 'value');
	return { lovelace: uint(coin, 'value coin'), assets: ma ? multiAsset(ma, 'value') : [] };
}

function script(language: Language, raw: Uint8Array): Script {
	return { language, cbor: hex(raw), hash: hash224(new Uint8Array([LANG_TAG[language], ...raw])) };
}

/** A reference script: tag 24 around the CBOR of [0, native] | [1, v1 bytes] | [2, v2 bytes] | [3, v3 bytes]. */
function scriptRef(i: CborItem): Script {
	const wrapped = i.type === 'tag' && i.tag === 24 ? bytes(i.item, 'script ref') : fail('script ref: expected tag 24');
	const [lang, body] = arr(decodeCbor(wrapped).item, 'script ref');
	const l = Number(uint(lang, 'script ref language'));
	if (l === 0) return script('native', rawOf(wrapped, body));
	return script((['V1', 'V2', 'V3'] as const)[l - 1] ?? fail(`script ref: unknown language ${l}`), bytes(body, 'script ref'));
}

function output(i: CborItem): TxOutput {
	let addr: Uint8Array;
	let val: CborItem | undefined;
	let datum: TxDatum = { kind: 'none' };
	let ref: Script | null = null;
	if (i.type === 'array') {
		// Legacy (Shelley to Alonzo): [address, value, ? datum hash]
		addr = bytes(i.items[0], 'output address');
		val = i.items[1];
		if (i.items[2]) datum = { kind: 'hash', hash: hex(bytes(i.items[2], 'output datum hash')) };
	} else {
		const f = fields(i, 'output');
		addr = bytes(f.get(0), 'output address');
		val = f.get(1);
		const d = f.get(2);
		if (d) {
			const [kind, body] = arr(d, 'output datum');
			if (uint(kind, 'output datum kind') === 0n) datum = { kind: 'hash', hash: hex(bytes(body, 'output datum hash')) };
			else {
				const inline = body.type === 'tag' && body.tag === 24 ? bytes(body.item, 'inline datum') : fail('inline datum: expected tag 24');
				datum = { kind: 'inline', cbor: hex(inline), hash: hash256(inline) };
			}
		}
		if (f.get(3)) ref = scriptRef(f.get(3)!);
	}
	return { address: addressText(addr), addressHex: hex(addr), value: value(val), datum, scriptRef: ref };
}

const refs = (i: CborItem | undefined, what: string): TxRef[] =>
	i ? arr(i, what).map((x) => {
		const [h, ix] = arr(x, what);
		return { txHash: hex(bytes(h, `${what} tx id`)), index: Number(uint(ix, `${what} index`)) };
	}) : [];

// --- certificates, votes, proposals ---

function credential(i: CborItem, what: string): Credential {
	const [kind, h] = arr(i, what);
	return { script: uint(kind, what) === 1n, hash: hex(bytes(h, what)) };
}
function drep(i: CborItem): DRep {
	const [kind, h] = arr(i, 'drep');
	const k = Number(uint(kind, 'drep'));
	return k === 0 ? { kind: 'key', hash: hex(bytes(h, 'drep')) } : k === 1 ? { kind: 'script', hash: hex(bytes(h, 'drep')) } : { kind: k === 2 ? 'abstain' : 'noConfidence' };
}
function anchor(i: CborItem | undefined): Anchor | null {
	if (!i || i.type === 'simple') return null;
	const [url, h] = arr(i, 'anchor');
	return { url: url.type === 'text' ? url.value : '', hash: hex(bytes(h, 'anchor hash')) };
}

function certificate(i: CborItem, index: number): Certificate {
	const c = arr(i, 'certificate');
	const t = Number(uint(c[0], 'certificate type'));
	const base = { index, item: i, credential: null as Credential | null };
	const cred = () => credential(c[1], 'certificate credential');
	switch (t) {
		case 0:
			return { ...base, kind: 'stakeRegistration', credential: cred() };
		case 1:
			return { ...base, kind: 'stakeDeregistration', credential: cred() };
		case 2:
			return { ...base, kind: 'stakeDelegation', credential: cred(), poolId: hex(bytes(c[2], 'pool id')) };
		case 3:
			return { ...base, kind: 'poolRegistration', poolId: hex(bytes(c[1], 'pool id')) };
		case 4:
			return { ...base, kind: 'poolRetirement', poolId: hex(bytes(c[1], 'pool id')), epoch: uint(c[2], 'epoch') };
		case 5:
			return { ...base, kind: 'genesisDelegation' };
		case 6:
			return { ...base, kind: 'moveInstantaneousRewards' };
		case 7:
			return { ...base, kind: 'registration', credential: cred(), deposit: uint(c[2], 'deposit') };
		case 8:
			return { ...base, kind: 'unregistration', credential: cred(), deposit: uint(c[2], 'deposit') };
		case 9:
			return { ...base, kind: 'voteDelegation', credential: cred(), drep: drep(c[2]) };
		case 10:
			return { ...base, kind: 'stakeVoteDelegation', credential: cred(), poolId: hex(bytes(c[2], 'pool id')), drep: drep(c[3]) };
		case 11:
			return { ...base, kind: 'stakeRegDelegation', credential: cred(), poolId: hex(bytes(c[2], 'pool id')), deposit: uint(c[3], 'deposit') };
		case 12:
			return { ...base, kind: 'voteRegDelegation', credential: cred(), drep: drep(c[2]), deposit: uint(c[3], 'deposit') };
		case 13:
			return { ...base, kind: 'stakeVoteRegDelegation', credential: cred(), poolId: hex(bytes(c[2], 'pool id')), drep: drep(c[3]), deposit: uint(c[4], 'deposit') };
		case 14:
			return { ...base, kind: 'authCommitteeHot', credential: cred(), hotCredential: credential(c[2], 'hot credential') };
		case 15:
			return { ...base, kind: 'resignCommitteeCold', credential: cred(), anchor: anchor(c[2]) };
		case 16:
			return { ...base, kind: 'drepRegistration', credential: cred(), deposit: uint(c[2], 'deposit'), anchor: anchor(c[3]) };
		case 17:
			return { ...base, kind: 'drepUnregistration', credential: cred(), deposit: uint(c[2], 'deposit') };
		case 18:
			return { ...base, kind: 'drepUpdate', credential: cred(), anchor: anchor(c[2]) };
		default:
			return { ...base, kind: 'unknown' };
	}
}

const VOTERS: VoterKind[] = ['committeeKey', 'committeeScript', 'drepKey', 'drepScript', 'pool'];
const VOTES = ['no', 'yes', 'abstain'] as const;

function votes(i: CborItem | undefined): Vote[] {
	if (!i) return [];
	const out: Vote[] = [];
	for (const v of map(i, 'votes')) {
		const [kind, h] = arr(v.k, 'voter');
		const voter = { kind: VOTERS[Number(uint(kind, 'voter type'))] ?? fail('voter: unknown type'), hash: hex(bytes(h, 'voter')) };
		for (const a of map(v.v, 'voting procedures')) {
			const [tx, ix] = arr(a.k, 'gov action id');
			const [vote, anc] = arr(a.v, 'voting procedure');
			out.push({ voter, action: { txHash: hex(bytes(tx, 'gov action tx')), index: Number(uint(ix, 'gov action index')) }, vote: VOTES[Number(uint(vote, 'vote'))] ?? fail('vote: unknown value'), anchor: anchor(anc) });
		}
	}
	return out;
}

const ACTIONS: GovActionKind[] = ['parameterChange', 'hardFork', 'treasuryWithdrawals', 'noConfidence', 'updateCommittee', 'newConstitution', 'info'];

function proposals(i: CborItem | undefined): Proposal[] {
	if (!i) return [];
	return arr(i, 'proposals').map((p) => {
		const [deposit, account, action, anc] = arr(p, 'proposal');
		const a = arr(action, 'gov action');
		const kind = ACTIONS[Number(uint(a[0], 'gov action type'))] ?? fail('gov action: unknown type');
		const policyItem = kind === 'parameterChange' ? a[3] : kind === 'treasuryWithdrawals' ? a[2] : undefined;
		const acct = bytes(account, 'reward account');
		return {
			deposit: uint(deposit, 'deposit'),
			rewardAccount: hex(acct),
			returnAddress: addressText(acct),
			kind,
			policy: policyItem?.type === 'bytes' ? hex(policyItem.value) : null,
			action,
			anchor: anchor(anc)
		};
	});
}

// --- witnesses ---

/** Redeemers: a list [tag, index, data, ex units] (Alonzo, Babbage) or a map {[tag, index]: [data, ex units]} (Conway). */
function redeemers(all: Uint8Array, i: CborItem | undefined): Redeemer[] {
	if (!i) return [];
	const one = (tag: CborItem, index: CborItem, data: CborItem, units: CborItem): Redeemer => {
		const [mem, steps] = arr(units, 'ex units');
		return { tag: TAGS[Number(uint(tag, 'redeemer tag'))] ?? fail('redeemer: unknown tag'), index: Number(uint(index, 'redeemer index')), data: hex(rawOf(all, data)), exUnits: { mem: uint(mem, 'mem'), steps: uint(steps, 'steps') } };
	};
	const x = untag(i);
	if (x?.type === 'map')
		return x.entries.map((e) => {
			const [tag, index] = arr(e.k, 'redeemer key');
			const [data, units] = arr(e.v, 'redeemer value');
			return one(tag, index, data, units);
		});
	return arr(x, 'redeemers').map((r) => {
		const [tag, index, data, units] = arr(r, 'redeemer');
		return one(tag, index, data, units);
	});
}

function witnesses(all: Uint8Array, i: CborItem | undefined): DecodedTx['witnesses'] {
	const f = i ? fields(i, 'witness set') : new Map<number, CborItem>();
	const list = (k: number) => (f.get(k) ? arr(f.get(k), `witness ${k}`) : []);
	const scripts: Script[] = [
		...list(1).map((s) => script('native', rawOf(all, s))),
		...list(3).map((s) => script('V1', bytes(s, 'plutus v1 script'))),
		...list(6).map((s) => script('V2', bytes(s, 'plutus v2 script'))),
		...list(7).map((s) => script('V3', bytes(s, 'plutus v3 script')))
	];
	return {
		vkeys: list(0).map((w) => {
			const [vkey, sig] = arr(w, 'vkey witness');
			const k = bytes(vkey, 'vkey');
			return { vkey: hex(k), signature: hex(bytes(sig, 'signature')), keyHash: hash224(k) };
		}),
		bootstrap: list(2).length,
		scripts,
		datums: list(4).map((d) => {
			const raw = rawOf(all, d);
			return { hash: hash256(raw), cbor: hex(raw) };
		}),
		redeemers: redeemers(all, f.get(5))
	};
}

function metadataOf(i: CborItem | undefined): CborItem | null {
	if (!i || i.type === 'simple') return null;
	if (i.type === 'map') return i; // Shelley: the metadata map itself
	if (i.type === 'array') return i.items[0] ?? null; // Shelley-MA: [metadata, scripts]
	if (i.type === 'tag' && i.tag === 259) return fields(i.item, 'auxiliary data').get(0) ?? null; // Alonzo: {0: metadata, …}
	return null;
}

/** Decode a transaction from CBOR (hex or bytes). Throws with a readable message when it isn't one. */
export function decodeTransaction(input: string | Uint8Array): DecodedTx {
	const all = typeof input === 'string' ? new Uint8Array((input.trim().toLowerCase().match(/../g) ?? []).map((h) => parseInt(h, 16))) : input;
	if (typeof input === 'string' && !/^([0-9a-f]{2})+$/i.test(input.trim())) fail('The transaction must be CBOR hex.');
	const { item: root, trailing } = decodeCbor(all);
	if (trailing) fail(`${trailing} unexpected bytes after the transaction.`);
	const parts = root.type === 'array' ? root.items : fail('A transaction is a CBOR array [body, witnesses, …].');
	if (parts.length < 3) fail('A transaction has a body, witnesses and validity (or auxiliary data).');
	{
		const bodyItem = parts[0];
		const body = fields(bodyItem, 'transaction body');
		// Mary-era transactions are [body, witnesses, auxiliary data]; Alonzo on add is_valid as the third element.
		const hasValidity = parts.length === 4;
		const isValid = hasValidity ? parts[2].type === 'simple' && parts[2].value === true : true;
		const aux = hasValidity ? parts[3] : parts[2];
		const opt = (k: number, f: (i: CborItem) => bigint) => (body.get(k) ? f(body.get(k)!) : null);
		return {
			hash: hash256(rawOf(all, bodyItem)),
			size: all.length,
			isValid,
			inputs: refs(body.get(0), 'inputs'),
			outputs: body.get(1) ? arr(body.get(1), 'outputs').map(output) : [],
			fee: uint(body.get(2), 'fee'),
			ttl: opt(3, (i) => uint(i, 'ttl')),
			certificates: body.get(4) ? arr(body.get(4), 'certificates').map(certificate) : [],
			withdrawals: body.get(5)
				? map(body.get(5), 'withdrawals').map((w) => {
						const b = bytes(w.k, 'reward account');
						return { rewardAccount: hex(b), ...rewardAccount(b), amount: uint(w.v, 'withdrawal') };
					})
				: [],
			auxiliaryDataHash: body.get(7) ? hex(bytes(body.get(7), 'auxiliary data hash')) : null,
			validityStart: opt(8, (i) => uint(i, 'validity start')),
			mint: body.get(9) ? multiAsset(body.get(9)!, 'mint', true) : [],
			scriptDataHash: body.get(11) ? hex(bytes(body.get(11), 'script data hash')) : null,
			collateral: refs(body.get(13), 'collateral'),
			requiredSigners: body.get(14) ? arr(body.get(14), 'required signers').map((s) => hex(bytes(s, 'required signer'))) : [],
			networkId: body.get(15) ? Number(uint(body.get(15), 'network id')) : null,
			collateralReturn: body.get(16) ? output(body.get(16)!) : null,
			totalCollateral: opt(17, (i) => uint(i, 'total collateral')),
			referenceInputs: refs(body.get(18), 'reference inputs'),
			votes: votes(body.get(19)),
			proposals: proposals(body.get(20)),
			currentTreasury: opt(21, (i) => uint(i, 'treasury')),
			donation: opt(22, (i) => uint(i, 'donation')),
			witnesses: witnesses(all, parts[1]),
			metadata: metadataOf(aux),
			root,
			bytes: all
		};
	}
}

export { TxDecodeError };
