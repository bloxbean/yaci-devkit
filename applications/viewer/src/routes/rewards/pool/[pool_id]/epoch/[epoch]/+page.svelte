<script lang="ts">
    import { page } from '$app/stores';
    import { goto } from '$app/navigation';
    import { onMount } from 'svelte';
    import { formatAda, formatLovelace, truncate } from '$lib/util';

    // Type definition for data from server
    export let data: {
        error: string | null;
        rewards: Array<{
            address: string;
            reward: number;
            type: string;
            earned_epoch: number;
        }>;
        poolId: string;
        epoch: number;
        currentPage: number;
        count: number;
    };

    // Interface matching updated server/API structure
    interface PoolEpochReward {
        address: string;
        reward: number;
        type: string;
        earned_epoch: number;
    }

    let loading = false;
    let error: string | null = null;
    let rewards: PoolEpochReward[] = [];
    let poolId = data?.poolId;
    let epoch = data?.epoch;
    let currentPage = 1;
    const count = data?.count || 15;

    // Add toast state
    let showToast = false;
    let toastTimeout: number;
    let toastMessage = '';

    // Add clipboard function with toast
    async function copyToClipboard(text: string, message: string) {
        try {
            await navigator.clipboard.writeText(text);
            showToast = true;
            toastMessage = message;
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

    onMount(() => {
        currentPage = data?.currentPage || 1;
        if (data) {
            error = data.error;
            rewards = data.rewards || [];
        }
        loading = false;

        return () => {
            if (toastTimeout) {
                clearTimeout(toastTimeout);
            }
        };
    });

    // Function to navigate to a different page
    function goToPage(newPage: number) {
        if (newPage < 1 || loading) return;
        loading = true;
        // Pass poolId and epoch along with page/count
        const url = `/rewards/pool/${poolId}/epoch/${epoch}?page=${newPage}&count=${count}`;
        goto(url);
    }

    // Determine if more data likely exists
    $: hasMore = rewards.length === count;

    // Update component state when data or URL changes
    $: if (data && $page && !loading) {
        currentPage = data.currentPage;
        error = data.error;
        rewards = data.rewards || [];
        poolId = data.poolId; // Update poolId/epoch in case they could change (unlikely here)
        epoch = data.epoch;
        loading = false;
    }

</script>

<div class="container mx-auto px-4 py-8">
    <!-- Toast Notification -->
    {#if showToast}
        <div class="fixed bottom-4 right-4 bg-green-500 text-white px-4 py-2 rounded-lg shadow-lg transition-opacity duration-300 z-50">
            {toastMessage}
        </div>
    {/if}

    <div class="mb-4">
        <a href="/rewards" class="btn btn-sm btn-outline btn-primary">&larr; Back to Rewards Explorer</a>
    </div>
    <h1 class="text-3xl font-bold mb-1">Rewards for Pool in Epoch {epoch}</h1>
    <div class="mb-6 flex items-center gap-2">
        <span class="text-gray-600">Pool ID:</span>
        <div class="flex items-center gap-2">
            <a href="/pools/{poolId}" class="text-blue-500 hover:underline break-all">{poolId}</a>
            <button 
                class="text-gray-400 hover:text-gray-600" 
                on:click={() => copyToClipboard(poolId, 'Pool ID copied to clipboard!')}
                title="Copy pool ID"
            >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
                </svg>
            </button>
        </div>
    </div>

    {#if error && !loading}
        <div class="alert alert-error mb-4 shadow-lg">
            <!-- Error Display -->
             <div>
                <svg xmlns="http://www.w3.org/2000/svg" class="stroke-current flex-shrink-0 h-6 w-6" fill="none" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
                <span>{error}</span>
            </div>
        </div>
    {/if}

    <!-- Pagination Controls (Top) -->
    {#if !loading && rewards.length > 0} 
        <div class="mt-0 mb-6 flex justify-between items-center">
            <button 
                class="btn btn-outline btn-sm"
                on:click={() => goToPage(currentPage - 1)}
                disabled={currentPage <= 1}
            >
                &lt; Previous
            </button>
            <span class="text-sm text-gray-700">Page {currentPage}</span>
            <button 
                class="btn btn-outline btn-sm"
                on:click={() => goToPage(currentPage + 1)}
                disabled={!hasMore} 
            >
                Next &gt;
            </button>
        </div>
    {/if}

    <!-- Content Area -->
    <div class="bg-white shadow-md rounded-lg p-4 min-h-[400px]">
        {#if loading}
             <div class="flex justify-center items-center h-64">
                 <span class="loading loading-spinner loading-lg"></span>
            </div>
        {:else if !rewards || rewards.length === 0}
            <p class="text-gray-500">No rewards found for this pool in this epoch.</p>
        {:else}
            <div class="overflow-x-auto">
                <table class="min-w-full divide-y divide-gray-200">
                    <thead class="bg-gray-50">
                        <tr>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Stake Address</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Type</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Amount (ADA)</th>
                             <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Earned Epoch</th>
                        </tr>
                    </thead>
                    <tbody class="bg-white divide-y divide-gray-200">
                        {#each rewards as item (item.address + item.earned_epoch)} 
                            <tr class="hover:bg-gray-50">
                                <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                                    <div class="flex items-center gap-2">
                                        <a href="/rewards/account/{item.address}" class="text-blue-500 hover:underline">{truncate(item.address, 60, '...')}</a>
                                        <button 
                                            class="text-gray-400 hover:text-gray-600" 
                                            on:click={() => copyToClipboard(item.address, 'Stake address copied to clipboard!')}
                                            title="Copy stake address"
                                        >
                                            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
                                            </svg>
                                        </button>
                                    </div>
                                </td>
                                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500 capitalize">{item.type}</td>
                                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500 tooltip" data-tip={formatLovelace(item.reward)}>
                                    {formatAda(item.reward)}
                                </td>
                                 <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{item.earned_epoch}</td>
                            </tr>
                        {/each}
                    </tbody>
                </table>
            </div>
        {/if}

        <!-- Pagination Controls (Bottom) -->
        {#if !loading && rewards.length > 0} 
            <div class="mt-6 flex justify-between items-center">
                <button 
                    class="btn btn-outline btn-sm"
                    on:click={() => goToPage(currentPage - 1)}
                    disabled={currentPage <= 1}
                >
                    &lt; Previous
                </button>
                <span class="text-sm text-gray-700">Page {currentPage}</span>
                <button 
                    class="btn btn-outline btn-sm"
                    on:click={() => goToPage(currentPage + 1)}
                    disabled={!hasMore} 
                >
                    Next &gt;
                </button>
            </div>
        {/if}
    </div>
</div>

<style>
    /* Add transition for smooth appearance/disappearance */
    .fixed {
        transition: opacity 0.3s ease-in-out;
        z-index: 50;
    }
</style> 