/* What pages get back when a section came from Yaci Store analytics instead of the regular endpoints. */

/** Where analytics data came from, for the label next to it. */
export interface AnalyticsSource {
	/** The last exported day (ISO date): newer data comes from the live views where the store has them. */
	exportedTo: string | null;
	live: boolean;
}

/** A transaction that paid an address or spent from it. */
export interface AddressTx {
	tx_hash: string;
	slot: number;
	/** Seconds. */
	block_time: number;
	received: boolean;
	sent: boolean;
}

export interface AddressTxPage extends AnalyticsSource {
	rows: AddressTx[];
	/** The days searched (ISO dates, inclusive). */
	from: string;
	to: string;
	/** Spends are only matched against outputs created since this day. */
	lookbackFrom: string;
	/** Query string for the next (older) page, or null at the end. */
	older: string | null;
}
