<script lang="ts">
    import { page } from '$app/stores';
    import { onMount } from 'svelte';
    import { formatAda, formatLovelace, truncate } from '$lib/util';
    import { goto } from '$app/navigation';

    export let data: {
        error: string | null;
        poolRewards: Array<{ address: string; earned_epoch: number; type: string; pool_id: string; amount: number; spendable_epoch: number; }>;
        restRewards: Array<{ address: string; amount: number; type: string; earned_epoch: number; spendable_epoch: number; }>;
        unclaimedRewards: Array<{ address: string; amount: number; type: string; earned_epoch: number; spendable_epoch: number; }>;
        epoch: number;
        currentPage: number; // Server provides current page
        count: number;
    };

    interface PoolReward { address: string; earned_epoch: number; type: string; pool_id: string; amount: number; spendable_epoch: number; }
    
    interface RestReward {
        address: string;
        type: string;
        amount: number;
        earned_epoch: number;
        spendable_epoch: number;
    }

    let currentTab: 'pool' | 'rest' | 'unclaimed' = 'pool';
    let loading = false;
    let error: string | null = null;
    
    let poolRewards: PoolReward[] = [];
    let restRewards: RestReward[] = [];
    let unclaimedRewards: RestReward[] = [];
    
    let currentPage = 1;
    const count = data?.count || 15; // Use count from data or default
    const epoch = data?.epoch;

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

    // Cleanup timeout on component destroy
    onMount(() => {
        const initialTab = $page.url.searchParams.get('tab') as 'pool' | 'rest' | 'unclaimed' || 'pool';
        currentTab = initialTab;
        currentPage = data?.currentPage || 1;
        
        if (data) {
            error = data.error;
            poolRewards = data.poolRewards || [];
            restRewards = data.restRewards || [];
            unclaimedRewards = data.unclaimedRewards || [];
        }
        // Initial loading is handled by server, so set loading false
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
        const url = `/rewards/epoch/${epoch}?page=${newPage}&count=${count}&tab=${currentTab}`;
        goto(url);
    }
    
    // Determine if the currently active tab *likely* has more data
    $: hasMore = 
        (currentTab === 'pool' && poolRewards.length === count) ||
        (currentTab === 'rest' && restRewards.length === count) ||
        (currentTab === 'unclaimed' && unclaimedRewards.length === count);

    // Update component state when data or URL changes (after navigation)
    $: if (data && $page && !loading) {
        console.log('[DEBUG] Reactive update triggered. New data received:', JSON.stringify(data));
        
        const urlTab = $page.url.searchParams.get('tab') as 'pool' | 'rest' | 'unclaimed' || 'pool';
        currentTab = urlTab;
        currentPage = data.currentPage; // Ensure currentPage is updated from server data
        error = data.error;
        poolRewards = data.poolRewards || [];
        restRewards = data.restRewards || [];
        unclaimedRewards = data.unclaimedRewards || [];
        
        // No need to update count, it's constant from initial load
        // No need to set loading = false here, goto handles it implicitly by providing new data
        
        // Log state *after* update
        console.log(`[DEBUG] State Updated - Tab: ${currentTab}, Pool Len: ${poolRewards.length}, Rest Len: ${restRewards.length}, Unclaimed Len: ${unclaimedRewards.length}, Count: ${count}, Current Page: ${currentPage}`);
    }

    // Log hasMore whenever its dependencies change
    // $: console.log(`[DEBUG] hasMore calculated: ${hasMore} (Based on Tab: ${currentTab}, Pool Len: ${poolRewards.length}, Rest Len: ${restRewards.length}, Unclaimed Len: ${unclaimedRewards.length}, Count: ${count})`); // Keep this log commented for now unless needed

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
    <h1 class="text-3xl font-bold mb-6">Rewards for Epoch {epoch}</h1>

    {#if error && !loading} 
      <div class="alert {error.startsWith('Warning') ? 'alert-warning' : 'alert-error'} mb-4 shadow-lg">
          <div>
              <svg xmlns="http://www.w3.org/2000/svg" class="stroke-current flex-shrink-0 h-6 w-6" fill="none" viewBox="0 0 24 24">
                {#if error.startsWith('Warning')}
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                {:else}
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
                {/if}
            </svg>
            <span>{error}</span>
          </div>
      </div>
    {/if}

    <!-- Tabs with counts -->
    <div class="tabs tabs-boxed bg-base-200 mb-6 p-2 rounded-lg">
         <button 
            class="tab tab-lg flex-1 {currentTab === 'pool' ? 'tab-active bg-primary text-primary-content' : ''}"
            on:click={() => { currentTab = 'pool'; goToPage(1); /* Go to page 1 of new tab */ }}
        >
            Pool Rewards ({poolRewards.length})
        </button>
        <button 
            class="tab tab-lg flex-1 {currentTab === 'rest' ? 'tab-active bg-primary text-primary-content' : ''}"
            on:click={() => { currentTab = 'rest'; goToPage(1); /* Go to page 1 of new tab */ }}
        >
             Other Rewards ({restRewards.length})
        </button>
         <button 
            class="tab tab-lg flex-1 {currentTab === 'unclaimed' ? 'tab-active bg-primary text-primary-content' : ''}"
            on:click={() => { currentTab = 'unclaimed'; goToPage(1); /* Go to page 1 of new tab */ }}
        >
            Unclaimed Rewards ({unclaimedRewards.length})
        </button>
    </div>

    <!-- Pagination Controls (Top) -->
    {#if !loading && (poolRewards.length > 0 || restRewards.length > 0 || unclaimedRewards.length > 0)} 
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
        {:else if currentTab === 'pool'}
             <h2 class="text-xl font-semibold mb-4">Pool Rewards</h2>
             {#if !poolRewards || poolRewards.length === 0}
                 <p class="text-gray-500">No pool rewards found for this epoch.</p>
             {:else}
                 <div class="overflow-x-auto">
                    <table class="min-w-full divide-y divide-gray-200">
                        <thead class="bg-gray-50">
                            <tr>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Pool ID</th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Stake Address</th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Type</th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Amount (ADA)</th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Earned Epoch</th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Spendable Epoch</th>
                            </tr>
                        </thead>
                        <tbody class="bg-white divide-y divide-gray-200">
                            {#each poolRewards as reward (reward.pool_id + reward.address + reward.earned_epoch)} 
                                <tr class="hover:bg-gray-50">
                                    <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
                                        <div class="flex items-center gap-2">
                                            <a href="/pools/{reward.pool_id}" class="text-blue-500 hover:underline">{truncate(reward.pool_id, 20, '...')}</a>
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
                                     <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                         <div class="flex items-center gap-2">
                                             <a href="/rewards/account/{reward.address}" class="text-blue-500 hover:underline">{truncate(reward.address, 20, '...')}</a>
                                             <button 
                                                 class="text-gray-400 hover:text-gray-600" 
                                                 on:click={() => copyToClipboard(reward.address, 'Stake address copied to clipboard!')}
                                                 title="Copy stake address"
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
                                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{reward.earned_epoch}</td>
                                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{reward.spendable_epoch}</td>
                                </tr>
                            {/each}
                        </tbody>
                    </table>
                </div>
             {/if}
        {:else if currentTab === 'rest'}
             <h2 class="text-xl font-semibold mb-4">Other Rewards (Treasury, Reserves, etc.)</h2>
             {#if !restRewards || restRewards.length === 0}
                 <p class="text-gray-500">No other rewards found for this epoch.</p>
             {:else}
                 <div class="overflow-x-auto">
                    <table class="min-w-full divide-y divide-gray-200">
                        <thead class="bg-gray-50">
                            <tr>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Stake Address</th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Type</th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Amount (ADA)</th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Earned Epoch</th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Spendable Epoch</th>
                            </tr>
                        </thead>
                        <tbody class="bg-white divide-y divide-gray-200">
                            {#each restRewards as reward, i (reward.address + reward.type + reward.earned_epoch + i)} 
                                <tr class="hover:bg-gray-50">
                                     <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                         <div class="flex items-center gap-2">
                                             <a href="/rewards/account/{reward.address}" class="text-blue-500 hover:underline">{truncate(reward.address, 20, '...')}</a>
                                             <button 
                                                 class="text-gray-400 hover:text-gray-600" 
                                                 on:click={() => copyToClipboard(reward.address, 'Stake address copied to clipboard!')}
                                                 title="Copy stake address"
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
                                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{reward.earned_epoch}</td>
                                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{reward.spendable_epoch}</td>
                                </tr>
                            {/each}
                        </tbody>
                    </table>
                </div>
             {/if}
        {:else if currentTab === 'unclaimed'}
             <h2 class="text-xl font-semibold mb-4">Unclaimed Other Rewards</h2>
             {#if !unclaimedRewards || unclaimedRewards.length === 0}
                 <p class="text-gray-500">No unclaimed rewards found for this epoch.</p>
             {:else}
                 <div class="overflow-x-auto">
                     <table class="min-w-full divide-y divide-gray-200">
                        <thead class="bg-gray-50">
                            <tr>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Stake Address</th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Type</th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Amount (ADA)</th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Earned Epoch</th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Spendable Epoch</th>
                            </tr>
                        </thead>
                         <tbody class="bg-white divide-y divide-gray-200">
                            {#each unclaimedRewards as reward, i (reward.address + reward.type + reward.earned_epoch + i)} 
                                <tr class="hover:bg-gray-50">
                                     <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                         <div class="flex items-center gap-2">
                                             <a href="/rewards/account/{reward.address}" class="text-blue-500 hover:underline">{truncate(reward.address, 20, '...')}</a>
                                             <button 
                                                 class="text-gray-400 hover:text-gray-600" 
                                                 on:click={() => copyToClipboard(reward.address, 'Stake address copied to clipboard!')}
                                                 title="Copy stake address"
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
                                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{reward.earned_epoch}</td>
                                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{reward.spendable_epoch}</td>
                                </tr>
                            {/each}
                        </tbody>
                    </table>
                 </div>
             {/if}
        {/if}

        <!-- Pagination Controls (Bottom) -->
        {#if !loading && (poolRewards.length > 0 || restRewards.length > 0 || unclaimedRewards.length > 0)} 
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