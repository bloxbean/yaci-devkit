/*
 * What kind of address this is, from its header. Shelley addresses are bech32: the first data character after
 * "addr1" / "addr_test1" carries the top five bits of the header byte, whose high nibble is the address type.
 */

const CHARSET = 'qpzry9x8gf2tvdw0s3jn54khce6mua7l';

export interface AddressInfo {
	era: 'shelley' | 'byron' | 'stake' | 'unknown';
	/** Payment part is a script (a contract address). */
	script: boolean;
	/** Has a stake part that can delegate. */
	delegates: boolean;
	label: string;
	network: 'mainnet' | 'testnet' | null;
}

const TYPES: Record<number, Omit<AddressInfo, 'era' | 'network'>> = {
	0: { script: false, delegates: true, label: 'Base address' },
	1: { script: true, delegates: true, label: 'Script address' },
	2: { script: false, delegates: true, label: 'Base address (script stake)' },
	3: { script: true, delegates: true, label: 'Script address (script stake)' },
	4: { script: false, delegates: true, label: 'Pointer address' },
	5: { script: true, delegates: true, label: 'Script pointer address' },
	6: { script: false, delegates: false, label: 'Enterprise address' },
	7: { script: true, delegates: false, label: 'Script enterprise address' }
};

export function addressInfo(addr: string): AddressInfo {
	const m = addr.match(/^(addr|addr_test|stake|stake_test)1(.)/);
	if (!m) {
		if (/^(Ae2|DdzFF)/.test(addr)) return { era: 'byron', script: false, delegates: false, label: 'Byron address', network: null };
		return { era: 'unknown', script: false, delegates: false, label: 'Address', network: null };
	}
	const network = m[1].endsWith('_test') ? 'testnet' : 'mainnet';
	if (m[1].startsWith('stake')) return { era: 'stake', script: false, delegates: true, label: 'Stake address', network };
	const top5 = CHARSET.indexOf(m[2]);
	const type = top5 >> 1;
	return { era: 'shelley', network, ...(TYPES[type] ?? { script: false, delegates: false, label: 'Address' }) };
}
