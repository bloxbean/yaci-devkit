import { describe, expect, it, vi } from 'vitest';

vi.mock('../engine.svelte', () => ({ julc: {} }));
const { argsFor, vmRequest } = await import('./run');
import type { RunPlan } from './build';

const plan = (over: Partial<RunPlan>): RunPlan => ({
	key: 'spend#0',
	purpose: 'spend',
	index: 0,
	scriptHash: 'ab'.repeat(28),
	language: 'V2',
	declared: null,
	redeemer: 'd87980',
	datum: '01',
	hashedDatums: [],
	witnessDatums: [],
	request: { script: { script: '4e4d01000033222220051200120011', language: 'V2' }, transaction: {} as never, target: { language: 'PlutusV2', protocol: 11 }, costModel: null, maxCpu: 10n },
	notes: [],
	...over
});
const c = (value: string) => ({ format: 'cbor' as const, value });

describe('argsFor', () => {
	it('applies V3 scripts to the context only', () => {
		expect(argsFor(plan({ language: 'V3' }), 'ctx')).toEqual([c('ctx')]);
	});
	it('applies V1/V2 spending scripts to datum, redeemer and context', () => {
		expect(argsFor(plan({}), 'ctx')).toEqual([c('01'), c('d87980'), c('ctx')]);
		expect(argsFor(plan({ datum: null }), 'ctx')).toBeNull();
	});
	it('applies other V1/V2 scripts to redeemer and context', () => {
		expect(argsFor(plan({ purpose: 'mint', datum: null }), 'ctx')).toEqual([c('d87980'), c('ctx')]);
	});
});

describe('vmRequest', () => {
	it('keeps the script, target, cost model and limits of the plan', () => {
		const p = plan({});
		expect(vmRequest(p, [c('x')])).toEqual({ script: p.request!.script, args: [c('x')], target: p.request!.target, costModel: null, maxCpu: 10n, maxMem: undefined });
	});
});
