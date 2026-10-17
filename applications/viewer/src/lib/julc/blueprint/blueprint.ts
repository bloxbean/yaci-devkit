import type { PlutusData } from '$lib/cbor';
import { blake2b } from '$lib/governance/blake2b';

/*
 * CIP-57 blueprints (plutus.json): find a script's datum and redeemer schemas and use them to show Plutus data with
 * type, variant and field names. Aiken 1.x lists one validator per handler (title "module.name.spend", ".mint",
 * ".withdraw", …) sharing one hash, so a schema is looked up by script hash and purpose.
 */

export type Schema = Record<string, unknown>;

export interface BlueprintValidator {
	title: string;
	hash: string;
	/** spend, mint, reward, cert, vote, propose; null when the title doesn't say. */
	purpose: string | null;
	datum: Schema | null;
	redeemer: Schema | null;
	parameters: { title: string; schema: Schema }[];
}

export interface Blueprint {
	title: string;
	version: string | null;
	plutusVersion: string | null;
	validators: BlueprintValidator[];
	definitions: Record<string, Schema>;
}

const HANDLERS: Record<string, string> = { spend: 'spend', mint: 'mint', withdraw: 'reward', publish: 'cert', vote: 'vote', propose: 'propose', else: 'else' };

const hexToBytes = (hex: string) => new Uint8Array((hex.match(/../g) ?? []).map((h) => parseInt(h, 16)));
const toHex = (b: Uint8Array) => Array.from(b, (x) => x.toString(16).padStart(2, '0')).join('');

/** Script hash of a blueprint's compiledCode (CBOR-wrapped flat): BLAKE2b-224 of the language tag and the script. */
export function scriptHashOf(compiledCode: string, plutusVersion: string | null): string {
	const tag = { v1: 1, v2: 2, v3: 3 }[String(plutusVersion ?? 'v3').toLowerCase()] ?? 3;
	return toHex(blake2b(new Uint8Array([tag, ...hexToBytes(compiledCode)]), 28));
}

export function parseBlueprint(json: unknown): Blueprint {
	const b = json as { preamble?: Record<string, string>; validators?: Record<string, unknown>[]; definitions?: Record<string, Schema> };
	if (!b || typeof b !== 'object' || !Array.isArray(b.validators)) throw new Error('Not a CIP-57 blueprint: there is no "validators" list.');
	const plutusVersion = b.preamble?.plutusVersion ?? null;
	const validators = b.validators.map((v) => {
		const title = String(v.title ?? 'validator');
		const handler = title.split('.').at(-1) ?? '';
		const hash = typeof v.hash === 'string' ? v.hash : typeof v.compiledCode === 'string' ? scriptHashOf(v.compiledCode, plutusVersion) : '';
		const schemaOf = (x: unknown) => ((x as { schema?: Schema })?.schema ?? null) as Schema | null;
		return {
			title,
			hash: hash.toLowerCase(),
			purpose: HANDLERS[handler] ?? null,
			datum: schemaOf(v.datum),
			redeemer: schemaOf(v.redeemer),
			parameters: (Array.isArray(v.parameters) ? v.parameters : []).map((p: { title?: string; schema?: Schema }) => ({ title: p.title ?? '', schema: p.schema ?? {} }))
		};
	});
	if (!validators.some((v) => v.hash)) throw new Error('The blueprint has no validator hashes or compiled code.');
	return { title: b.preamble?.title ?? 'Blueprint', version: b.preamble?.version ?? null, plutusVersion, validators, definitions: b.definitions ?? {} };
}

/** The validator entry for a script hash and purpose (falling back to any entry for that hash). */
export function findValidator(blueprints: Blueprint[], hash: string, purpose?: string | null): { blueprint: Blueprint; validator: BlueprintValidator } | null {
	for (const blueprint of blueprints) {
		const matches = blueprint.validators.filter((v) => v.hash === hash.toLowerCase());
		if (!matches.length) continue;
		const validator = matches.find((v) => v.purpose === purpose) ?? matches.find((v) => v.purpose !== 'else') ?? matches[0];
		return { blueprint, validator };
	}
	return null;
}

// --- typing data with a schema ---

export interface TypedNode {
	/** Field title, when the schema names the field. */
	field?: string;
	/** Type (definition) title, e.g. "Action" or "VerificationKeyHash". */
	type?: string;
	/** Constructor title, e.g. "Swap". */
	variant?: string;
	kind: 'constructor' | 'int' | 'bytes' | 'list' | 'map' | 'data';
	value?: string;
	children?: TypedNode[];
	/** The data doesn't fit the schema here; shown untyped. */
	mismatch?: boolean;
}

