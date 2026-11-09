import { describe, expect, it } from 'vitest';
import { readFileSync } from 'node:fs';
import { decodePlutusData } from '$lib/cbor';
import { findValidator, parseBlueprint, scriptHashOf, typeData, typeNameOf, variantOf } from './blueprint';

// An Aiken 1.x style blueprint: one entry per handler, sharing a hash; schemas referencing definitions.
const json = {
	preamble: { title: 'acme/dex', version: '0.1.0', plutusVersion: 'v3' },
	validators: [
		{
			title: 'pool.pool.spend',
			hash: 'aa'.repeat(28),
			datum: { schema: { $ref: '#/definitions/pool~1PoolDatum' } },
			redeemer: { schema: { $ref: '#/definitions/pool~1Action' } }
		},
		{ title: 'pool.pool.mint', hash: 'aa'.repeat(28), redeemer: { schema: { $ref: '#/definitions/Int' } } },
		{ title: 'pool.pool.else', hash: 'aa'.repeat(28), redeemer: { schema: {} } }
	],
	definitions: {
		Int: { dataType: 'integer' },
		ByteArray: { dataType: 'bytes' },
		'pool/PoolDatum': {
			title: 'PoolDatum',
			anyOf: [{ title: 'PoolDatum', dataType: 'constructor', index: 0, fields: [{ title: 'owner', $ref: '#/definitions/ByteArray' }, { title: 'reserves', $ref: '#/definitions/List$Int' }] }]
		},
		'List$Int': { dataType: 'list', items: { $ref: '#/definitions/Int' } },
		'pool/Action': {
			title: 'Action',
			anyOf: [
				{ title: 'Swap', dataType: 'constructor', index: 0, fields: [{ title: 'min_out', $ref: '#/definitions/Int' }] },
				{ title: 'Close', dataType: 'constructor', index: 1, fields: [] }
			]
		}
	}
};

describe('blueprints', () => {
	it('parses validators with purposes and finds them by hash and purpose', () => {
		const bp = parseBlueprint(json);
		expect(bp.validators.map((v) => v.purpose)).toEqual(['spend', 'mint', 'else']);
		expect(findValidator([bp], 'AA'.repeat(28), 'mint')?.validator.title).toBe('pool.pool.mint');
		expect(findValidator([bp], 'aa'.repeat(28), 'reward')?.validator.title).toBe('pool.pool.spend');
		expect(findValidator([bp], 'bb'.repeat(28))).toBeNull();
		expect(() => parseBlueprint({ hello: 1 })).toThrow(/validators/);
	});

	it('computes a script hash from compiled code like the ledger', () => {
		const cbor = readFileSync(new URL('./fixture-c3e28c.cbor.txt', import.meta.url), 'utf8').trim();
		expect(scriptHashOf(cbor, 'v2')).toBe('c3e28c36c3447315ba5a56f33da6a6ddc1770a876a8d9f0cb3a97c4c');
		const bp = parseBlueprint({ preamble: { plutusVersion: 'v2' }, validators: [{ title: 'x.spend', compiledCode: cbor }] });
		expect(bp.validators[0].hash).toBe('c3e28c36c3447315ba5a56f33da6a6ddc1770a876a8d9f0cb3a97c4c');
	});

	it('names types, variants and fields', () => {
		const bp = parseBlueprint(json);
		const v = bp.validators[0];
		// PoolDatum { owner: #cafe, reserves: [1, 2] }
		const datum = typeData(decodePlutusData('d8799f42cafe9f0102ffff'), v.datum, bp.definitions);
		expect(datum).toMatchObject({ type: 'PoolDatum', variant: 'PoolDatum', kind: 'constructor' });
		expect(datum.children!.map((c) => [c.field, c.kind])).toEqual([
			['owner', 'bytes'],
			['reserves', 'list']
		]);
		expect(datum.children![1].children!.map((c) => c.value)).toEqual(['1', '2']);

		const swap = decodePlutusData('d8799f182aff'); // Constr 0 [42]
		expect(variantOf(swap, v.redeemer, bp.definitions)).toBe('Action.Swap');
		expect(typeData(swap, v.redeemer, bp.definitions).children![0]).toMatchObject({ field: 'min_out', value: '42' });
		expect(variantOf(decodePlutusData('d87a80'), v.redeemer, bp.definitions)).toBe('Action.Close');
	});

	it('names schema types and does not repeat a field name as its type', () => {
		const bp = parseBlueprint(json);
		expect(typeNameOf(bp.validators[0].redeemer, bp.definitions)).toBe('Action');
		expect(typeNameOf({ $ref: '#/definitions/Int' }, bp.definitions)).toBe('Int');
		const t = typeData(decodePlutusData('d8799f182aff'), { title: 'T', dataType: 'constructor', index: 0, fields: [{ title: 'opaque' }] }, {});
		expect(t.children![0]).toMatchObject({ field: 'opaque', kind: 'int', value: '42' });
		expect(t.children![0].type).toBeUndefined();
	});

	it('marks data that does not fit the schema', () => {
		const bp = parseBlueprint(json);
		const t = typeData(decodePlutusData('d87b80'), bp.validators[0].redeemer, bp.definitions); // Constr 2: no such variant
		expect(t.mismatch).toBe(true);
		expect(variantOf(decodePlutusData('d87b80'), bp.validators[0].redeemer, bp.definitions)).toBeNull();
	});
});
