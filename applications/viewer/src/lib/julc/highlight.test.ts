import { describe, expect, it } from 'vitest';
import { splitMatches, tokenize } from './highlight';

// Adjacent tokens of the same kind (names and spaces) are merged into one span.
const kinds = (line: string, lang: 'uplc') => tokenize(line, lang).filter((t) => t.text.trim()).map((t) => [t.text.trim(), t.kind]);

describe('tokenize', () => {
	it('highlights UPLC keywords, builtins, types and constants', () => {
		expect(kinds('[(builtin addInteger) (con integer -5)]', 'uplc')).toEqual([
			['[(', 'punct'],
			['builtin', 'kw'],
			['addInteger', 'builtin'],
			[')', 'punct'],
			['(', 'punct'],
			['con', 'kw'],
			['integer', 'type'],
			['-5', 'num'],
			[')]', 'punct']
		]);
		expect(kinds('(con bytestring #c0ffee)', 'uplc')).toContainEqual(['#c0ffee', 'bytes']);
		expect(kinds('(con string "a \\"b\\"")', 'uplc')).toContainEqual(['"a \\"b\\""', 'str']);
		expect(kinds('(lam i_1 i_1)', 'uplc')).toEqual([
			['(', 'punct'],
			['lam', 'kw'],
			['i_1 i_1', 'plain'],
			[')', 'punct']
		]);
	});

	it('keeps every character', () => {
		const line = '  [(force (builtin ifThenElse)) (con bool True) #ff "q" ~]';
		expect(tokenize(line, 'uplc').map((t) => t.text).join('')).toBe(line);
	});
});

describe('splitMatches', () => {
	it('marks case-insensitive hits', () => {
		expect(splitMatches('addInteger AddInteger', 'addinteger')).toEqual([
			{ text: 'addInteger', hit: true },
			{ text: ' ', hit: false },
			{ text: 'AddInteger', hit: true }
		]);
		expect(splitMatches('abc', '')).toEqual([{ text: 'abc', hit: false }]);
	});
});
