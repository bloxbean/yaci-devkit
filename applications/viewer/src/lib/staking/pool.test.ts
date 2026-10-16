import { describe, expect, it } from 'vitest';
import { isPoolId, marginPct, poolBech32, poolStatus } from './pool';

describe('pool helpers', () => {
	it('converts a hex pool hash to bech32', () => {
		expect(poolBech32('00beef8710427e328a29555283c74b202b40bec9a62630a9f03b1e18')).toBe('pool1qzlwlpcsgflr9z3f24fg836tyq45p0kf5cnrp20s8v0psp6tdkx');
		expect(poolBech32('pool1abc')).toBe('pool1abc');
	});

	it('recognises pool ids', () => {
		expect(isPoolId('pool1qzlwlpcsgflr9z3f24fg836tyq45p0kf5cnrp20s8v0psp6tdkx')).toBe(true);
		expect(isPoolId('7301761068762f5900bde9eb7c1c15b09840285130f5b0f53606cc57')).toBe(true);
		expect(isPoolId('pool1zzz')).toBe(false);
	});

	it('describes status and margin', () => {
		expect(poolStatus(null, 10)).toBe('active');
		expect(poolStatus(12, 10)).toBe('retiring');
		expect(poolStatus(9, 10)).toBe('retired');
		expect(marginPct('0.02')).toBe('2%');
		expect(marginPct(0.0125)).toBe('1.25%');
	});
});
