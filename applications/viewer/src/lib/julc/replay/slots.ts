/*
 * Slot → POSIX time (ms), which is how a ScriptContext expresses a transaction's validity range.
 *
 * Public networks started in the Byron era with 20 s slots, so their genesis start time can't be used directly:
 * they use the time and slot where Shelley (1 s slots) began. Devnets (and other custom networks) have no Byron
 * slots, so time = system start + slot × slot length, from Yaci Store's /genesis.
 */

export interface SlotConfig {
	/** POSIX ms of `zeroSlot`. */
	zeroTime: number;
	zeroSlot: number;
	/** Slot length in ms. */
	slotLength: number;
}

const KNOWN: Record<number, SlotConfig> = {
	764824073: { zeroTime: 1596059091000, zeroSlot: 4492800, slotLength: 1000 }, // mainnet
	1: { zeroTime: 1655769600000, zeroSlot: 86400, slotLength: 1000 }, // preprod
	2: { zeroTime: 1666656000000, zeroSlot: 0, slotLength: 1000 } // preview
};

export interface Genesis {
	network_magic?: number;
	/** Seconds since the epoch. */
	system_start?: number;
	/** Seconds (may be fractional on fast devnets). */
	slot_length?: number;
}

export function slotConfigFor(magic: number | null | undefined, genesis: Genesis | null | undefined): SlotConfig | null {
	if (magic != null && KNOWN[magic]) return KNOWN[magic];
	if (genesis?.system_start == null || !genesis.slot_length) return null;
	return { zeroTime: Math.round(genesis.system_start * 1000), zeroSlot: 0, slotLength: Math.round(genesis.slot_length * 1000) };
}

export function slotToPosixMs(slot: number, config: SlotConfig): bigint {
	return BigInt(config.zeroTime) + BigInt(slot - config.zeroSlot) * BigInt(config.slotLength);
}
