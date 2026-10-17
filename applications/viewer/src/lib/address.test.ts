import { describe, expect, it } from 'vitest';
import { addressInfo } from './address';

describe('addressInfo', () => {
	it('reads the address type from the header', () => {
		expect(addressInfo('addr1qxzcr6xfgu4u5dwhruqml9yj0v9j8l2vl3qcwdla4famv3vlex2jvsm78g26gu6sjg7t95a2y5s6whydc2vh73k7arws7svqpg')).toMatchObject({
			label: 'Base address',
			script: false,
			network: 'mainnet'
		});
		expect(addressInfo('addr1z8j6ytjvxwsx5w3zq').script).toBe(true);
		expect(addressInfo('addr1w8abc').label).toBe('Script enterprise address');
		expect(addressInfo('addr_test1vzs0r2nae22szlq3ul3h8t4u7rz9dr850mqjh98caddm4zcypzjjt')).toMatchObject({
			label: 'Enterprise address',
			delegates: false,
			network: 'testnet'
		});
		expect(addressInfo('stake1ux0un9fxgdlr59dywdgfy09j6w4z2gd8tjxu9xtlgm0w3hgp0qx48').era).toBe('stake');
	});
});

import { stakeAddressOf } from './bech32';

describe('stakeAddressOf', () => {
	it('derives the stake address of a base address', () => {
		expect(
			stakeAddressOf('addr1qxzcr6xfgu4u5dwhruqml9yj0v9j8l2vl3qcwdla4famv3vlex2jvsm78g26gu6sjg7t95a2y5s6whydc2vh73k7arws7svqpg')
		).toBe('stake1ux0un9fxgdlr59dywdgfy09j6w4z2gd8tjxu9xtlgm0w3hgp0qx48');
		expect(stakeAddressOf('addr_test1vzs0r2nae22szlq3ul3h8t4u7rz9dr850mqjh98caddm4zcypzjjt')).toBeNull();
		expect(stakeAddressOf('not an address')).toBeNull();
	});
});
