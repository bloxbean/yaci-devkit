import { describe, expect, it } from 'vitest';
import { checkAnchorUrl, isBlockedIp } from './anchor-url';
import { blake2b256Hex } from './blake2b';

const refused = (u: string) => !checkAnchorUrl(u).ok;

describe('checkAnchorUrl', () => {
	it('accepts public http(s) and maps ipfs to a gateway', () => {
		expect(checkAnchorUrl('https://example.com/meta.jsonld').ok).toBe(true);
		expect(checkAnchorUrl('http://example.com:80/a').ok).toBe(true);
		const ipfs = checkAnchorUrl('ipfs://bafkreigulsqznngqsp7ry3zkkma4h4zpb5ff7ngrgba4t6fv5wbetxbu6m');
		expect(ipfs.ok && ipfs.url.href).toBe('https://ipfs.io/ipfs/bafkreigulsqznngqsp7ry3zkkma4h4zpb5ff7ngrgba4t6fv5wbetxbu6m');
		expect(ipfs.ok && ipfs.alternates?.length).toBe(3);
	});

	it('refuses other schemes, credentials and odd ports', () => {
		expect(refused('file:///etc/passwd')).toBe(true);
		expect(refused('ftp://example.com/x')).toBe(true);
		expect(refused('javascript:alert(1)')).toBe(true);
		expect(refused('gopher://example.com')).toBe(true);
		expect(refused('https://user:pw@example.com/')).toBe(true);
		expect(refused('http://example.com:8080/')).toBe(true);
		expect(refused('ipfs://../../etc')).toBe(true);
		expect(refused('')).toBe(true);
	});

	it('refuses local hosts and private address literals', () => {
		for (const u of [
			'http://localhost:8080/api',
			'http://localhost/',
			'http://foo.localhost/',
			'http://printer.local/',
			'http://intranet/',
			'http://127.0.0.1/',
			'http://127.1.2.3/',
			'http://10.0.0.5/',
			'http://172.20.0.1/',
			'http://192.168.1.1/',
			'http://169.254.169.254/latest/meta-data/',
			'http://0.0.0.0/',
			'http://[::1]/',
			'http://[::ffff:127.0.0.1]/',
			'http://[::ffff:7f00:1]/',
			'http://[fe80::1]/',
			'http://[fd00::1]/',
			'http://2130706433/',
			'http://0x7f000001/'
		]) {
			expect(refused(u), u).toBe(true);
		}
	});
});

describe('isBlockedIp', () => {
	it('blocks special ranges, including IPv6 forms that embed IPv4', () => {
		for (const ip of ['127.0.0.1', '10.1.2.3', '100.64.0.1', '172.31.255.255', '192.168.0.1', '169.254.169.254', '224.0.0.1', '255.255.255.255', '::', '::1', '::ffff:10.0.0.1', '64:ff9b::7f00:1', '2002:7f00:1::', 'fc00::1', 'fe80::abcd', 'not-an-ip']) {
			expect(isBlockedIp(ip), ip).toBe(true);
		}
	});

	it('allows public addresses', () => {
		for (const ip of ['1.1.1.1', '93.184.216.34', '172.32.0.1', '2606:4700:4700::1111', '::ffff:8.8.8.8']) {
			expect(isBlockedIp(ip), ip).toBe(false);
		}
	});
});

describe('blake2b256Hex', () => {
	it('matches the RFC test vectors', () => {
		expect(blake2b256Hex(new TextEncoder().encode('abc'))).toBe('bddd813c634239723171ef3fee98579b94964e3bb1cb3e427262c8c068d52319');
		expect(blake2b256Hex(new Uint8Array())).toBe('0e5751c026e543b2e8ab2eb06099daa1d1e5df47778f7787faab45cdf12fe3a8');
		// Longer than one block
		expect(blake2b256Hex(new Uint8Array(300).fill(97))).toHaveLength(64);
	});
});

import { rewardHexToStake } from './reward';

describe('rewardHexToStake', () => {
	it('encodes reward addresses', () => {
		expect(rewardHexToStake('e19bf9954990de3a15a4e6a8923cb2d3aa2521a75c8dc2997f46dee8dd')).toMatch(/^stake1/);
		expect(rewardHexToStake('e0' + '00'.repeat(28))).toMatch(/^stake_test1/);
		expect(rewardHexToStake('zz')).toBeNull();
	});
});

import { drepIdCandidates, drepIdFromHash } from './drep';

describe('drep ids', () => {
	it('builds CIP-129 ids from hashes', () => {
		expect(drepIdFromHash('69077c7a93a4131767c72721d187b5d386eaaa70d9b4022dcb77fc9d')).toBe('drep1yf5swlr6jwjpx9m8cunjr5v8khfcd642wrvmgq3dedmle8g8lrfgp');
		expect(drepIdCandidates('drep1yf5swlr6jwjpx9m8cunjr5v8khfcd642wrvmgq3dedmle8g8lrfgp')).toEqual(['drep1yf5swlr6jwjpx9m8cunjr5v8khfcd642wrvmgq3dedmle8g8lrfgp']);
		expect(drepIdCandidates('69077c7a93a4131767c72721d187b5d386eaaa70d9b4022dcb77fc9d')).toHaveLength(2);
		expect(drepIdCandidates('nonsense')).toEqual([]);
	});
});
