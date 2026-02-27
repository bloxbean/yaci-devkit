<script lang="ts">
    import { page } from '$app/stores';
    import { goto } from '$app/navigation';
    import { onMount } from 'svelte';
    import { truncate, getDate } from '$lib/util';
    import { parseUnit } from '$lib/utils/asset';
    import TruncateCopy from '../../../../components/TruncateCopy.svelte';
    import AddressLink from '../../../../components/AddressLink.svelte';
    import AmountBadges from '../../../../components/AmountBadges.svelte';
    import EmptyState from '../../../../components/EmptyState.svelte';

    export let data: any;

    const HISTORY_TAB = 0;
    const TXS_TAB = 1;
    const UTXOS_TAB = 2;

    let activeTabIndex = HISTORY_TAB;
    let loading = false;
    let unit = '';
    let supply: any = null;
    let history: any[] = [];
    let transactions: any[] = [];
    let utxos: any[] = [];
    let currentPage = 1;
    let count = 15;
    let order = 'desc';

    $: parsed = unit ? parseUnit(unit) : { policyId: '', assetNameHex: '', assetNameUtf8: '' };
    $: fingerprint = history.length > 0 ? history[0].fingerprint : '';
    $: supplyValue = supply?.supply ?? '—';

    $: hasMore = activeTabIndex === HISTORY_TAB
        ? history.length === count
        : activeTabIndex === TXS_TAB
        ? transactions.length === count
        : utxos.length === count;

    function goToPage(newPage: number) {
        if (newPage < 1 || loading) return;
        loading = true;
        const tab = activeTabIndex === HISTORY_TAB ? 'history' : activeTabIndex === TXS_TAB ? 'transactions' : 'utxos';
        goto(`/assets/unit/${unit}?page=${newPage}&count=${count}&order=${order}&tab=${tab}`);
    }

    function switchTab(tabIndex: number) {
        activeTabIndex = tabIndex;
        loading = true;
        const tab = tabIndex === HISTORY_TAB ? 'history' : tabIndex === TXS_TAB ? 'transactions' : 'utxos';
        goto(`/assets/unit/${unit}?page=1&count=${count}&order=${order}&tab=${tab}`);
    }

    onMount(() => {
        const urlTab = $page.url.searchParams.get('tab') || 'history';
        if (urlTab === 'transactions') activeTabIndex = TXS_TAB;
        else if (urlTab === 'utxos') activeTabIndex = UTXOS_TAB;
        else activeTabIndex = HISTORY_TAB;
    });

    $: if (data && $page) {
        loading = false;
        unit = data.unit;
        supply = data.supply;
        history = data.history || [];
        transactions = data.transactions || [];
        utxos = data.utxos || [];
        currentPage = data.currentPage;
        count = data.count;
        order = data.order;

        const urlTab = $page.url.searchParams.get('tab') || 'history';
        if (urlTab === 'transactions') activeTabIndex = TXS_TAB;
        else if (urlTab === 'utxos') activeTabIndex = UTXOS_TAB;
        else activeTabIndex = HISTORY_TAB;
    }
</script>

