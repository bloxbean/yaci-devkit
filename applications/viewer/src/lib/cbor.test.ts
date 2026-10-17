import { describe, expect, it } from 'vitest';
import { decodePlutusData, encodePlutusData, tryDecodePlutusData, type PlutusData } from './cbor';

describe('decodePlutusData', () => {
	it('decodes constructors', () => {
		expect(decodePlutusData('d87980')).toEqual({ constructor: 0, fields: [] });
		expect(decodePlutusData('d87a80')).toEqual({ constructor: 1, fields: [] });
		// Constr 7 via tag 1280
		expect(decodePlutusData('d9050080')).toEqual({ constructor: 7, fields: [] });
	});

	it('decodes ints, bytes, lists and maps', () => {
		// Constr 0 [42, h'cafe', [1, -2], {1: 2}] using an indefinite list for the fields
		expect(decodePlutusData('d8799f182a42cafe820121a10102ff')).toEqual({
			constructor: 0,
			fields: [{ int: '42' }, { bytes: 'cafe' }, { list: [{ int: '1' }, { int: '-2' }] }, { map: [{ k: { int: '1' }, v: { int: '2' } }] }]
		});
	});

	it('decodes big integers and chunked bytes', () => {
		expect(decodePlutusData('c249010000000000000000')).toEqual({ int: '18446744073709551616' });
		expect(decodePlutusData('5f4201024103ff')).toEqual({ bytes: '010203' });
	});

	it('returns null for things that are not Plutus data', () => {
		expect(tryDecodePlutusData('zz')).toBeNull();
		expect(tryDecodePlutusData('60')).toBeNull(); // text string
		expect(tryDecodePlutusData(null)).toBeNull();
	});
});

describe('encodePlutusData', () => {
	it('encodes the way the Plutus libraries do', () => {
		expect(encodePlutusData({ constructor: 0, fields: [] })).toBe('d87980');
		expect(encodePlutusData({ constructor: 0, fields: [{ int: '42' }, { bytes: 'cafe' }] })).toBe('d8799f182a42cafeff');
		expect(encodePlutusData({ constructor: 7, fields: [] })).toBe('d9050080');
		expect(encodePlutusData({ constructor: 200, fields: [] })).toBe('d8668218c880');
		expect(encodePlutusData({ int: '-1' })).toBe('20');
		expect(encodePlutusData({ map: [{ k: { int: '1' }, v: { int: '2' } }] })).toBe('a10102');
		expect(encodePlutusData({ int: (1n << 64n).toString() })).toBe('c249010000000000000000');
		expect(encodePlutusData({ int: (-(1n << 64n) - 1n).toString() })).toBe('c349010000000000000000');
		expect(encodePlutusData({ int: (-(1n << 64n)).toString() })).toBe('3bffffffffffffffff');
	});

	it('round-trips through the decoder, including chunked bytes', () => {
		const samples: PlutusData[] = [
			{ bytes: 'ab'.repeat(100) },
			{ constructor: 2, fields: [{ list: [{ map: [] }, { int: '18446744073709551615' }, { int: '-18446744073709551617' }] }] },
			{ map: [{ k: { bytes: '' }, v: { constructor: 130, fields: [{ int: '0' }] } }, { k: { bytes: '' }, v: { list: [] } }] }
		];
		for (const d of samples) expect(decodePlutusData(encodePlutusData(d))).toEqual(d);
		const long = encodePlutusData({ bytes: 'ab'.repeat(100) });
		expect(long.startsWith('5f5840')).toBe(true);
		expect(long.endsWith('ff')).toBe(true);
	});
});
