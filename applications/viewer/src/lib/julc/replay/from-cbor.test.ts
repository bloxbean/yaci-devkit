import { describe, expect, it } from 'vitest';
import { decodeTransaction } from '$lib/tx/decode';
import { buildRuns, type ReplayInput, type StoreCert } from './build';
import { replayInputFromCbor } from './from-cbor';
import { slotConfigFor } from './slots';
import cborFx from './fixtures/mainnet-tx-cbor.json';
import withdrawZero from './fixtures/mainnet-withdrawZero.json';
import v3 from './fixtures/mainnet-v3.json';
import mint from './fixtures/mainnet-mint.json';
import proposal from './fixtures/mainnet-proposal.json';

/*
 * The same Mainnet transactions replayed two ways: from their CBOR (fixtures/mainnet-tx-cbor.json) and from Yaci
 * Store's JSON (the other fixtures). Both must describe the same runs and the same transaction to julc.
 */

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function storeInput(f: any): ReplayInput {
	const certificates: StoreCert[] = [
		...(f.stakes ?? []).map((s: { cert_index: number; address: string; registration: boolean }) => ({ cert_index: s.cert_index, type: s.registration ? 'registration' : 'deregistration', address: s.address })),
		...(f.delegations ?? []).map((d: { cert_index: number; address: string; pool_id: string }) => ({ cert_index: d.cert_index, type: 'delegation', address: d.address, pool_id: d.pool_id }))
	];
	// As load.ts does: a proposing run's script is the guardrail its proposal names, its body fetched by hash (taken
	// from the transaction's witnesses here).
	const witnessed = new Map(decodeTransaction((cborFx.txs as Record<string, { cbor: string }>)[f.hash].cbor).witnesses.scripts.map((s) => [s.hash, s]));
	const scripts = f.scripts.map((s: { script_hash: string | null; script_content: string | null; type: string | null; redeemer: { tag: string; index: number } }) => {
		const guardrail = !s.script_hash && /^propos/.test(s.redeemer.tag) ? f.proposals?.[s.redeemer.index]?.details?.policyHash : null;
		const hash = s.script_hash ?? guardrail;
		const w = hash ? witnessed.get(hash) : null;
		return { ...s, script_hash: hash, type: s.type ?? (w ? `plutus${w.language}` : null), script_content: s.script_content ?? (w ? JSON.stringify({ content: w.cbor }) : null) };
	});
	return { hash: f.hash, tx: f.tx, scripts, withdrawals: f.withdrawals ?? [], mints: f.mints ?? [], votes: f.votes ?? [], certificates, proposals: f.proposals ?? [], datums: {}, params: f.params, slots: slotConfigFor(f.genesis.network_magic, f.genesis) };
}

function cborInput(store: ReplayInput): ReplayInput {
	const cbor = (cborFx.txs as Record<string, { cbor: string }>)[store.hash].cbor;
	const known = new Map([...store.tx.inputs, ...(store.tx.reference_inputs ?? [])].map((u) => [`${u.tx_hash}#${u.output_index}`, u]));
	const { input, missing } = replayInputFromCbor(decodeTransaction(cbor), { resolve: (r) => known.get(`${r.txHash}#${r.index}`) ?? null, params: store.params, slots: store.slots, mainnet: true });
	expect(missing).toEqual([]);
	// Reference scripts aren't in the transaction: the loader fetches their bodies by hash. Take them from the store here.
	const bodies = new Map(store.scripts.map((s) => [s.script_hash, s]));
	return { ...input, scripts: input.scripts.map((s) => (s.script_content ? s : { ...s, type: bodies.get(s.script_hash)?.type ?? null, script_content: bodies.get(s.script_hash)?.script_content ?? null })) };
}

const summary = (input: ReplayInput) =>
	buildRuns(input).map((p) => ({ key: p.key, scriptHash: p.scriptHash, language: p.language, redeemer: p.redeemer, declared: p.declared, datum: p.datum, skip: p.skip }));
const txOf = (input: ReplayInput, key: string) => {
	const t = buildRuns(input).find((p) => p.key === key)!.request!.transaction;
	return { inputs: t.inputs, referenceInputs: t.referenceInputs, outputs: t.outputs, mint: t.mint, withdrawals: t.withdrawals, validRange: t.validRange, signatories: t.signatories, fee: t.fee };
};

describe('replays from CBOR match replays from the store', () => {
	for (const [name, f] of Object.entries({ withdrawZero, v3, mint, proposal })) {
		it(`${name}: same runs and the same transaction`, () => {
			const store = storeInput(f);
			const cbor = cborInput(store);
			expect(summary(cbor)).toEqual(summary(store));
			const first = buildRuns(store).find((p) => p.request)!.key;
			expect(txOf(cbor, first)).toEqual(txOf(store, first));
			expect(buildRuns(cbor).every((p) => p.source === 'cbor')).toBe(true);
			expect(buildRuns(store).every((p) => p.source === 'store')).toBe(true);
		});
	}

	it('encodes a proposal’s governance action from CBOR exactly as from the store’s JSON', () => {
		const store = storeInput(proposal);
		const cbor = cborInput(store);
		const [fromStore] = buildRuns(store);
		const [fromCbor] = buildRuns(cbor);
		expect(fromCbor.proposals).toEqual(fromStore.proposals);
		expect(fromCbor.proposals[0]).toBeTruthy();
	});

	it('uses the witnessed datums exactly', () => {
		const cbor = (cborFx.txs as Record<string, { cbor: string }>)['a5554f4aa71cac02c10f6f523779ad210c5eb384b681789b15eb60014c48ad2e'].cbor;
		const tx = decodeTransaction(cbor);
		const { input, missing } = replayInputFromCbor(tx, { resolve: () => null, params: null, slots: null, mainnet: true });
		expect(missing).toHaveLength(2);
		expect(input.witnessDatums).toEqual([{ hash: tx.witnesses.datums[0].hash, datum: tx.witnesses.datums[0].cbor }]);
		expect(input.datums[tx.witnesses.datums[0].hash]).toBe(tx.witnesses.datums[0].cbor);
	});
});
