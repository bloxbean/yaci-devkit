<script lang="ts">
    import { page } from '$app/stores';
    import { goto } from '$app/navigation';
    import { onMount } from 'svelte';
    import { lovelaceToAda, truncate } from '$lib/util';
    import TruncateCopy from '../../../components/TruncateCopy.svelte';
    import EmptyState from '../../../components/EmptyState.svelte';

    export let data: any;

    const REWARDS_TAB = 0;
    const OTHER_REWARDS_TAB = 1;
    const WITHDRAWALS_TAB = 2;

    let activeTabIndex = REWARDS_TAB;
    let loading = false;
    let address = '';
    let rewards: any[] = [];
    let rewardRest: any[] = [];
    let withdrawals: any[] = [];
    let currentPage = 1;
    let count = 15;
    let order: 'asc' | 'desc' = 'desc';

    $: activeData = activeTabIndex === REWARDS_TAB
        ? rewards
        : activeTabIndex === OTHER_REWARDS_TAB
        ? rewardRest
        : withdrawals;

    $: hasMore = activeData.length === count;

    function goToPage(newPage: number) {
        if (newPage < 1 || loading) return;
        loading = true;
        const tab = activeTabIndex === REWARDS_TAB ? 'rewards' : activeTabIndex === OTHER_REWARDS_TAB ? 'other_rewards' : 'withdrawals';
        goto(`/stake/${address}?page=${newPage}&count=${count}&order=${order}&tab=${tab}`);
    }

    function switchTab(tabIndex: number) {
        activeTabIndex = tabIndex;
        loading = true;
        const tab = tabIndex === REWARDS_TAB ? 'rewards' : tabIndex === OTHER_REWARDS_TAB ? 'other_rewards' : 'withdrawals';
        goto(`/stake/${address}?page=1&count=${count}&order=${order}&tab=${tab}`);
    }

    function syncTab(urlTab: string) {
        if (urlTab === 'other_rewards') activeTabIndex = OTHER_REWARDS_TAB;
        else if (urlTab === 'withdrawals') activeTabIndex = WITHDRAWALS_TAB;
        else activeTabIndex = REWARDS_TAB;
    }

    onMount(() => {
        syncTab($page.url.searchParams.get('tab') || 'rewards');
    });

    $: if (data && $page) {
        loading = false;
        address = data.address;
        rewards = data.rewards || [];
        rewardRest = data.rewardRest || [];
        withdrawals = data.withdrawals || [];
        currentPage = data.currentPage;
        count = data.count;
        order = data.order;
        syncTab($page.url.searchParams.get('tab') || 'rewards');
    }
</script>

