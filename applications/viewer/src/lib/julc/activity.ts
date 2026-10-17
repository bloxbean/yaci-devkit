/* A script's recent executions, as loaded by activity.server.ts for the Activity panel. */

export interface ScriptExecution {
	tx_hash: string;
	tx_index: number;
	purpose: string;
	redeemer_data_hash: string | null;
	unit_mem: string;
	unit_steps: string;
	fee?: string | null;
}

export interface ScriptActivity {
	executions: ScriptExecution[];
	/** Redeemer data hash → CBOR, for the distinct redeemers (at most 40). */
	redeemers: Record<string, string>;
	limits: { mem: number; steps: number };
}
