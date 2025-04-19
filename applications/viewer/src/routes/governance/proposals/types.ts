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
    txHash: string;
    index: number;
    slot: number;
    deposit: number;
    returnAddress: string;
    govAction: GovAction;
    anchorUrl: string;
    anchorHash: string;
    status: string;
    epoch: number;
    blockNumber: number;
    blockTime: number;
} 