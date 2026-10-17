import { describe, expect, it } from 'vitest';
import { bech32Encode } from '$lib/bech32';
import { buildRuns, type ReplayInput, type StoreCert } from './build';
import { slotConfigFor, slotToPosixMs } from './slots';
import withdrawZero from './fixtures/mainnet-withdrawZero.json';
import v3 from './fixtures/mainnet-v3.json';
import mint from './fixtures/mainnet-mint.json';
import proposal from './fixtures/mainnet-proposal.json';
import { decodePlutusData, type PlutusData } from '$lib/cbor';

/*
 * Fixtures are Mainnet transactions captured from Yaci Store with the same requests as load.ts (script bodies
 * trimmed). They were re-run in the browser with julc: withdrawZero's two spend redeemers reproduce their on-chain
 * budgets exactly; its reward redeemer fails because the context lacks the other redeemers (julc#244).
 */

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function inputOf(f: any): ReplayInput {
	const certificates: StoreCert[] = [
		...f.stakes.map((s: { cert_index: number; address: string; registration: boolean }) => ({ cert_index: s.cert_index, type: s.registration ? 'registration' : 'deregistration', address: s.address })),
		...f.delegations.map((d: { cert_index: number; address: string; pool_id: string }) => ({ cert_index: d.cert_index, type: 'delegation', address: d.address, pool_id: d.pool_id }))
	];
	return { hash: f.hash, tx: f.tx, scripts: f.scripts, withdrawals: f.withdrawals, mints: f.mints, votes: f.votes, certificates, datums: {}, params: f.params, slots: slotConfigFor(f.genesis.network_magic, f.genesis) };
}

const stakeAddr = (header: number, fill: number) => bech32Encode('stake', new Uint8Array([header, ...Array(28).fill(fill)]));

describe('slots', () => {
	it('uses Shelley constants for public networks and genesis for devnets', () => {
		const mainnet = slotConfigFor(764824073, { system_start: 1506203091, slot_length: 1 })!;
		expect(slotToPosixMs(4492800, mainnet)).toBe(1596059091000n);
		expect(slotToPosixMs(200642780, mainnet)).toBe(1596059091000n + (200642780n - 4492800n) * 1000n);
		const devnet = slotConfigFor(42, { network_magic: 42, system_start: 1792193424, slot_length: 0.2 })!;
		expect(slotToPosixMs(0, devnet)).toBe(1792193424000n);
		expect(slotToPosixMs(10, devnet)).toBe(1792193426000n);
		expect(slotConfigFor(42, null)).toBeNull();
	});
});

