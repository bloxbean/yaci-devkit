import { describe, expect, it } from 'vitest';
import { decodePlutusData, tryDecodePlutusData } from './cbor';

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
