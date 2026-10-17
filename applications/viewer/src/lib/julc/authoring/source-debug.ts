import { julc } from '../engine.svelte';
import type { RunPlan } from '../replay/build';

type Session = {
	ok: boolean;
	sessionId?: string;
	timeline?: { errorStep?: bigint | null } | null;
	goto(step: bigint): Promise<{ snapshot?: { javaLocation?: { line: number; fragment: string | null } | null } | null }>;
	close(): Promise<unknown>;
};

/**
 * Where a Java validator failed: julc's source-debug build maps the failing step back to a Java line. Null when the
 * run doesn't fail there or julc can't map it (the debug build is experimental in 0.1.0-pre18).
 */
export async function javaFailureLine(source: string, params: string[], plan: RunPlan): Promise<{ line: number; fragment: string | null } | null> {
	const req = plan.request!;
	const sd = await julc.run(async (c) => {
		const open = (c as unknown as { sourceDebug?: { open(r: unknown): Promise<Session> } }).sourceDebug?.open;
		return open ? (c as unknown as { sourceDebug: { open(r: unknown): Promise<Session> } }).sourceDebug.open({ source, params: params.map((value) => ({ format: 'cbor', value })), transaction: req.transaction, target: req.target, costModel: req.costModel, maxCpu: req.maxCpu, maxMem: req.maxMem, locals: false }) : null;
	});
	if (!sd?.ok || !sd.sessionId) return null;
	try {
		const at = sd.timeline?.errorStep;
		if (at == null) return null;
		const loc = (await sd.goto(BigInt(at))).snapshot?.javaLocation;
		return loc?.line ? { line: loc.line, fragment: loc.fragment } : null;
	} finally {
		sd.close().catch(() => {});
	}
}
