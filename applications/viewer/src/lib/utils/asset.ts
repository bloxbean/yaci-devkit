/**
 * Parse a Cardano native asset unit string into its components.
 * A unit is: policyId (56 hex chars) + assetNameHex (remaining hex chars).
 */
export function parseUnit(unit: string): { policyId: string; assetNameHex: string; assetNameUtf8: string } {
    const policyId = unit.slice(0, 56);
    const assetNameHex = unit.slice(56);
    let assetNameUtf8 = '';
    try {
        const bytes = new Uint8Array(assetNameHex.match(/.{1,2}/g)?.map(byte => parseInt(byte, 16)) || []);
        assetNameUtf8 = new TextDecoder('utf-8', { fatal: true }).decode(bytes);
    } catch {
        assetNameUtf8 = assetNameHex;
    }
    return { policyId, assetNameHex, assetNameUtf8 };
}

/**
 * Classify an asset identifier string.
 * - 'fingerprint': starts with 'asset1'
 * - 'policy': exactly 56 hex chars
 * - 'unit': 57+ hex chars (policy + asset name)
 * - 'unknown': anything else
 */
export function classifyAssetId(input: string): 'unit' | 'policy' | 'fingerprint' | 'unknown' {
    const trimmed = input.trim();
    if (trimmed.startsWith('asset1')) return 'fingerprint';
    if (/^[0-9a-fA-F]+$/.test(trimmed)) {
        if (trimmed.length === 56) return 'policy';
        if (trimmed.length > 56) return 'unit';
    }
    return 'unknown';
}
