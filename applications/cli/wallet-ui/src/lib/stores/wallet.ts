import { writable, derived } from 'svelte/store';

// Configuration
const CLI_URL = '';
const STORE_URL = 'http://localhost:8080';

// Types
export interface WalletAccount {
    id: string;
    name: string;
    address: string;
    stakeAddress?: string;
    isDefault: boolean;
}

export interface Utxo {
    tx_hash: string;
    output_index: number;
    address: string;
    amount: Array<{ unit: string; quantity: string | number }>;
    block_number?: number;
    block_time?: number;
}

export interface TokenBalance {
    unit: string;
    quantity: string;
    policyId?: string;
    assetName?: string;
    assetNameUtf8?: string;  // Decoded name
}

export interface TransferAmount {
    unit: string;      // "lovelace" or "{policyId}{assetNameHex}"
    quantity: string;  // Amount as string
}

export interface TransferResult {
    success: boolean;
    txHash?: string;
    message?: string;
    errorCode?: string;
}

export interface WalletState {
    accounts: WalletAccount[];
    selectedAccountId: string | null;
    balance: bigint;
    tokens: TokenBalance[];         // Native tokens (excluding lovelace)
    utxos: Utxo[];
    loading: boolean;
    transferInProgress: boolean;
    error: string | null;
}

// Initial state
const initialState: WalletState = {
    accounts: [],
    selectedAccountId: null,
    balance: BigInt(0),
    tokens: [],
    utxos: [],
    loading: false,
    transferInProgress: false,
    error: null
};

// Helper to decode hex to UTF-8
function hexToUtf8(hex: string): string {
    try {
        if (!hex) return '';
        const bytes = new Uint8Array(hex.match(/.{1,2}/g)?.map(byte => parseInt(byte, 16)) || []);
        return new TextDecoder().decode(bytes);
    } catch {
        return hex; // Return original if decoding fails
    }
}

// Helper to parse token unit into policyId and assetName
function parseTokenUnit(unit: string): { policyId: string; assetName: string; assetNameUtf8: string } {
    // Policy ID is 56 characters (28 bytes hex)
    const policyId = unit.substring(0, 56);
    const assetNameHex = unit.length > 56 ? unit.substring(56) : '';
    const assetNameUtf8 = hexToUtf8(assetNameHex);
    return { policyId, assetName: assetNameHex, assetNameUtf8 };
}