function resolve(schema: Schema, defs: Record<string, Schema>, depth = 0): Schema {
	const ref = schema.$ref;
	if (typeof ref !== 'string' || depth > 20) return schema;
	const key = ref.replace(/^#\/definitions\//, '').replace(/~1/g, '/').replace(/~0/g, '~');
	const target = defs[key];
	return target ? { ...resolve(target, defs, depth + 1), title: target.title ?? key.split('/').at(-1) } : schema;
}

const isConstr = (d: PlutusData): d is { constructor: number; fields: PlutusData[] } => typeof (d as { constructor: unknown }).constructor === 'number';

function untyped(d: PlutusData, field?: string, mismatch = false): TypedNode {
	if (isConstr(d)) return { field, kind: 'constructor', variant: `Constr ${d.constructor}`, children: d.fields.map((x) => untyped(x)), mismatch };
	if ('int' in d) return { field, kind: 'int', value: d.int, mismatch };
	if ('bytes' in d) return { field, kind: 'bytes', value: d.bytes, mismatch };
	if ('list' in d) return { field, kind: 'list', children: d.list.map((x) => untyped(x)), mismatch };
	return { field, kind: 'map', children: d.map.map((e) => ({ kind: 'data' as const, children: [untyped(e.k, 'key'), untyped(e.v, 'value')] })), mismatch };
}

export function typeData(d: PlutusData, raw: Schema | null | undefined, defs: Record<string, Schema>, field?: string): TypedNode {
	if (!raw) return untyped(d, field);
	const schema = resolve(raw, defs);
	// An inline field schema's title is the field's name, not a type.
	const type = typeof schema.title === 'string' && schema.title !== field ? schema.title : undefined;
	const anyOf = schema.anyOf as Schema[] | undefined;
	if (Array.isArray(anyOf)) {
		if (!isConstr(d)) {
			// e.g. {"anyOf": [{"dataType": "integer"}, {"dataType": "bytes"}]}
			const kind = 'int' in d ? 'integer' : 'bytes' in d ? 'bytes' : 'list' in d ? 'list' : 'map';
			const alt = anyOf.map((a) => resolve(a, defs)).find((a) => a.dataType === kind);
			return alt ? { ...typeData(d, alt, defs, field), type } : untyped(d, field, true);
		}
		const alt = anyOf.map((a) => resolve(a, defs)).find((a) => a.index === d.constructor || (a.dataType === 'constructor' && a.index === d.constructor));
		if (!alt) return untyped(d, field, true);
		return { ...typeData(d, alt, defs, field), type };
	}
	switch (schema.dataType) {
		case 'constructor': {
			if (!isConstr(d) || (typeof schema.index === 'number' && schema.index !== d.constructor)) return untyped(d, field, true);
			const fields = (schema.fields as Schema[] | undefined) ?? [];
			return {
				field,
				type,
				variant: typeof schema.title === 'string' ? schema.title : `Constr ${d.constructor}`,
				kind: 'constructor',
				children: d.fields.map((x, i) => typeData(x, fields[i], defs, typeof fields[i]?.title === 'string' ? (fields[i].title as string) : undefined)),
				mismatch: fields.length !== d.fields.length || undefined
			};
		}
		case 'integer':
			return 'int' in d ? { field, type, kind: 'int', value: d.int } : untyped(d, field, true);
		case 'bytes':
			return 'bytes' in d ? { field, type, kind: 'bytes', value: d.bytes } : untyped(d, field, true);
		case 'list': {
			if (!('list' in d)) return untyped(d, field, true);
			const items = schema.items;
			return { field, type, kind: 'list', children: d.list.map((x, i) => typeData(x, Array.isArray(items) ? (items[i] as Schema) : (items as Schema), defs)) };
		}
		case 'map': {
			if (!('map' in d)) return untyped(d, field, true);
			return { field, type, kind: 'map', children: d.map.map((e) => ({ kind: 'data' as const, children: [typeData(e.k, schema.keys as Schema, defs, 'key'), typeData(e.v, schema.values as Schema, defs, 'value')] })) };
		}
		default:
			// Opaque "Data", or a schema shape this viewer doesn't know: show the data as is.
			return { ...untyped(d, field), type };
	}
}

/** A schema's type name: the definition's title (or name) for a $ref, else its own title or data type. */
export function typeNameOf(s: Schema | null | undefined, defs: Record<string, Schema>): string | null {
	if (!s) return null;
	if (typeof s.$ref === 'string') {
		const key = s.$ref.replace(/^#\/definitions\//, '').replace(/~1/g, '/').replace(/~0/g, '~');
		return (defs[key]?.title as string | undefined) ?? key.split('/').at(-1) ?? key;
	}
	return (s.title as string | undefined) ?? (s.dataType as string | undefined) ?? 'Data';
}

/** The redeemer variant name (e.g. "Action.Swap") for a script's redeemer, if a blueprint describes it. */
export function variantOf(d: PlutusData, schema: Schema | null, defs: Record<string, Schema>): string | null {
	if (!schema) return null;
	const t = typeData(d, schema, defs);
	if (t.mismatch || t.kind !== 'constructor' || !t.variant || t.variant.startsWith('Constr ')) return null;
	return t.type && t.type !== t.variant ? `${t.type}.${t.variant}` : t.variant;
}