<section class="py-8 px-4 md:px-6 min-h-screen">
    <div class="container mx-auto max-w-6xl">
        <!-- Header -->
        <div class="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden mb-8">
            <div class="bg-gray-50/50 p-6 border-b border-gray-100">
                <h2 class="text-xs font-bold text-gray-500 uppercase tracking-wider mb-2">
                    Asset
                </h2>
                <div class="text-lg font-semibold text-gray-900 mb-1">
                    {parsed.assetNameUtf8 || parsed.assetNameHex || '(empty name)'}
                </div>
                <div class="flex items-center gap-3">
                    <TruncateCopy text={unit} max={60} />
                </div>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-3 divide-y md:divide-y-0 md:divide-x divide-gray-100">
                <div class="p-6">
                    <div class="text-xs font-bold text-gray-400 uppercase tracking-wider mb-1">
                        Policy ID
                    </div>
                    <div class="text-gray-900 text-sm">
                        <a href="/assets/policy/{parsed.policyId}" class="text-blue-600 hover:underline">
                            <TruncateCopy text={parsed.policyId} max={24} />
                        </a>
                    </div>
                </div>

                <div class="p-6">
                    <div class="text-xs font-bold text-gray-400 uppercase tracking-wider mb-1">
                        Fingerprint
                    </div>
                    <div class="text-gray-900 text-sm">
                        {#if fingerprint}
                            <TruncateCopy text={fingerprint} max={24} />
                        {:else}
                            <span class="text-gray-400">—</span>
                        {/if}
                    </div>
                </div>

                <div class="p-6">
                    <div class="text-xs font-bold text-gray-400 uppercase tracking-wider mb-1">
                        Total Supply
                    </div>
                    <div class="text-gray-900 font-medium text-lg">
                        {supplyValue}
                    </div>
                </div>
            </div>
        </div>

        <!-- Tabs -->
        <div class="mb-6 bg-gray-50 rounded-xl p-2">
            <div class="flex flex-wrap gap-2 border-b border-gray-200">
                <button
                    class="px-5 py-3 text-sm font-medium transition-colors duration-200 border-b-2 {activeTabIndex === HISTORY_TAB
                        ? 'border-blue-600 text-blue-600'
                        : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'}"
                    on:click={() => switchTab(HISTORY_TAB)}
                >
                    Mint/Burn History
                    {#if history.length > 0}
                        <span class="ml-1 bg-gray-100 text-gray-600 py-0.5 px-2 rounded-full text-xs">{history.length}</span>
                    {/if}
                </button>

                <button
                    class="px-5 py-3 text-sm font-medium transition-colors duration-200 border-b-2 {activeTabIndex === TXS_TAB
                        ? 'border-blue-600 text-blue-600'
                        : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'}"
                    on:click={() => switchTab(TXS_TAB)}
                >
                    Transactions
                    {#if transactions.length > 0}
                        <span class="ml-1 bg-gray-100 text-gray-600 py-0.5 px-2 rounded-full text-xs">{transactions.length}</span>
                    {/if}
                </button>

                <button
                    class="px-5 py-3 text-sm font-medium transition-colors duration-200 border-b-2 {activeTabIndex === UTXOS_TAB
                        ? 'border-blue-600 text-blue-600'
                        : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'}"
                    on:click={() => switchTab(UTXOS_TAB)}
                >
                    UTXOs
                    {#if utxos.length > 0}
                        <span class="ml-1 bg-gray-100 text-gray-600 py-0.5 px-2 rounded-full text-xs">{utxos.length}</span>
                    {/if}
                </button>
            </div>
        </div>

        <!-- Tab Content -->
        <div class="bg-white rounded-xl shadow-sm border border-gray-200 p-6 min-h-[300px] relative">
            {#if loading}
                <div class="absolute inset-0 bg-white bg-opacity-75 flex justify-center items-center z-10">
                    <span class="loading loading-spinner loading-lg"></span>
                </div>
            {/if}

            <!-- History Tab -->
            {#if activeTabIndex === HISTORY_TAB}
                <div class="w-full animate-fade-in">
                    {#if history.length === 0}
                        <EmptyState title="No Mint/Burn History" message="No mint or burn events found for this asset." />
                    {:else}
                        <div class="overflow-x-auto">
                            <table class="min-w-full divide-y divide-gray-200">
                                <thead class="bg-gray-50">
                                    <tr>
                                        <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Tx Hash</th>
                                        <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Block</th>
                                        <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Type</th>
                                        <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Quantity</th>
                                        <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Time</th>
                                    </tr>
                                </thead>
                                <tbody class="bg-white divide-y divide-gray-200">
                                    {#each history as event (event.tx_hash + event.quantity)}
                                        <tr class="hover:bg-gray-50">
                                            <td class="px-4 py-4 text-sm">
                                                <a href="/transactions/{event.tx_hash}" class="text-blue-600 hover:underline">
                                                    {truncate(event.tx_hash, 20, '...')}
                                                </a>
                                            </td>
                                            <td class="px-4 py-4 text-sm text-gray-500">
                                                <a href="/blocks/{event.block_number}" class="text-blue-500 hover:underline">
                                                    {event.block_number}
                                                </a>
                                            </td>
                                            <td class="px-4 py-4 text-sm">
                                                {#if event.mint_type === 'MINT'}
                                                    <span class="badge badge-success badge-sm">MINT</span>
                                                {:else}
                                                    <span class="badge badge-error badge-sm">BURN</span>
                                                {/if}
                                            </td>
                                            <td class="px-4 py-4 text-sm font-medium text-gray-900">
                                                {event.quantity}
                                            </td>
                                            <td class="px-4 py-4 text-sm text-gray-500">
                                                {getDate(event.block_time)}
                                            </td>
                                        </tr>
                                    {/each}
                                </tbody>
                            </table>
                        </div>

                        <div class="mt-6 flex justify-between items-center">
                            <button class="btn btn-outline btn-sm" on:click={() => goToPage(currentPage - 1)} disabled={currentPage <= 1}>
                                &lt; Previous
                            </button>
                            <span class="text-sm text-gray-700">Page {currentPage}</span>
                            <button class="btn btn-outline btn-sm" on:click={() => goToPage(currentPage + 1)} disabled={!hasMore}>
                                Next &gt;
                            </button>
                        </div>
                    {/if}
                </div>
            {/if}

            <!-- Transactions Tab -->
            {#if activeTabIndex === TXS_TAB}
                <div class="w-full animate-fade-in">
                    {#if transactions.length === 0}
                        <EmptyState title="No Transactions" message="No transactions found for this asset." />
                    {:else}
                        <div class="overflow-x-auto">
                            <table class="min-w-full divide-y divide-gray-200">
                                <thead class="bg-gray-50">
                                    <tr>
                                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Tx Hash</th>
                                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Block</th>
                                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Time</th>
                                    </tr>
                                </thead>
                                <tbody class="bg-white divide-y divide-gray-200">
                                    {#each transactions as tx (tx.tx_hash)}
                                        <tr class="hover:bg-gray-50">
                                            <td class="px-6 py-4 whitespace-nowrap text-sm">
                                                <a href="/transactions/{tx.tx_hash}" class="text-blue-600 hover:underline">
                                                    {truncate(tx.tx_hash, 25, '...')}
                                                </a>
                                            </td>
                                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                                <a href="/blocks/{tx.block_number || tx.block_height}" class="text-blue-500 hover:underline">
                                                    {tx.block_number || tx.block_height}
                                                </a>
                                            </td>
                                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                                {getDate(tx.block_time)}
                                            </td>
                                        </tr>
                                    {/each}
                                </tbody>
                            </table>
                        </div>

                        <div class="mt-6 flex justify-between items-center">
                            <button class="btn btn-outline btn-sm" on:click={() => goToPage(currentPage - 1)} disabled={currentPage <= 1}>
                                &lt; Previous
                            </button>
                            <span class="text-sm text-gray-700">Page {currentPage}</span>
                            <button class="btn btn-outline btn-sm" on:click={() => goToPage(currentPage + 1)} disabled={!hasMore}>
                                Next &gt;
                            </button>
                        </div>
                    {/if}
                </div>
            {/if}

            <!-- UTXOs Tab -->
            {#if activeTabIndex === UTXOS_TAB}
                <div class="w-full animate-fade-in">
                    {#if utxos.length === 0}
                        <EmptyState title="No UTXOs" message="No unspent transaction outputs found holding this asset." />
                    {:else}
                        <div class="overflow-x-auto">
                            <table class="min-w-full divide-y divide-gray-200">
                                <thead class="bg-gray-50">
                                    <tr>
                                        <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Tx Hash # Index</th>
                                        <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Address</th>
                                        <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Amounts</th>
                                    </tr>
                                </thead>
                                <tbody class="bg-white divide-y divide-gray-200">
                                    {#each utxos as utxo (utxo.tx_hash + '#' + utxo.output_index)}
                                        <tr class="hover:bg-gray-50">
                                            <td class="px-4 py-4 text-sm">
                                                <a href="/transactions/{utxo.tx_hash}" class="text-blue-600 hover:underline">
                                                    {truncate(utxo.tx_hash, 20, '...')}
                                                </a>
                                                <span class="text-gray-400">#{utxo.output_index}</span>
                                            </td>
                                            <td class="px-4 py-4 text-sm">
                                                <AddressLink address={utxo.address} />
                                            </td>
                                            <td class="px-4 py-4 text-sm">
                                                <div class="space-x-1">
                                                    <AmountBadges amounts={utxo.amount} />
                                                </div>
                                            </td>
                                        </tr>
                                    {/each}
                                </tbody>
                            </table>
                        </div>

                        <div class="mt-6 flex justify-between items-center">
                            <button class="btn btn-outline btn-sm" on:click={() => goToPage(currentPage - 1)} disabled={currentPage <= 1}>
                                &lt; Previous
                            </button>
                            <span class="text-sm text-gray-700">Page {currentPage}</span>
                            <button class="btn btn-outline btn-sm" on:click={() => goToPage(currentPage + 1)} disabled={!hasMore}>
                                Next &gt;
                            </button>
                        </div>
                    {/if}
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
