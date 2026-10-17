import { describe, expect, it } from 'vitest';
import { ada, adaCompact, compact, duration, hexToText, parseUnit, shorten, timeAgo } from './format';

describe('format', () => {
	it('formats lovelace past the safe integer range exactly', () => {
		expect(ada('45000000000000000')).toBe('45,000,000,000.00');
		expect(ada('9000683336928985', 6)).toBe('9,000,683,336.928985');
		expect(ada(1_500_000, 'auto')).toBe('1.50');
		expect(ada(-2_000_000)).toBe('−2.00');
		expect(ada(null)).toBe('0.00');
	});

	it('compacts numbers', () => {
		expect(compact(1234)).toBe('1.2K');
		expect(compact(45_000_000_000)).toBe('45B');
		expect(adaCompact('288009970500')).toBe('288K');
	});

	it('describes time', () => {
		const now = 1_000_000_000_000;
		expect(timeAgo(now / 1000 - 30, now)).toBe('30s ago');
		expect(timeAgo(now / 1000 - 3 * 3600, now)).toBe('3h ago');
		expect(duration(192)).toBe('3m 12s');
		expect(duration(90061)).toBe('1d 1h');
	});

	it('shortens and decodes', () => {
		expect(shorten('abcdefghijklmnopqrstuvwxyz', 4, 3)).toBe('abcd…xyz');
		expect(hexToText('4d494e54')).toBe('MINT');
		expect(hexToText('00ff')).toBeNull();
		expect(parseUnit('lovelace').name).toBe('ADA');
		expect(parseUnit('c'.repeat(56) + '4d494e54')).toMatchObject({ policy: 'c'.repeat(56), name: 'MINT' });
		expect(parseUnit('c'.repeat(56) + '0014df105553444d')).toMatchObject({ name: 'USDM', label: 'FT' });
	});
});
