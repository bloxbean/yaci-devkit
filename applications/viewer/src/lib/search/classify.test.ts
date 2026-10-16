import { describe, expect, it } from 'vitest';
import { classify } from './classify';

const kinds = (q: string) => classify(q).map((c) => c.kind);

describe('classify', () => {
	it('recognises bech32 entities with certainty', () => {
		expect(classify('addr_test1vzs0r2nae22szlq3ul3h8t4u7rz9dr850mqjh98caddm4zcypzjjt')[0]).toMatchObject({
			kind: 'address',
			certain: true
		});
		expect(kinds('stake_test1uqfu74w3wh4gfzu8m6e7j987h4lq9r3t7ef5gaw497uu85qsqfy27')).toEqual(['stake']);
		expect(kinds('pool1pu5jlj4q9w9jlxeu370a3c9myx47md5j5m2str0naunn2q3lkdy')).toEqual(['pool']);
		expect(kinds('drep1ygr9tuapcanc3kpeyy4dc3vmrz9cfe5q7v9wj3x9j0ap3tswtre9j')).toEqual(['drep']);
		expect(kinds('asset1rjklcrnsdzqp65wjgrg55sy9723kw09mlgvlc3')).toEqual(['fingerprint']);
	});

	it('offers block and epoch for a number', () => {
		expect(kinds('1234')).toEqual(['block', 'epoch']);
		expect(classify('epoch 12')[0]).toMatchObject({ kind: 'epoch', href: '/epochs/12' });
	});

	it('treats 64-hex as tx, block, datum or asset, and 56-hex as policy, script, pool or DRep', () => {
		expect(kinds('a'.repeat(64))).toEqual(['tx', 'block', 'datum', 'asset']);
		expect(kinds('b'.repeat(56))).toEqual(['policy', 'script', 'pool', 'drep']);
		expect(kinds('c'.repeat(56) + '59616369546f6b656e')).toEqual(['asset']);
	});

	it('reads <txhash>#<index> as a UTxO reference or a governance action', () => {
		const h = 'd'.repeat(64);
		expect(classify(`${h}#0`).map((c) => c.href)).toEqual([`/transactions/${h}`, `/governance/govactions/${h}_0`]);
	});

	it('ignores junk', () => {
		expect(classify('   ')).toEqual([]);
		expect(classify('hello world')).toEqual([]);
	});
});