describe('buildRuns', () => {
	it('rebuilds a withdraw-zero transaction (Plutus V2) in ledger order', () => {
		const runs = buildRuns(inputOf(withdrawZero));
		expect(runs.map((r) => r.key).sort()).toEqual(['reward#0', 'spend#0', 'spend#1']);
		for (const r of runs) expect(r.skip).toBeUndefined();

		const spend = runs.find((r) => r.key === 'spend#0')!;
		const tx = spend.request!.transaction;
		expect(spend.language).toBe('V2');
		expect(spend.declared).toEqual({ mem: 77308n, steps: 23525984n });
		expect(tx.purpose).toEqual({ type: 'spend', index: 0 });
		expect(tx.txId).toBe(withdrawZero.hash);
		const refs = tx.inputs.map((i) => `${i.txId}#${i.index}`);
		expect(refs).toEqual([...refs].sort());
		expect(tx.inputs[0].datum.kind).toBe('inline');
		expect(tx.validRange).toEqual({
			from: 1596059091000n + (BigInt(withdrawZero.tx.validity_interval_start) - 4492800n) * 1000n,
			fromInclusive: true,
			to: 1596059091000n + (BigInt(withdrawZero.tx.ttl) - 4492800n) * 1000n,
			toInclusive: false
		});
		expect(tx.signatories).toEqual(withdrawZero.tx.required_signers);
		expect(tx.withdrawals).toEqual([{ credential: withdrawZero.withdrawals[0].address, amount: '0' }]);

		expect(spend.request!.target).toEqual({ language: 'PlutusV2', protocol: 11 });
		expect(spend.request!.costModel!.parameters.length).toBe(withdrawZero.params.cost_models_raw.PlutusV2.length);
		expect(spend.notes.join(' ')).toMatch(/julc#244/);

		const reward = runs.find((r) => r.key === 'reward#0')!;
		expect(reward.request!.transaction.purpose).toEqual({ type: 'reward', target: 'script:1eae96baf29e27682ea3f815aba361a0c6059d45e4bfbe95bbd2f44a' });
	});

	it('rebuilds Plutus V3 runs', () => {
		const runs = buildRuns(inputOf(v3));
		expect(runs.length).toBe(2);
		for (const r of runs) {
			expect(r.language).toBe('V3');
			expect(r.request?.target.language).toBe('PlutusV3');
		}
	});

	it('targets the minting policy and sorts the mint', () => {
		const runs = buildRuns(inputOf(mint));
		const m = runs.find((r) => r.purpose === 'mint')!;
		expect(m.request!.transaction.purpose).toEqual({ type: 'mint', target: m.scriptHash });
		expect(m.request!.transaction.mint).toEqual([{ policyId: mint.mints[0].unit.slice(0, 56), tokenName: mint.mints[0].unit.slice(56), quantity: String(mint.mints[0].quantity) }]);
	});

	it('orders withdrawals as the ledger does: script credentials first', () => {
		const key = stakeAddr(0xe1, 0x00);
		const script = stakeAddr(0xf1, 0xff);
		const input = inputOf(withdrawZero);
		input.withdrawals = [
			{ address: key, amount: 1 },
			{ address: script, amount: 2 }
		];
		const tx = buildRuns(input)[0].request!.transaction;
		expect(tx.withdrawals.map((w) => w.credential)).toEqual([script, key]);
	});

	it('explains runs it cannot rebuild', () => {
		const input = inputOf(withdrawZero);
		const byron = { ...input, tx: { ...input.tx, outputs: [{ ...input.tx.outputs[0], address: 'DdzFFzCqrht4PWfBGtmrQz4x1GkZHYLVGbK7aaBkjWxujxzz3L5GxCgPiTsks5RvMN1Bo8Va6V9C6Rm2vkGQhJmq8dRLPMtJBXCjCC8' }] } };
		expect(buildRuns(byron)[0].skip).toMatch(/Byron/);
		expect(buildRuns({ ...input, slots: null })[0].skip).toMatch(/Slot times/);
		const propose = { ...input, scripts: [{ ...input.scripts[0], redeemer: { tag: 'propose', index: 0, data: 'd87980', ex_units: { mem: 1, steps: 1 } } }] };
		expect(buildRuns(propose)[0].skip).toMatch(/proposal #0/);
		const cert = { ...input, scripts: [{ ...input.scripts[0], redeemer: { tag: 'cert', index: 0, data: 'd87980', ex_units: { mem: 1, steps: 1 } } }] };
		expect(buildRuns(cert)[0].skip).toMatch(/certificate/);
	});

	it('normalises purpose names and infers V3 for proposals', () => {
		const input = inputOf(withdrawZero);
		input.scripts = [{ script_hash: 'ab'.repeat(28), type: null, script_content: null, redeemer: { tag: 'proposing', index: 0, data: 'd87980', ex_units: { mem: 1, steps: 1 } } }];
		const [run] = buildRuns(input);
		expect(run.key).toBe('propose#0');
		expect(run.language).toBe('V3');
		expect(run.skip).toMatch(/proposal #0/);
	});

	it('treats a 0 TTL and validity start (how Yaci Store reports none) as no bound', () => {
		const input = inputOf(withdrawZero);
		input.tx = { ...input.tx, ttl: 0, validity_interval_start: 0 };
		input.slots = null;
		const [run] = buildRuns(input);
		expect(run.skip).toBeUndefined();
		expect(run.request!.transaction.validRange).toEqual({ from: null, fromInclusive: true, to: null, toInclusive: false });
	});

	it('maps votes in ledger voter order and targets the voting script', () => {
		const input = inputOf(v3);
		input.votes = [
			{ voter_type: 'DREP_KEY_HASH', voter_hash: 'aa'.repeat(28), gov_action_tx_hash: '11'.repeat(32), gov_action_index: 0, vote: 'NO' },
			{ voter_type: 'DREP_SCRIPT_HASH', voter_hash: input.scripts[0].script_hash, gov_action_tx_hash: '11'.repeat(32), gov_action_index: 0, vote: 'YES' }
		];
		input.scripts = [{ ...input.scripts[0], redeemer: { tag: 'vote', index: 0, data: 'd87980', ex_units: { mem: 1, steps: 1 } } }];
		const [run] = buildRuns(input);
		const tx = run.request!.transaction;
		expect(tx.votes.map((v) => v.voter)).toEqual([`script:${input.scripts[0].script_hash}`, `key:${'aa'.repeat(28)}`]);
		expect(tx.purpose).toEqual({ type: 'vote', target: `script:${input.scripts[0].script_hash}`, voterType: 'drep' });
	});
});

describe('buildRuns: proposals', () => {
	// gov_action14dr5yg75pchr2sz42djtuflpvx5qnsek29qg7s7cft8lzrqt5vrqqtqntpk: guardrail run reproduced exactly in the browser.
	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	const f = proposal as any;
	const input = (proposals = f.proposals): ReplayInput => ({
		...inputOf(f),
		// The store has no script hash or body for proposing runs; load.ts takes the hash from the proposal and fetches the body.
		scripts: f.scripts.map((s: object) => ({ ...s, script_hash: f.proposals[0].details.policyHash, type: 'plutusV3', script_content: JSON.stringify({ content: '4e4d01000033222220051200120011' }) })),
		proposals
	});

	it('rebuilds a proposing run with the real governance action for the context fix', () => {
		const [run] = buildRuns(input());
		expect(run.key).toBe('propose#0');
		expect(run.language).toBe('V3');
		expect(run.scriptHash).toBe('fa24fb305126805cf2164c161d852a0e7330cf988f1fe558cf7d4a64');
		expect(run.request!.transaction.purpose).toEqual({ type: 'propose', index: 0 });
		expect(run.request!.transaction.proposals[0]).toMatchObject({ deposit: '100000000000', actionType: 'parameterChange', guardrail: run.scriptHash });
		const procedure = decodePlutusData(run.proposals[0]) as { fields: PlutusData[] };
		expect(procedure.fields[0]).toEqual({ int: '100000000000' });
		const action = procedure.fields[2] as { constructor: number; fields: PlutusData[] };
		expect(action.constructor).toBe(0);
		expect((action.fields[1] as { map: { k: { int: string } }[] }).map.map((e) => e.k.int)).toEqual(['16', '20', '21']);
	});

	it('skips a proposing run when the store has no proposals', () => {
		expect(buildRuns(input([]))[0].skip).toMatch(/proposal #0/);
	});
});