// Create the wallet store
function createWalletStore() {
    const { subscribe, set, update } = writable<WalletState>(initialState);

    return {
        subscribe,

        // Load all accounts from CLI
        async loadAccounts(): Promise<void> {
            update(s => ({ ...s, loading: true, error: null }));
            try {
                const response = await fetch(`${CLI_URL}/api/v1/wallet/accounts`);
                if (!response.ok) {
                    throw new Error('Failed to fetch accounts');
                }
                const data = await response.json();
                const accounts = data.accounts || [];

                update(s => ({
                    ...s,
                    accounts,
                    selectedAccountId: accounts.length > 0 ? accounts[0].id : null,
                    loading: false
                }));

                // Auto-load balance for first account
                if (accounts.length > 0) {
                    await this.loadBalance(accounts[0].address);
                    await this.loadUtxos(accounts[0].address);
                }
            } catch (error: any) {
                update(s => ({
                    ...s,
                    loading: false,
                    error: error.message || 'Failed to load accounts'
                }));
            }
        },

        // Select an account
        async selectAccount(accountId: string): Promise<void> {
            update(s => ({ ...s, selectedAccountId: accountId }));

            // Get the store value to access the selected account's address
            let currentState: WalletState;
            subscribe(s => { currentState = s; })();

            const account = currentState!.accounts.find(a => a.id === accountId);
            if (account) {
                await this.loadBalance(account.address);
                await this.loadUtxos(account.address);
            }
        },

        // Load balance for an address from Yaci Store (includes native tokens)
        async loadBalance(address: string): Promise<void> {
            try {
                const response = await fetch(`${STORE_URL}/api/v1/addresses/${address}/balance`);
                if (!response.ok) {
                    throw new Error('Failed to fetch balance');
                }
                const data = await response.json();

                // Parse balance and tokens from amounts array
                let balance = BigInt(0);
                const tokens: TokenBalance[] = [];

                if (data.amounts && Array.isArray(data.amounts)) {
                    for (const amount of data.amounts) {
                        const unit = amount.unit;
                        const quantity = String(amount.quantity || '0');

                        if (unit === 'lovelace') {
                            balance = BigInt(quantity);
                        } else {
                            // Native token
                            const { policyId, assetName, assetNameUtf8 } = parseTokenUnit(unit);
                            tokens.push({
                                unit,
                                quantity,
                                policyId,
                                assetName,
                                assetNameUtf8
                            });
                        }
                    }
                }

                update(s => ({ ...s, balance, tokens }));
            } catch (error) {
                console.warn('Failed to load balance:', error);
                update(s => ({ ...s, balance: BigInt(0), tokens: [] }));
            }
        },

        // Load UTXOs for an address from Yaci Store
        async loadUtxos(address: string): Promise<void> {
            try {
                const response = await fetch(`${STORE_URL}/api/v1/addresses/${address}/utxos`);
                if (!response.ok) {
                    throw new Error('Failed to fetch UTXOs');
                }
                const utxos = await response.json();
                update(s => ({ ...s, utxos: Array.isArray(utxos) ? utxos : [] }));
            } catch (error) {
                console.warn('Failed to load UTXOs:', error);
                update(s => ({ ...s, utxos: [] }));
            }
        },

        // Transfer ADA and/or native tokens
        async transfer(receiverAddress: string, amounts: TransferAmount[]): Promise<TransferResult> {
            update(s => ({ ...s, transferInProgress: true, error: null }));

            try {
                // Get current account ID
                let currentState: WalletState;
                subscribe(s => { currentState = s; })();

                if (!currentState!.selectedAccountId) {
                    throw new Error('No account selected');
                }

                const response = await fetch(`${CLI_URL}/api/v1/wallet/transfer`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        accountId: currentState!.selectedAccountId,
                        receiverAddress,
                        amounts
                    })
                });

                const result: TransferResult = await response.json();

                update(s => ({ ...s, transferInProgress: false }));

                // Refresh balance after successful transfer
                if (result.success) {
                    const account = currentState!.accounts.find(a => a.id === currentState!.selectedAccountId);
                    if (account) {
                        // Wait a bit for the transaction to be processed
                        setTimeout(async () => {
                            await this.loadBalance(account.address);
                            await this.loadUtxos(account.address);
                        }, 2000);
                    }
                }

                return result;
            } catch (error: any) {
                update(s => ({
                    ...s,
                    transferInProgress: false,
                    error: error.message || 'Transfer failed'
                }));

                return {
                    success: false,
                    errorCode: 'NETWORK_ERROR',
                    message: error.message || 'Failed to connect to wallet API'
                };
            }
        },

        // Import a custom account
        async importAccount(mnemonic: string, name: string): Promise<WalletAccount | null> {
            update(s => ({ ...s, loading: true, error: null }));
            try {
                const response = await fetch(`${CLI_URL}/api/v1/wallet/accounts/import`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ mnemonic, name })
                });

                if (!response.ok) {
                    throw new Error('Failed to import account');
                }

                const account = await response.json();
                update(s => ({
                    ...s,
                    accounts: [...s.accounts, account],
                    loading: false
                }));

                return account;
            } catch (error: any) {
                update(s => ({
                    ...s,
                    loading: false,
                    error: error.message || 'Failed to import account'
                }));
                return null;
            }
        },

        // Delete a custom account
        async deleteAccount(accountId: string): Promise<boolean> {
            try {
                const response = await fetch(`${CLI_URL}/api/v1/wallet/accounts/${accountId}`, {
                    method: 'DELETE'
                });

                if (response.ok) {
                    update(s => ({
                        ...s,
                        accounts: s.accounts.filter(a => a.id !== accountId),
                        selectedAccountId: s.selectedAccountId === accountId
                            ? (s.accounts[0]?.id || null)
                            : s.selectedAccountId
                    }));
                    return true;
                }
                return false;
            } catch (error) {
                console.error('Failed to delete account:', error);
                return false;
            }
        },

        // Set the active CIP-30 account on the backend
        async setActiveAccount(accountId: string): Promise<boolean> {
            try {
                const response = await fetch(`${CLI_URL}/api/v1/wallet/active-account`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ accountId })
                });
                return response.ok;
            } catch {
                return false;
            }
        },

        // Check if wallet API is available
        async checkAvailability(): Promise<boolean> {
            try {
                const response = await fetch(`${CLI_URL}/api/v1/wallet/health`);
                return response.ok;
            } catch {
                return false;
            }
        },

        // Reset store
        reset(): void {
            set(initialState);
        }
    };
}

export const wallet = createWalletStore();

// Derived store for selected account
export const selectedAccount = derived(
    wallet,
    ($wallet) => $wallet.accounts.find(a => a.id === $wallet.selectedAccountId) || null
);

// Helper function to format lovelace to ADA
export function formatAda(lovelace: bigint | number | string): string {
    const value = typeof lovelace === 'bigint' ? lovelace : BigInt(lovelace);
    const ada = Number(value) / 1_000_000;
    return ada.toLocaleString(undefined, {
        minimumFractionDigits: 2,
        maximumFractionDigits: 6
    });
}

// Helper function to truncate address
export function truncateAddress(address: string, start = 12, end = 8): string {
    if (!address || address.length < start + end + 3) return address;
    return `${address.slice(0, start)}...${address.slice(-end)}`;
}

// Helper function to truncate hash/policy ID
export function truncateHash(hash: string, chars = 8): string {
    if (!hash || hash.length < chars * 2 + 3) return hash;
    return `${hash.slice(0, chars)}...${hash.slice(-chars)}`;
}

// Helper function to format token quantity
export function formatTokenQuantity(quantity: string | number): string {
    const num = typeof quantity === 'string' ? BigInt(quantity) : BigInt(quantity);
    return num.toLocaleString();
}
