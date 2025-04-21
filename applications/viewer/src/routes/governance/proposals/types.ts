export interface GovAction {
    type: string;
    policyHash?: string;
    withdrawals?: Record<string, number>;
    threshold?: {
        numerator: number;
        denominator: number;
    };
    govActionId?: string | null;
    membersForRemoval?: string[];
    newMembersAndTerms?: Record<string, number>;
}

export interface Proposal {
    tx_hash: string;
    index: number;
    slot: number;
    deposit: number;
    return_address: string;
    gov_action: GovAction;
    anchor_url: string;
    anchor_hash: string;
    status: string;
    epoch: number;
    block_number: number;
    block_time: number;
} 