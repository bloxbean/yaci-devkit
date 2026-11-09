/*
 * Line-by-line syntax highlighting for UPLC. Only the lines on screen are tokenised, so even very large scripts stay
 * fast. Tokens are rendered as text, never as HTML.
 */

export type TokenKind = 'kw' | 'builtin' | 'type' | 'num' | 'str' | 'bytes' | 'punct' | 'plain';
export interface Token {
	text: string;
	kind: TokenKind;
}
export type CodeLanguage = 'uplc' | 'plain';

const UPLC_KW = new Set(['program', 'lam', 'delay', 'force', 'builtin', 'con', 'error', 'constr', 'case']);
const UPLC_TYPES = new Set(['integer', 'bytestring', 'string', 'bool', 'unit', 'data', 'list', 'pair', 'array', 'value', 'bls12_381_G1_element', 'bls12_381_G2_element', 'bls12_381_MlResult']);

const UPLC_RE = /("(?:[^"\\]|\\.)*")|(#[0-9a-fA-F]*)|(-?\d+)|([A-Za-z_][\w']*)|([()[\]{},.<>])|(\s+)|(.)/g;

export function tokenize(line: string, lang: CodeLanguage): Token[] {
	if (lang === 'plain') return [{ text: line, kind: 'plain' }];
	const out: Token[] = [];
	const push = (text: string, kind: TokenKind) => {
		const last = out[out.length - 1];
		if (last && last.kind === kind) last.text += text;
		else out.push({ text, kind });
	};
	let prevWord = '';
	for (const m of line.matchAll(UPLC_RE)) {
		const [text, str, bytes, num, word, punct] = m;
		if (str) push(text, 'str');
		else if (bytes) push(text, 'bytes');
		else if (num) push(text, 'num');
		else if (word) {
			if (UPLC_KW.has(word)) push(text, 'kw');
			else if (prevWord === 'builtin') push(text, 'builtin');
			else if (UPLC_TYPES.has(word) || prevWord === 'con') push(text, 'type');
			else if (word === 'True' || word === 'False' || word === 'Constr' || word === 'Map' || word === 'List' || word === 'I' || word === 'B') push(text, 'num');
			else push(text, 'plain');
			prevWord = word;
		} else if (punct) push(text, 'punct');
		else push(text, 'plain');
	}
	return out;
}

/** Split a line into plain and matched parts, for highlighting search hits (case-insensitive). */
export function splitMatches(text: string, query: string): { text: string; hit: boolean }[] {
	if (!query) return [{ text, hit: false }];
	const parts: { text: string; hit: boolean }[] = [];
	const lower = text.toLowerCase();
	const q = query.toLowerCase();
	let at = 0;
	for (let i = lower.indexOf(q); i !== -1; i = lower.indexOf(q, i + q.length)) {
		if (i > at) parts.push({ text: text.slice(at, i), hit: false });
		parts.push({ text: text.slice(i, i + q.length), hit: true });
		at = i + q.length;
	}
	if (at < text.length) parts.push({ text: text.slice(at), hit: false });
	return parts;
}
