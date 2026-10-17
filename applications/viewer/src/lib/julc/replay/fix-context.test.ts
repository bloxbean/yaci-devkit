import { describe, expect, it } from 'vitest';
import { decodePlutusData, encodePlutusData, type PlutusData } from '$lib/cbor';
import { fixContext, ownRedeemer } from './fix-context';

const b = (hex: string): PlutusData => ({ bytes: hex });
const i = (n: number): PlutusData => ({ int: String(n) });
const c = (constructor: number, ...fields: PlutusData[]): PlutusData => ({ constructor, fields });
const map = (...entries: [PlutusData, PlutusData][]): PlutusData => ({ map: entries.map(([k, v]) => ({ k, v })) });
const list = (...items: PlutusData[]): PlutusData => ({ list: items });

const P1 = 'aa'.repeat(28);
const P2 = '11'.repeat(28);
// Policy P1 before P2: out of ledger order. Token names out of order too.
const unsorted = () => map([b(''), map([b(''), i(5)])], [b(P1), map([b('02'), i(1)], [b('01'), i(2)])], [b(P2), map([b('00'), i(3)])]);
const sorted = map([b(''), map([b(''), i(5)])], [b(P2), map([b('00'), i(3)])], [b(P1), map([b('01'), i(2)], [b('02'), i(1)])]);

const ownEntry = { k: c(1, c(0, b('ab'.repeat(32)), i(0))), v: c(0) };

/** A V3-shaped context: TxInfo with 16 fields, one input with a value and an inline datum. */
function v3Context(): string {
	const txOut = (value: PlutusData) => c(0, c(0, c(1, b('cc'.repeat(28))), c(1)), value, c(2, i(7)), c(1));
	const info = [
		list(c(0, c(0, b('ab'.repeat(32)), i(0)), txOut(unsorted()))), // inputs
		list(), // reference inputs
		list(txOut(unsorted())), // outputs
		i(200000), // fee (integer in V3)
		unsorted(), // mint
		list(),
		map(),
		c(0),
		list(),
		map([ownEntry.k, ownEntry.v]), // redeemers
		map(), // data
		b('ef'.repeat(32)),
		map(),
		list(),
		c(1),
		c(1)
	];
	return encodePlutusData(c(0, c(0, ...info), c(0), c(1)));
}

const infoOf = (cbor: string) => (decodePlutusData(cbor) as { fields: { fields: PlutusData[] }[] }).fields[0].fields;

describe('fixContext', () => {
	it('puts values in ledger order without touching datums', () => {
		const fixed = fixContext(v3Context(), 'V3', null)!;
		expect(fixed.sortedValues).toBe(true);
		const info = infoOf(fixed.cbor);
		const inputOut = (info[0] as { list: { fields: { fields: PlutusData[] }[] }[] }).list[0].fields[1].fields;
		expect(inputOut[1]).toEqual(sorted);
		expect(inputOut[2]).toEqual(c(2, i(7)));
		expect((info[2] as { list: { fields: PlutusData[] }[] }).list[0].fields[1]).toEqual(sorted);
		expect(info[4]).toEqual(sorted);
		expect(info[3]).toEqual(i(200000));
	});

	it('returns null when nothing needs changing', () => {
		const once = fixContext(v3Context(), 'V3', null)!.cbor;
		expect(fixContext(once, 'V3', null)).toBeNull();
	});

	it('reads the own redeemer and completes the redeemer map', () => {
		const ctx = v3Context();
		expect(ownRedeemer(ctx)).toEqual(ownEntry);
		const other = { k: c(2, c(1, b('dd'.repeat(28)))), v: i(1) };
		const fixed = fixContext(ctx, 'V3', [ownEntry, other])!;
		expect(fixed.completedRedeemers).toBe(true);
		expect(infoOf(fixed.cbor)[9]).toEqual({ map: [ownEntry, other] });
	});

	it('restores datums given by hash and adds witnessed datums in hash order', () => {
		const hashA = '99'.repeat(32);
		const hashB = '01'.repeat(32);
		const fixed = fixContext(v3Context(), 'V3', null, [{ input: 0, hash: hashA, datum: '07' }], [{ hash: hashB, datum: 'd87980' }, { hash: hashA, datum: '07' }])!;
		expect(fixed.restoredDatums).toBe(true);
		const info = infoOf(fixed.cbor);
		const inputOut = (info[0] as { list: { fields: { fields: PlutusData[] }[] }[] }).list[0].fields[1].fields;
		expect(inputOut[2]).toEqual(c(1, b(hashA)));
		expect(info[10]).toEqual(map([b(hashB), c(0)], [b(hashA), i(7)]));
	});
});

describe('fixContext: proposals', () => {
	// julc's placeholder: a parameter change without changes.
	const placeholder = () => c(0, i(100), c(0, b('aa'.repeat(28))), c(0, c(1), map(), c(0, b('fa'.repeat(28)))));
	const real = c(0, i(100), c(0, b('aa'.repeat(28))), c(0, c(1), map([i(11), list(i(1), i(10))]), c(0, b('fa'.repeat(28)))));
	function proposingContext(): string {
		const info: PlutusData[] = Array.from({ length: 16 }, () => list());
		info[3] = i(0);
		info[9] = map([c(5, i(0), placeholder()), c(0)]);
		info[13] = list(placeholder());
		return encodePlutusData(c(0, c(0, ...info), c(0), c(5, i(0), placeholder())));
	}

	it('puts the real procedure into the proposal list, the redeemer purpose and the ScriptInfo', () => {
		const fixed = fixContext(proposingContext(), 'V3', null, [], [], [encodePlutusData(real)])!;
		expect(fixed.restoredProposals).toBe(true);
		const ctx = decodePlutusData(fixed.cbor) as { fields: PlutusData[] };
		const info = (ctx.fields[0] as { fields: PlutusData[] }).fields;
		expect(info[13]).toEqual(list(real));
		expect((info[9] as { map: { k: PlutusData }[] }).map[0].k).toEqual(c(5, i(0), real));
		expect(ctx.fields[2]).toEqual(c(5, i(0), real));
	});

	it('leaves V2 contexts and missing proposals alone', () => {
		expect(fixContext(proposingContext(), 'V3', null, [], [], [])).toBeNull();
		expect(fixContext(proposingContext(), 'V3', null, [], [], [encodePlutusData(real), encodePlutusData(real)])?.cbor).toBe(encodePlutusData((() => {
			const d = decodePlutusData(proposingContext()) as { fields: PlutusData[] };
			const info = (d.fields[0] as { fields: PlutusData[] }).fields;
			(info[9] as { map: { k: PlutusData }[] }).map[0].k = c(5, i(0), real);
			d.fields[2] = c(5, i(0), real);
			return d as PlutusData;
		})()));
	});
});
