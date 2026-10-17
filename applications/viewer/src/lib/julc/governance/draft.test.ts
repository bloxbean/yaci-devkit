import { describe, expect, it } from 'vitest';
import { actionOf, isEmpty, queryOf } from './draft';

describe('draft links', () => {
	it('keeps parameter values, withdrawals and a custom script in the URL', () => {
		expect(queryOf({ kind: 'param', values: { treasuryGrowthRate: '1/10', minPoolCost: '170000000' }, withdrawals: [], from: null }, null)).toBe('p.treasuryGrowthRate=1%2F10&p.minPoolCost=170000000');
		expect(queryOf({ kind: 'treasury', values: {}, withdrawals: [{ address: 'stake1x', amount: '5' }, { address: ' ', amount: '' }], from: null }, 'ab')).toBe('type=treasury&w=stake1x%3A5&script=ab');
		// Cost models are too long for a link: point at the proposal they came from.
		expect(queryOf({ kind: 'param', values: {}, costModels: { '2': '80' }, withdrawals: [], from: 'tx_0' }, null)).toBe('from=tx_0');
	});

	it('turns a draft into a guardrail action', () => {
		expect(actionOf({ kind: 'treasury', values: {}, withdrawals: [{ address: ' e1aa ', amount: '7 ' }, { address: '', amount: '' }], from: null })).toEqual({ type: 'TREASURY_WITHDRAWALS_ACTION', withdrawals: [{ address: 'e1aa', amount: '7' }] });
		expect(isEmpty({ kind: 'param', values: {}, withdrawals: [], from: null })).toBe(true);
	});
});
