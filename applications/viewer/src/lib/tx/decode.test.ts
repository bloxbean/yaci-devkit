import { describe, expect, it } from 'vitest';
import fx from '../julc/replay/fixtures/mainnet-tx-cbor.json';
import { decodeCbor, hexToBytes } from './cbor-item';
import { decodeTransaction } from './decode';

/*
 * Mainnet transactions (fixtures/mainnet-tx-cbor.json, from Koios). Expected values were checked against Yaci Store:
 * redeemer purposes and ex-units, script hashes, and the datum hashes of the inputs the witnessed datums belong to.
 */
const txs = fx.txs as Record<string, { note: string; cbor: string }>;
const tx = (prefix: string) => {
	const [hash, { cbor }] = Object.entries(txs).find(([h]) => h.startsWith(prefix))!;
	return { hash, t: decodeTransaction(cbor) };
};

describe('decodeTransaction', () => {
	it('computes every fixture’s transaction id from its body bytes', () => {
		for (const [hash, { cbor }] of Object.entries(txs)) expect(decodeTransaction(cbor).hash).toBe(hash);
	});

	it('reads a Conway batcher transaction: reference inputs, withdraw-zero, map-form redeemers', () => {
		const { t } = tx('a678733d');
		expect(t.inputs).toHaveLength(3);
		expect(t.referenceInputs).toHaveLength(4);
		expect(t.withdrawals).toEqual([expect.objectContaining({ amount: 0n, credential: { script: true, hash: '1eae96baf29e27682ea3f815aba361a0c6059d45e4bfbe95bbd2f44a' } })]);
		expect(t.withdrawals[0].address).toMatch(/^stake1/);
		expect(t.witnesses.redeemers).toEqual([
			{ tag: 'spend', index: 1, data: 'd87980', exUnits: { mem: 25305n, steps: 8177555n } },
			expect.objectContaining({ tag: 'spend', index: 0 }),
			expect.objectContaining({ tag: 'reward', index: 0, exUnits: { mem: 1326013n, steps: 427333304n } })
		]);
		expect(t.isValid).toBe(true);
		expect(t.metadata?.type).toBe('map');
	});

	it('reads an Alonzo-era Plutus V1 transaction: legacy outputs, list-form redeemers, witnessed datums', () => {
		const { t } = tx('a5554f4a');
		expect(t.witnesses.scripts).toEqual([expect.objectContaining({ language: 'V1', hash: expect.stringMatching(/^a813c752/) })]);
		// The witnessed datum is the one the spent input carries (its data_hash in Yaci Store).
		expect(t.witnesses.datums.map((d) => d.hash.slice(0, 8))).toEqual(['4492a520']);
		expect(t.outputs[0].datum).toEqual({ kind: 'hash', hash: expect.stringMatching(/^4492a520/) });
		expect(t.outputs[0].address).toMatch(/^addr1w/);
		expect(t.witnesses.redeemers).toEqual([expect.objectContaining({ tag: 'spend', index: 1, exUnits: { mem: 1110270n, steps: 454438838n } })]);
	});

	it('reads governance: committee votes, proposals with their guardrail, and Conway certificates', () => {
		const vote = tx('12c22eb0').t;
		expect(vote.votes.map((v) => [v.voter.kind, v.vote])).toEqual([
			['committeeScript', 'yes'],
			['committeeScript', 'yes']
		]);
		expect(vote.witnesses.redeemers.map((r) => r.tag)).toEqual(['spend', 'vote']);
		const change = tx('ab474223').t;
		expect(change.proposals).toEqual([expect.objectContaining({ kind: 'parameterChange', policy: 'fa24fb305126805cf2164c161d852a0e7330cf988f1fe558cf7d4a64', deposit: 100000000000n })]);
		expect(change.witnesses.scripts[0].hash).toBe('fa24fb305126805cf2164c161d852a0e7330cf988f1fe558cf7d4a64');
		const treasury = tx('6eddf4f1').t;
		expect(treasury.proposals[0]).toMatchObject({ kind: 'treasuryWithdrawals', policy: 'fa24fb305126805cf2164c161d852a0e7330cf988f1fe558cf7d4a64' });
		expect(treasury.proposals[0].returnAddress).toMatch(/^stake1/);
		expect(treasury.certificates).toEqual([expect.objectContaining({ kind: 'registration', deposit: 2000000n })]);
	});

	it('reads mints with negative quantities and minting redeemers', () => {
		const { t } = tx('90692ee7');
		expect(t.mint).toHaveLength(1);
		expect(t.witnesses.redeemers.find((r) => r.tag === 'mint')).toEqual(expect.objectContaining({ index: 0 }));
		expect(t.witnesses.scripts.map((s) => s.language)).toEqual(['V1', 'V1', 'V1']);
	});

	it('decodes certificate kinds a script can witness', () => {
		const h = '11'.repeat(28);
		// A minimal body {0: [], 1: [], 2: 0, 4: certs}, no witnesses, valid, no auxiliary data. The certificates: vote
		// delegation to abstain, stake and vote delegation (no confidence), DRep registration without an anchor.
		const vd = `8309 8201581c${h} 8102`.replace(/ /g, '');
		const svd = `840a 8201581c${h} 581c${'22'.repeat(28)} 8103`.replace(/ /g, '');
		const reg = `8410 8201581c${h} 1a001e8480 f6`.replace(/ /g, '');
		const t = decodeTransaction(`84a400800180020004 83${vd}${svd}${reg} a0f5f6`.replace(/ /g, ''));
		expect(t.certificates.map((c) => [c.kind, c.credential])).toEqual([
			['voteDelegation', { script: true, hash: h }],
			['stakeVoteDelegation', { script: true, hash: h }],
			['drepRegistration', { script: true, hash: h }]
		]);
		expect(t.certificates[0].drep).toEqual({ kind: 'abstain' });
		expect(t.certificates[1]).toMatchObject({ poolId: '22'.repeat(28), drep: { kind: 'noConfidence' } });
		expect(t.certificates[2]).toMatchObject({ deposit: 2000000n, anchor: null });
	});

	it('explains input that isn’t a transaction', () => {
		expect(() => decodeTransaction('zz')).toThrow(/hex/);
		expect(() => decodeTransaction('d87980')).toThrow(/array/);
		expect(() => decodeTransaction('8100')).toThrow(/body/);
	});
});

describe('decodeCbor', () => {
	it('reads every major type with byte ranges', () => {
		const { item, trailing } = decodeCbor(hexToBytes('a3016161029f0102ff03d8184201ff'));
		expect(trailing).toBe(0);
		if (item.type !== 'map') throw new Error('map');
		expect(item.entries.map((e) => e.v.type)).toEqual(['text', 'array', 'tag']);
		expect(item.entries[1].v).toMatchObject({ type: 'array', indefinite: true, start: 5, end: 9 });
		expect(decodeCbor(hexToBytes('f93c00')).item).toMatchObject({ type: 'float', value: 1 });
		expect(decodeCbor(hexToBytes('3903e7')).item).toMatchObject({ type: 'nint', value: -1000n });
	});
});
