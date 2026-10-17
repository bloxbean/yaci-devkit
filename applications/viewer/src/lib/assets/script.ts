/* Yaci Store reports script types as plutusV1/V2/V3, timelock (native), or PLUTUS_V3-style enums in tx scripts. */
export function scriptTypeLabel(type: string | null | undefined): string {
	const t = String(type ?? '').toLowerCase().replace(/[_\s]/g, '');
	if (t.startsWith('plutusv')) return `Plutus V${t.slice(7)}`;
	if (t === 'timelock' || t.includes('native') || t === 'multisig') return 'Native script';
	return type ? String(type) : 'Script';
}

export const isPlutus = (type: string | null | undefined) => String(type ?? '').toLowerCase().includes('plutus');

/** 'V1' | 'V2' | 'V3' for Plutus script types (plutusV2, PLUTUS_V3, PlutusScriptV1), else null. */
export function plutusVersionOf(type: string | null | undefined): 'V1' | 'V2' | 'V3' | null {
	const m = String(type ?? '').match(/plutus\D*v?_?([123])/i);
	return m ? (`V${m[1]}` as 'V1' | 'V2' | 'V3') : null;
}
