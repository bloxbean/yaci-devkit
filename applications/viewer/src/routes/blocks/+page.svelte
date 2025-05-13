<script lang="ts">
    import { formatAda, getDate, truncate } from '$lib/util';
    import { goto } from '$app/navigation';
    import { page } from '$app/stores';
    import { onMount } from 'svelte';

    // Corrected Type for a single block based on server data
    interface Block {
        number: number;
        slot: number;
        slot_leader: string; // Pool ticker/ID
        size: number; // Size in bytes
        tx_count: number;
        // Remove fields not actually present in server response for the list view
        // timestamp?: number; 
        // no_of_transactions?: number;
        // total_output?: number;
        // total_fee?: number;
        // block_size?: number;
    }

    // Type for successful data response
    interface SuccessData {
        blocks: Block[];
        total: number;
        total_pages: number;
        page: number; // 0-based from server
        count: number;
        status?: undefined;
        body?: undefined;
    }

    // Type for error response
    interface ErrorData {
        status: number;
        body: { error?: string };
        blocks?: undefined;
    }

    // Combined type for the data prop
    export let data: SuccessData | ErrorData;

    // Type guard to check if data is an error
    function isErrorData(data: SuccessData | ErrorData): data is ErrorData {
        // Add optional chaining for safety
        return data?.status !== undefined && data.status >= 400;
    }

    // --- Pagination State (UI uses 1-based) --- 
    let loading = false;
    let currentPage = 1; 
    const ITEMS_PER_PAGE = 15;

    onMount(() => {
        if (!isErrorData(data)) {
            // Ensure data.page is treated as a number
            currentPage = parseInt(String(data.page), 10) + 1; 
        }
        loading = false;
    });

    // --- Has More Logic --- 
    // Simply check if we have blocks
    $: hasMore = !isErrorData(data) && data.blocks && data.blocks.length > 0;

    // --- Reactive Update Block --- 
    $: if (data && $page) { 
        console.log("[DEBUG Blocks Page] Reactive update. Data Received:", data);
        if (!isErrorData(data)) {
             // Use the page number directly since URL is now 1-based
             currentPage = parseInt(String(data.page), 10);
             console.log(`[DEBUG Blocks Page] State Updated: currentPage=${currentPage}`);
             console.log(`[DEBUG Blocks Page] Calculated hasMore: ${hasMore} (Blocks length: ${data.blocks?.length})`);
        }
        loading = false; 
        console.log("[DEBUG Blocks Page] Loading set to false.");
    }

    // --- Navigation Function (targetPage should be number due to calculation) ---
    function goToPage(targetPage: number) {
        if (targetPage < 1 || loading) return;
        loading = true;
        // Use 1-based page numbers in URL
        const url = `/blocks?page=${targetPage}&count=${ITEMS_PER_PAGE}`;
        goto(url);
    }

</script>

<div class="container mx-auto px-4 py-8">
    <h1 class="text-3xl font-bold mb-8">Recent Blocks</h1>

    {#if isErrorData(data)}
        <div class="alert alert-error mb-4">
            <span>{data.body?.error || 'Unknown error loading blocks.'}</span>
        </div>
    {:else}
        <!-- Table Display -->
        {#if !data.blocks || data.blocks.length === 0}
            <div class="alert alert-info mb-4">
                <span>No blocks found.</span>
            </div>
        {:else}
         <!-- Pagination Controls -->
            <div class="flex justify-end mt-6">
                <div class="join">
                    <button 
                        class="join-item btn btn-sm" 
                        disabled={currentPage <= 1 || loading}
                        on:click={() => goToPage(currentPage - 1)}
                    >
                        «
                    </button>
                    <button class="join-item btn  btn-sm">
                        Page {currentPage}
                    </button>
                    <button 
                        class="join-item btn  btn-sm" 
                        disabled={!hasMore || loading}
                        on:click={() => goToPage(currentPage + 1)}
                    >
                        »
                    </button>
                </div>
            </div>
            <div class="bg-white shadow-md rounded-lg overflow-hidden relative">
                <!-- Loading Overlay -->
                {#if loading}
                    <div class="absolute inset-0 bg-white bg-opacity-75 flex justify-center items-center z-10">
                        <span class="loading loading-spinner loading-lg"></span>
                    </div>
                {/if}
                <div class="overflow-x-auto">
                    <table class="min-w-full divide-y divide-gray-200">
                        <thead class="bg-gray-50">
                            <tr>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Block</th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Slot</th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Pool</th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Size (kb)</th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"># of Txs</th>
                            </tr>
                        </thead>
                        <tbody class="bg-white divide-y divide-gray-200">
                            {#each data.blocks as block (block.number)}
                                <tr class="hover:bg-gray-50">
                                    <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-blue-600 hover:text-blue-800">
                                        <a href="/blocks/{block.number}">{block.number}</a>
                                    </td>
                                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{block.slot}</td>
                                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{truncate(block.slot_leader, 15)}</td>
                                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{(block.size / 1024).toFixed(2)}</td>
                                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{block.tx_count}</td>
                                </tr>
                            {/each}
                        </tbody>
                    </table>
                </div>
            </div>
            <!-- Pagination Controls -->
            <div class="flex justify-end mt-6">
                <div class="join">
                    <button 
                        class="join-item btn btn-sm" 
                        disabled={currentPage <= 1 || loading}
                        on:click={() => goToPage(currentPage - 1)}
                    >
                        «
                    </button>
                    <button class="join-item btn  btn-sm">
                        Page {currentPage}
                    </button>
                    <button 
                        class="join-item btn  btn-sm" 
                        disabled={!hasMore || loading}
                        on:click={() => goToPage(currentPage + 1)}
                    >
                        »
                    </button>
                </div>
            </div>
        {/if}
    {/if}
</div>