<section class="py-8 px-4 md:px-6 min-h-screen">
    <div class="container mx-auto max-w-6xl">
        <!-- Header -->
        <div class="bg-base-100 rounded-xl shadow-sm border border-base-300 overflow-hidden mb-8">
            <div class="bg-base-200/50 p-6 border-b border-base-200">
                <h2 class="text-xs font-bold text-base-content/60 uppercase tracking-wider mb-2">
                    Stake Address
                </h2>
                <div class="flex items-center gap-3">
                    <TruncateCopy text={address} max={60} />
                </div>
            </div>
        </div>

        <!-- Tabs -->
        <div class="mb-6 bg-base-200 rounded-xl p-2">
            <div class="flex flex-wrap gap-2 border-b border-base-300">
                <button
                    class="px-5 py-3 text-sm font-medium transition-colors duration-200 border-b-2 {activeTabIndex === REWARDS_TAB
                        ? 'border-blue-600 text-blue-600'
                        : 'border-transparent text-base-content/60 hover:text-base-content/80 hover:border-gray-300'}"
                    on:click={() => switchTab(REWARDS_TAB)}
                >
                    Rewards
                    {#if rewards.length > 0}
                        <span class="ml-1 bg-base-200 text-base-content/70 py-0.5 px-2 rounded-full text-xs">{rewards.length}</span>
                    {/if}
                </button>

                <button
                    class="px-5 py-3 text-sm font-medium transition-colors duration-200 border-b-2 {activeTabIndex === OTHER_REWARDS_TAB
                        ? 'border-blue-600 text-blue-600'
                        : 'border-transparent text-base-content/60 hover:text-base-content/80 hover:border-gray-300'}"
                    on:click={() => switchTab(OTHER_REWARDS_TAB)}
                >
                    Other Rewards
                    {#if rewardRest.length > 0}
                        <span class="ml-1 bg-base-200 text-base-content/70 py-0.5 px-2 rounded-full text-xs">{rewardRest.length}</span>
                    {/if}
                </button>

                <button
                    class="px-5 py-3 text-sm font-medium transition-colors duration-200 border-b-2 {activeTabIndex === WITHDRAWALS_TAB
                        ? 'border-blue-600 text-blue-600'
                        : 'border-transparent text-base-content/60 hover:text-base-content/80 hover:border-gray-300'}"
                    on:click={() => switchTab(WITHDRAWALS_TAB)}
                >
                    Withdrawals
                    {#if withdrawals.length > 0}
                        <span class="ml-1 bg-base-200 text-base-content/70 py-0.5 px-2 rounded-full text-xs">{withdrawals.length}</span>
                    {/if}
                </button>
            </div>
        </div>

        <!-- Tab Content -->
        <div class="bg-base-100 rounded-xl shadow-sm border border-base-300 p-6 min-h-[300px] relative">
            {#if loading}
                <div class="absolute inset-0 bg-base-100 bg-opacity-75 flex justify-center items-center z-10">
                    <span class="loading loading-spinner loading-lg"></span>
                </div>
            {/if}

            <!-- Rewards Tab -->
            {#if activeTabIndex === REWARDS_TAB}
                <div class="w-full animate-fade-in">
                    {#if rewards.length === 0}
                        <EmptyState title="No Rewards" message="No pool rewards found for this stake address." />
                    {:else}
                        <div class="overflow-x-auto">
                            <table class="min-w-full divide-y divide-base-300">
                                <thead class="bg-base-200">
                                    <tr>
                                        <th class="px-4 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Epoch</th>
                                        <th class="px-4 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Pool ID</th>
                                        <th class="px-4 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Type</th>
                                        <th class="px-4 py-3 text-right text-xs font-medium text-base-content/60 uppercase tracking-wider">Amount</th>
                                    </tr>
                                </thead>
                                <tbody class="bg-base-100 divide-y divide-base-300">
                                    {#each rewards as reward}
                                        <tr class="hover:bg-base-200">
                                            <td class="px-4 py-4 text-sm text-base-content font-medium">
                                                {reward.epoch}
                                            </td>
                                            <td class="px-4 py-4 text-sm">
                                                <a href="/pools/{reward.pool_id}" class="font-mono text-blue-600 hover:underline" title={reward.pool_id}>
                                                    {truncate(reward.pool_id, 20, '...')}
                                                </a>
                                            </td>
                                            <td class="px-4 py-4 text-sm">
                                                {#if reward.type === 'member'}
                                                    <span class="badge badge-info badge-sm">MEMBER</span>
                                                {:else if reward.type === 'leader'}
                                                    <span class="badge badge-warning badge-sm">LEADER</span>
                                                {:else}
                                                    <span class="badge badge-ghost badge-sm">{reward.type}</span>
                                                {/if}
                                            </td>
                                            <td class="px-4 py-4 text-sm text-right font-medium text-base-content">
                                                {lovelaceToAda(reward.amount, 6)} <span class="text-base-content/60">ADA</span>
                                            </td>
                                        </tr>
                                    {/each}
                                </tbody>
                            </table>
                        </div>
                    {/if}
                </div>
            {/if}

            <!-- Other Rewards Tab -->
            {#if activeTabIndex === OTHER_REWARDS_TAB}
                <div class="w-full animate-fade-in">
                    {#if rewardRest.length === 0}
                        <EmptyState title="No Other Rewards" message="No treasury or reserve rewards found for this stake address." />
                    {:else}
                        <div class="overflow-x-auto">
                            <table class="min-w-full divide-y divide-base-300">
                                <thead class="bg-base-200">
                                    <tr>
                                        <th class="px-4 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Epoch</th>
                                        <th class="px-4 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Type</th>
                                        <th class="px-4 py-3 text-right text-xs font-medium text-base-content/60 uppercase tracking-wider">Amount</th>
                                    </tr>
                                </thead>
                                <tbody class="bg-base-100 divide-y divide-base-300">
                                    {#each rewardRest as reward}
                                        <tr class="hover:bg-base-200">
                                            <td class="px-4 py-4 text-sm text-base-content font-medium">
                                                {reward.epoch}
                                            </td>
                                            <td class="px-4 py-4 text-sm">
                                                <span class="badge badge-ghost badge-sm">{reward.type}</span>
                                            </td>
                                            <td class="px-4 py-4 text-sm text-right font-medium text-base-content">
                                                {lovelaceToAda(reward.amount, 6)} <span class="text-base-content/60">ADA</span>
                                            </td>
                                        </tr>
                                    {/each}
                                </tbody>
                            </table>
                        </div>
                    {/if}
                </div>
            {/if}

            <!-- Withdrawals Tab -->
            {#if activeTabIndex === WITHDRAWALS_TAB}
                <div class="w-full animate-fade-in">
                    {#if withdrawals.length === 0}
                        <EmptyState title="No Withdrawals" message="No withdrawals found for this stake address." />
                    {:else}
                        <div class="overflow-x-auto">
                            <table class="min-w-full divide-y divide-base-300">
                                <thead class="bg-base-200">
                                    <tr>
                                        <th class="px-4 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Tx Hash</th>
                                        <th class="px-4 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Epoch</th>
                                        <th class="px-4 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Block</th>
                                        <th class="px-4 py-3 text-right text-xs font-medium text-base-content/60 uppercase tracking-wider">Amount</th>
                                    </tr>
                                </thead>
                                <tbody class="bg-base-100 divide-y divide-base-300">
                                    {#each withdrawals as w}
                                        <tr class="hover:bg-base-200">
                                            <td class="px-4 py-4 text-sm">
                                                <a href="/transactions/{w.tx_hash}" class="text-blue-600 hover:underline">
                                                    {truncate(w.tx_hash, 20, '...')}
                                                </a>
                                            </td>
                                            <td class="px-4 py-4 text-sm text-base-content">
                                                {w.epoch}
                                            </td>
                                            <td class="px-4 py-4 text-sm">
                                                <a href="/blocks/{w.block_number}" class="text-blue-500 hover:underline">
                                                    {w.block_number}
                                                </a>
                                            </td>
                                            <td class="px-4 py-4 text-sm text-right font-medium text-base-content">
                                                {lovelaceToAda(w.amount, 6)} <span class="text-base-content/60">ADA</span>
                                            </td>
                                        </tr>
                                    {/each}
                                </tbody>
                            </table>
                        </div>
                    {/if}
                </div>
            {/if}

            <!-- Pagination -->
            {#if activeData.length > 0}
                <div class="mt-6 flex justify-between items-center">
                    <button
                        class="btn btn-outline btn-sm"
                        on:click={() => goToPage(currentPage - 1)}
                        disabled={currentPage <= 1}
                    >
                        &lt; Previous
                    </button>
                    <span class="text-sm text-base-content/80">Page {currentPage}</span>
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
</section>

<style>
    .animate-fade-in {
        animation: fadeIn 0.3s ease-in-out;
    }
    @keyframes fadeIn {
        from {
            opacity: 0;
            transform: translateY(5px);
        }
        to {
            opacity: 1;
            transform: translateY(0);
        }
    }
</style>
