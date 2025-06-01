import { writable } from 'svelte/store';

interface AdaPotState {
    totalAmount: number;
    currentEpoch: number;
    lastUpdated: string;
    history: any[];
    loading: boolean;
    error: string | null;
}

export const adapotStore = writable<AdaPotState>({
    totalAmount: 0,
    currentEpoch: 0,
    lastUpdated: '',
    history: [],
    loading: false,
    error: null
});

export async function fetchAdaPotData(): Promise<void> {
    try {
        adapotStore.update(state => ({ ...state, loading: true }));
        
        // TODO: Implement API calls to:
        // 1. Get total AdaPot amount
        // 2. Get current epoch
        // 3. Get last updated timestamp
        // 4. Get history data
        
        adapotStore.update(state => ({ ...state, loading: false }));
    } catch (error: unknown) {
        const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred';
        adapotStore.update(state => ({ 
            ...state, 
            loading: false, 
            error: errorMessage 
        }));
    }
}

export async function fetchAdaPotHistory(startEpoch: number, endEpoch: number): Promise<any[]> {
    try {
        // TODO: Implement API call to get AdaPot history for a range of epochs
        return [];
    } catch (error: unknown) {
        const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred';
        throw new Error(`Failed to fetch AdaPot history: ${errorMessage}`);
    }
} 