<script lang="ts">
    import { page } from '$app/stores';
    import { goto } from '$app/navigation';
    import { onMount } from 'svelte';
    import { formatAda, formatLovelace, truncate } from '$lib/util';

    export let data: {
        error: string | null;
        poolRewards: Array<{
            epoch: number;
            pool_id: string;
            amount: number;
            type: string;
        }>;
        restRewards: Array<{
            epoch: number;
            amount: number;
            type: string;
        }>;
        address: string;
        currentPage: number;
        count: number;
        order: 'asc' | 'desc';
    };

    // Interfaces matching server load function
    interface AccountPoolReward {
        epoch: number;
        pool_id: string;
        amount: number;
        type: string;
    }

    interface AccountRestReward {
        epoch: number;
        amount: number;
        type: string;
    }

    let currentTab: 'pool' | 'rest' = 'pool';
    let loading = false;
    let error: string | null = null;
    let poolRewards: AccountPoolReward[] = [];
    let restRewards: AccountRestReward[] = [];
    let address = '';
    let currentPage = 1;
    let count = 15;
    let order: 'asc' | 'desc' = 'desc';

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
        // Set initial tab based on URL query param or default
        const initialTab = $page.url.searchParams.get('tab') as 'pool' | 'rest' || 'pool';
        currentTab = initialTab;

        if (data) {
            error = data.error;
            poolRewards = data.poolRewards;
            restRewards = data.restRewards;
            address = data.address;
            currentPage = data.currentPage;
            count = data.count;
            order = data.order;
        }

        return () => {
            if (toastTimeout) {
                clearTimeout(toastTimeout);
            }
        };
    });

    // Function to navigate to a different page
    function goToPage(newPage: number) {
        if (newPage < 1) return;
        loading = true;
        // Include currentTab in the URL
        const url = `/rewards/account/${address}?page=${newPage}&count=${count}&order=${order}&tab=${currentTab}`;
        goto(url);
    }

    // Function to change sorting order
    function changeOrder(newOrder: 'asc' | 'desc') {
        loading = true;
        // Include currentTab in the URL
        const url = `/rewards/account/${address}?page=1&count=${count}&order=${newOrder}&tab=${currentTab}`;
        goto(url);
    }

    // Reactive check for pagination based on current tab
    $: hasMore = currentTab === 'pool' 
        ? poolRewards.length === count 
        : restRewards.length === count;

    // Update component state when data or URL changes
    $: if (data && $page) {
        loading = false; // Reset loading state
        error = data.error;
        poolRewards = data.poolRewards;
        restRewards = data.restRewards;
        address = data.address;
        currentPage = data.currentPage;
        count = data.count;
        order = data.order;
        // Update tab based on URL in case of direct navigation/refresh
        const urlTab = $page.url.searchParams.get('tab') as 'pool' | 'rest' || 'pool';
        currentTab = urlTab;
    }

    // Log state changes for debugging
    $: console.log(`Current Tab: ${currentTab}, Pool Rewards: ${poolRewards?.length}, Rest Rewards: ${restRewards?.length}`);
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
    <h1 class="text-3xl font-bold mb-1">Account Rewards History</h1>
    <p class="mb-6 text-gray-600 break-all">For: {address}</p>

    {#if error}
        <div class="alert alert-error mb-4 shadow-lg">
            <div>
                <svg xmlns="http://www.w3.org/2000/svg" class="stroke-current flex-shrink-0 h-6 w-6" fill="none" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
                <span>Error fetching data: {error}</span>
            </div>
        </div>
    {/if}

    <!-- Tabs -->
    <div class="tabs tabs-boxed bg-base-200 mb-6 p-2 rounded-lg">
        <button 
            class="tab tab-lg flex-1 {currentTab === 'pool' ? 'tab-active bg-primary text-primary-content' : ''}"
            on:click={() => { currentTab = 'pool'; changeOrder(order); /* Reload on tab click to reset page=1 */ }}
        >
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2" viewBox="0 0 20 20" fill="currentColor">
                <path d="M4 4a2 2 0 00-2 2v1h16V6a2 2 0 00-2-2H4z" />
                <path fill-rule="evenodd" d="M18 9H2v5a2 2 0 002 2h12a2 2 0 002-2V9zM4 13a1 1 0 011-1h1a1 1 0 110 2H5a1 1 0 01-1-1zm5-1a1 1 0 100 2h1a1 1 0 100-2H9z" clip-rule="evenodd" />
            </svg>
            Pool Rewards
        </button>
        <button 
            class="tab tab-lg flex-1 {currentTab === 'rest' ? 'tab-active bg-primary text-primary-content' : ''}"
            on:click={() => { currentTab = 'rest'; changeOrder(order); /* Reload on tab click to reset page=1 */ }}
        >
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M4 4a2 2 0 012-2h8a2 2 0 012 2v12a1 1 0 110 2h-3a1 1 0 01-1-1v-2a1 1 0 00-1-1H9a1 1 0 00-1 1v2a1 1 0 01-1 1H4a1 1 0 110-2V4zm3 1h6v4H7V5z" clip-rule="evenodd" />
            </svg>
            Other Rewards
        </button>
    </div>

    <!-- Sorting Controls -->
    <div class="mb-4 text-right">
        <span class="mr-2 text-sm text-gray-600">Sort by Epoch:</span>
        <button 
            class="btn btn-sm {order === 'desc' ? 'btn-primary' : 'btn-outline'}" 
            on:click={() => changeOrder('desc')}
        >Desc</button>
        <button 
            class="btn btn-sm ml-2 {order === 'asc' ? 'btn-primary' : 'btn-outline'}" 
            on:click={() => changeOrder('asc')}
        >Asc</button>
    </div>

    {#if loading}
        <div class="flex justify-center items-center h-32">
            <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-gray-900"></div>
        </div>
    {:else}
        <!-- Content Area -->
        <div class="bg-white shadow-md rounded-lg p-4 min-h-[400px]">
            {#if currentTab === 'pool'}
                <h2 class="text-xl font-semibold mb-4">Pool Rewards History</h2>
                {#if !poolRewards || poolRewards.length === 0}
                    <p class="text-gray-500">No pool rewards found for this account.</p>
                {:else}
                    <div class="overflow-x-auto">
                        <table class="min-w-full divide-y divide-gray-200">
                            <thead class="bg-gray-50">
                                <tr>
                                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Epoch</th>
                                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Pool ID</th>
                                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Type</th>
                                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Amount (ADA)</th>
                                </tr>
                            </thead>
                            <tbody class="bg-white divide-y divide-gray-200">
                                {#each poolRewards as reward (reward.epoch + reward.pool_id)} 
                                    <tr class="hover:bg-gray-50">
                                        <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{reward.epoch}</td>
                                        <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                                            <div class="flex items-center gap-2">
                                                <a href="/pools/{reward.pool_id}" class="text-blue-500 hover:underline">{truncate(reward.pool_id, 40, '...')}</a>
                                                <button 
                                                    class="text-gray-400 hover:text-gray-600" 
                                                    on:click={() => copyToClipboard(reward.pool_id, 'Pool ID copied to clipboard!')}
                                                    title="Copy pool ID"
                                                >
                                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
                                                    </svg>
                                                </button>
                                            </div>
                                        </td>
                                        <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500 capitalize">{reward.type}</td>
                                        <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500 tooltip" data-tip={formatLovelace(reward.amount)}>
                                            {formatAda(reward.amount)}
                                        </td>
                                    </tr>
                                {/each}
                            </tbody>
                        </table>
                    </div>
                {/if}
            
            {:else if currentTab === 'rest'}
                <h2 class="text-xl font-semibold mb-4">Other Rewards History</h2>
                {#if !restRewards || restRewards.length === 0}
                    <p class="text-gray-500">No other rewards found for this account.</p>
                {:else}
                    <div class="overflow-x-auto">
                        <table class="min-w-full divide-y divide-gray-200">
                            <thead class="bg-gray-50">
                                <tr>
                                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Epoch</th>
                                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Type</th>
                                    <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Amount (ADA)</th>
                                </tr>
                            </thead>
                            <tbody class="bg-white divide-y divide-gray-200">
                                {#each restRewards as reward, i (reward.epoch + reward.type + reward.amount + i)} 
                                    <tr class="hover:bg-gray-50">
                                        <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{reward.epoch}</td>
                                        <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500 capitalize">{reward.type}</td>
                                        <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500 tooltip" data-tip={formatLovelace(reward.amount)}>
                                            {formatAda(reward.amount)}
                                        </td>
                                    </tr>
                                {/each}
                            </tbody>
                        </table>
                    </div>
                {/if}
            {/if}

            <!-- Pagination Controls -->
            <div class="mt-6 flex justify-between items-center">
                <button 
                    class="btn btn-outline"
                    on:click={() => goToPage(currentPage - 1)}
                    disabled={currentPage <= 1}
                >
                    &lt; Previous
                </button>
                <span class="text-sm text-gray-700">Page {currentPage}</span>
                <button 
                    class="btn btn-outline"
                    on:click={() => goToPage(currentPage + 1)}
                    disabled={!hasMore}
                >
                    Next &gt;
                </button>
            </div>
        </div>
    {/if}
</div>

<style>
    /* Add transition for smooth appearance/disappearance */
    .fixed {
        transition: opacity 0.3s ease-in-out;
        z-index: 50;
    }
</style> 