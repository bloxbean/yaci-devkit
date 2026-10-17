/* One mint or burn event, as /assets/.../history returns it. */
export interface MintEvent {
	tx_hash: string;
	block_number: number;
	block_time: number;
	slot: number;
	policy: string;
	unit: string;
	fingerprint?: string;
	asset_name?: string;
	quantity: number | string;
	mint_type?: 'MINT' | 'BURN' | string;
}

/** Hue for an asset avatar, stable per policy. */
export const policyHue = (policy: string) => parseInt(policy.slice(0, 6) || '0', 16) % 360;
