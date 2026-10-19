/* What pages get back when a section came from Yaci Store analytics instead of the regular endpoints. */

/** Where analytics data came from, for the label next to it. */
export interface AnalyticsSource {
	/** The last exported day (ISO date): newer data comes from the live views where the store has them. */
	exportedTo: string | null;
	live: boolean;
}
