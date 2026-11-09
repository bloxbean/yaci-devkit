import { describe, expect, it } from 'vitest';
import { bech32Encode } from '$lib/bech32';
import { actionKey, drepKey, poolKey, scriptKeyOfAddress, tokenKey } from './keys';

const hash = '96ffa0b04d238780ca6b9d2343612820fe3178e79f392a711d705a85';
const bytes = (hex: string) => new Uint8Array(hex.match(/../g)!.map((h) => parseInt(h, 16)));

describe('name keys', () => {
	it('finds the script hash of script addresses only', () => {
		// Minswap's order contract address (script payment part, key stake part).
		const minswap = 'addr1wxt2d5z2cxpaxj0jwlcqmyav08lpwrwwxacna2dyj0melqgugxgdz';
		expect(scriptKeyOfAddress(minswap)).toBe('96a6d04ac183d349f277f00d93ac79fe170dce37713ea9a493f79f81');
		expect(scriptKeyOfAddress(bech32Encode('addr', bytes('61' + hash)))).toBeNull(); // key enterprise address
		expect(scriptKeyOfAddress(bech32Encode('stake', bytes('f1' + hash)))).toBe(hash);
		expect(scriptKeyOfAddress(bech32Encode('stake', bytes('e1' + hash)))).toBeNull();
		expect(scriptKeyOfAddress('DdzFFzCqrht…')).toBeNull();
	});

	it('reads DRep ids in any form as the credential hash', () => {
		expect(drepKey('drep1y2t0lg9sf53c0qx2dwwjxsmp9qs0uvtcu70nj2n3r4c94pgvaw2z2')).toBe(hash); // CIP-129
		expect(drepKey(bech32Encode('drep', bytes(hash)))).toBe(hash); // CIP-105
		expect(drepKey(hash.toUpperCase())).toBe(hash);
		expect(drepKey('drep_always_abstain')).toBeNull();
	});

	it('reads pool ids as hex', () => {
		const hex = '153806dbcd134ddee69a8c5204e38ac80448f62342f8c23cfe4b7edf';
		expect(poolKey('pool1z5uqdk7dzdxaae5633fqfcu2eqzy3a3rgtuvy087fdld7yws0xt')).toBe(hex);
		expect(poolKey(hex)).toBe(hex);
		expect(poolKey('pool1nope')).toBeNull();
	});

	it('reads governance action ids as tx#index', () => {
		const tx = '14d6a48df8b9099a3e727e1bf6a9ac6c911341a9601e91c7d3ac799bfd89b01b';
		expect(actionKey(tx, 0)).toBe(`${tx}#0`);
		expect(actionKey(`${tx}_3`)).toBe(`${tx}#3`);
		expect(actionKey('gov_action1znt2fr0chyye50nj0cdld2dvdjg3xsdfvq0fr37n43uehlvfkqdsqjnuq3n')).toBe(`${tx}#0`);
		expect(actionKey('nope')).toBeNull();
	});

	it('accepts token units only', () => {
		expect(tokenKey('29D222CE763455E3D7A09A665CE554F00AC89D2E99A1A83D267170C64D494E')).toBe('29d222ce763455e3d7a09a665ce554f00ac89d2e99a1a83d267170c64d494e');
		expect(tokenKey('lovelace')).toBeNull();
	});
});
