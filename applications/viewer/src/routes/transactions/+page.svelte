<script lang="ts">
    // Moved imports first for convention
    import { formatAda, getDate, truncate } from '$lib/util';
    import { goto } from '$app/navigation'; 
    import { page } from '$app/stores';   
    import { onMount } from 'svelte';      

    // Add toast state
    let showToast = false;
    let toastTimeout: number;

    // Add clipboard function with toast
    async function copyToClipboard(text: string) {
        try {
            await navigator.clipboard.writeText(text);
            showToast = true;
            // Clear any existing timeout
            if (toastTimeout) {
                clearTimeout(toastTimeout);
            }
            // Hide toast after 2 seconds
            toastTimeout = setTimeout(() => {
                showToast = false;
            }, 2000);
        } catch (err) {
            console.error('Failed to copy text: ', err);
        }
    }

    // Cleanup timeout on component destroy
    onMount(() => {
        return () => {
            if (toastTimeout) {
                clearTimeout(toastTimeout);
            }
        };
    });

    // --- Type Definitions --- 
    interface TxSummary {
        tx_hash: string;
        block_number: number;
        slot: number;
        total_output: number;
        fee: number;
        output_addresses: string[];
        timestamp?: number;
        epoch_no?: number;
    }
    interface SuccessData {
        txs: TxSummary[];
        total: number;
        total_pages: number;
        page: number;
        count: number;
        status?: undefined;
        body?: undefined;
    }
    interface ErrorData {
        status: number;
        body: { error?: string };
        txs?: undefined;
    }
    
    // Declare data prop first
    export let data: SuccessData | ErrorData;
    
    // Log initial data *after* declaration
    console.log("[DEBUG Tx Page] Script started. Initial data:", data);

    // Type guard
    function isErrorData(data: SuccessData | ErrorData): data is ErrorData { 
        return data?.status !== undefined && data.status >= 400; 
    }

    // --- Pagination State --- 
    let loading = false;
    let currentPage = 1; // UI 1-based
    const ITEMS_PER_PAGE = 15;

    onMount(() => {
        console.log("[DEBUG Tx Page] Component Mounted. Data:", data);
        if (!isErrorData(data)) {
            // Ensure data.page is treated as a number
            currentPage = parseInt(String(data.page), 10) + 1; 
        }
        loading = false;
    });

    // --- Has More Logic --- 
    // Simply check if we have transactions
    $: hasMore = !isErrorData(data) && data.txs && data.txs.length > 0;

    // --- Navigation Function --- 
    function goToPage(targetPage: number) {
        if (targetPage < 1 || loading) return;
        loading = true;
        // Use 1-based page numbers in URL
        const url = `/transactions?page=${targetPage}&count=${ITEMS_PER_PAGE}`;
        goto(url);
    }

    // --- Reactive Update Block --- 
    $: if (data && $page) {
        console.log("[DEBUG Tx Page] Reactive update. Data:", data);
        if (!isErrorData(data) && data.txs) { 
            // Use the page number directly since URL is now 1-based
            currentPage = parseInt(String(data.page), 10);
            console.log(`[DEBUG Tx Page] State Updated: currentPage=${currentPage}`);
            console.log(`[DEBUG Tx Page] Calculated hasMore: ${hasMore} (Txs length: ${data.txs.length})`);
        }
        loading = false; 
    }

</script>

<div class="container mx-auto px-4 py-8">
    <!-- Toast Notification -->
    {#if showToast}
        <div class="fixed bottom-4 right-4 bg-green-500 text-white px-4 py-2 rounded-lg shadow-lg transition-opacity duration-300 z-50">
            Transaction hash copied to clipboard!
        </div>
    {/if}

    <h1 class="text-3xl font-bold mb-8">Recent Transactions</h1>

    {#if isErrorData(data)}
         <div class="alert alert-error mb-4">
            <span>{data.body?.error || 'Unknown error loading transactions.'}</span>
        </div>
    {:else}
        <!-- Remove top pagination controls -->
        {#if !data.txs || data.txs.length === 0}
             <div class="alert alert-info mb-4">
                <span>No transactions found.</span>
            </div>
        {:else}
        <div class="flex justify-end mt-4 mb-4">
                <div class="join">
                    <button 
                        class="join-item btn btn-sm"
                        on:click={() => goToPage(currentPage - 1)}
                        disabled={currentPage == 1 || loading}
                    >
                        «
                    </button>
                    <button class="join-item btn btn-sm">
                        Page {currentPage}
                    </button>
                    <button 
                        class="join-item btn btn-sm"
                        on:click={() => goToPage(currentPage + 1)}
                        disabled={!hasMore || loading}
                    >
                        »
                    </button>
                </div>
            </div>
            <div class="bg-white shadow-md rounded-lg overflow-hidden relative">
                 {#if loading}
                     <!-- Loading Overlay -->
                     <div class="absolute inset-0 bg-white bg-opacity-75 flex justify-center items-center z-10">
                         <span class="loading loading-spinner loading-lg"></span>
                     </div>
                 {/if}
                <div class="overflow-x-auto">
                    <table class="min-w-full divide-y divide-gray-200">
                        <thead class="bg-gray-50">
                            <tr>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Tx Hash</th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Block / Slot</th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Total Output (ADA)</th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Fee (ADA)</th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Output Addresses</th>
                            </tr>
                        </thead>
                        <tbody class="bg-white divide-y divide-gray-200">
                            {#each data.txs as tx (tx.tx_hash)} 
                                <tr class="hover:bg-gray-50">
                                    <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-blue-600 hover:text-blue-800 flex items-center gap-2 align-middle">
                                        <a href="/transactions/{tx.tx_hash}">{truncate(tx.tx_hash, 15)}</a>
                                        <button 
                                            class="text-gray-400 hover:text-gray-600" 
                                            on:click={() => copyToClipboard(tx.tx_hash)}
                                            title="Copy transaction hash"
                                        >
                                            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
                                            </svg>
                                        </button>
                                    </td>
                                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                         <a href="/blocks/{tx.block_number}" class="text-blue-500 hover:underline">{tx.block_number}</a> / {tx.slot}
                                    </td>
                                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500" title="{tx.total_output} lovelace">
                                        {formatAda(tx.total_output)}
                                    </td>
                                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500" title="{tx.fee} lovelace">
                                        {formatAda(tx.fee)}
                                    </td>
                                    <td class="px-6 py-4 text-sm text-gray-500 break-all">
                                        {#if tx.output_addresses}
                                            {#each tx.output_addresses as address, i (address + i)}
                                                <div class="whitespace-nowrap">{truncate(address, 25, "...")}</div>
                                            {/each}
                                        {/if}
                                    </td>
                                </tr>
                            {/each}
                        </tbody>
                    </table>
                </div>
            </div>

            <!-- Updated Bottom Pagination Controls -->
            <div class="flex justify-end mt-4 mb-4">
                <div class="join">
                    <button 
                        class="join-item btn btn-sm"
                        on:click={() => goToPage(currentPage - 1)}
                        disabled={currentPage == 1 || loading}
                    >
                        «
                    </button>
                    <button class="join-item btn btn-sm">
                        Page {currentPage}
                    </button>
                    <button 
                        class="join-item btn btn-sm"
                        on:click={() => goToPage(currentPage + 1)}
                        disabled={!hasMore || loading}
                    >
                        »
                    </button>
                </div>
            </div>
        {/if}
    {/if}
</div>

<style>
    /* Add transition for smooth appearance/disappearance */
    .fixed {
        transition: opacity 0.3s ease-in-out;
        z-index: 50;
    }
</style>
