/*
 * Text from off-chain metadata is shown as a short label: control and bidirectional-override characters out (so a
 * name can't reorder the text around it), whitespace collapsed, length capped. JSON-LD values can be plain strings
 * or { "@value": "…" }.
 */

// eslint-disable-next-line no-control-regex
const UNSAFE = /[\u0000-\u001f\u007f-\u009f​-‏‪-‮⁠-⁩﻿]/g;

export function cleanText(v: unknown, max: number): string | undefined {
	if (v && typeof v === 'object' && '@value' in v) v = (v as { '@value': unknown })['@value'];
	if (typeof v !== 'string') return undefined;
	const s = v.replace(UNSAFE, ' ').replace(/\s+/g, ' ').trim();
	if (!s) return undefined;
	return s.length > max ? `${s.slice(0, max - 1).trimEnd()}…` : s;
}
