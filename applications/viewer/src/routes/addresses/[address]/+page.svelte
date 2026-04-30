<script lang="ts">
    import { page } from '$app/stores';
    import { goto } from '$app/navigation';
    import { onMount } from 'svelte';
    import { formatAda, truncate, lovelaceToAda, getDate } from '$lib/util';
    import { parseUnit } from '$lib/utils/asset';
    import { balanceApiMode } from '$lib/stores/balanceApi';
    import TruncateCopy from '../../../components/TruncateCopy.svelte';
    import AddressLink from '../../../components/AddressLink.svelte';
    import AmountBadges from '../../../components/AmountBadges.svelte';
    import EmptyState from '../../../components/EmptyState.svelte';
    import ErrorState from '../../../components/ErrorState.svelte';
    import OutputDetails from '../../../components/inputoutput/OutputDetails.svelte';

    export let data: any;

    const UTXOS_TAB = 0;
    const TXS_TAB = 1;
    const TOKENS_TAB = 2;

    let activeTabIndex = UTXOS_TAB;
    let loading = false;
    let address = '';
    let amounts: any[] = [];
    let utxos: any[] = [];
    let transactions: any[] = [];
    let currentPage = 1;
    let count = 15;
    let order: 'asc' | 'desc' = 'desc';
    let balanceApiUsed: 'balance' | 'amounts' = 'amounts';
    let errorMsg: string | null = null;

    let showDetails = false;
    let selectedUtxo: any = {};

    function toggleDetails(utxo: any) {
        selectedUtxo = utxo;
        showDetails = !showDetails;
    }

    function closeDetails() {
        showDetails = false;
    }

    // Compute ADA balance from amounts
    $: adaBalance = amounts.find((a: any) => a.unit === 'lovelace')?.quantity || 0;
    $: tokenCount = amounts.filter((a: any) => a.unit !== 'lovelace').length;
    $: tokens = amounts.filter((a: any) => a.unit !== 'lovelace');

    // Pagination
    $: hasMore = activeTabIndex === UTXOS_TAB
        ? utxos.length === count
        : transactions.length === count;

    function goToPage(newPage: number) {
        if (newPage < 1 || loading) return;
        loading = true;
        const tab = activeTabIndex === UTXOS_TAB ? 'utxos' : activeTabIndex === TXS_TAB ? 'transactions' : 'tokens';
        const url = `/addresses/${address}?page=${newPage}&count=${count}&order=${order}&tab=${tab}&balanceApi=${$balanceApiMode}`;
        goto(url);
    }

    function switchTab(tabIndex: number) {
        activeTabIndex = tabIndex;
        // Reset to page 1 when switching tabs
        loading = true;
        const tab = tabIndex === UTXOS_TAB ? 'utxos' : tabIndex === TXS_TAB ? 'transactions' : 'tokens';
        const url = `/addresses/${address}?page=1&count=${count}&order=${order}&tab=${tab}&balanceApi=${$balanceApiMode}`;
        goto(url);
    }


    onMount(() => {
        const urlTab = $page.url.searchParams.get('tab') || 'utxos';
        if (urlTab === 'transactions') activeTabIndex = TXS_TAB;
        else if (urlTab === 'tokens') activeTabIndex = TOKENS_TAB;
        else activeTabIndex = UTXOS_TAB;
    });

    // Update from server data
    $: if (data && $page) {
        loading = false;
        address = data.address;
        amounts = data.amounts || [];
        utxos = data.utxos || [];
        transactions = data.transactions || [];
        currentPage = data.currentPage;
        count = data.count;
        order = data.order;
        balanceApiUsed = data.balanceApiUsed;
        errorMsg = data.error;

        // Cache which balance API works
        if (data.balanceApiUsed) {
            balanceApiMode.set(data.balanceApiUsed);
        }

        // Sync tab from URL
        const urlTab = $page.url.searchParams.get('tab') || 'utxos';
        if (urlTab === 'transactions') activeTabIndex = TXS_TAB;
        else if (urlTab === 'tokens') activeTabIndex = TOKENS_TAB;
        else activeTabIndex = UTXOS_TAB;
    }
</script>

<section class="py-8 px-4 md:px-6 min-h-screen">
    <div class="container mx-auto max-w-6xl">
        <!-- Header -->
        <div class="bg-base-100 rounded-xl shadow-sm border border-base-300 overflow-hidden mb-8">
            <div class="bg-base-200/50 p-6 border-b border-base-200">
                <h2 class="text-xs font-bold text-base-content/60 uppercase tracking-wider mb-2">
                    Address
                </h2>
                <div class="flex items-center gap-3">
                    <TruncateCopy text={address} max={60} />
                </div>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-3 divide-y md:divide-y-0 md:divide-x divide-base-200">
                <div class="p-6">
                    <div class="text-xs font-bold text-base-content/50 uppercase tracking-wider mb-1">
                        ADA Balance
                    </div>
                    <div class="text-base-content font-medium text-lg">
                        {lovelaceToAda(adaBalance, 2)} <span class="text-sm text-base-content/60">ADA</span>
                    </div>
                </div>

                <div class="p-6">
                    <div class="text-xs font-bold text-base-content/50 uppercase tracking-wider mb-1">
                        Token Types
                    </div>
                    <div class="text-base-content font-medium text-lg">
                        {tokenCount}
                    </div>
                </div>

                <div class="p-6">
                    <div class="text-xs font-bold text-base-content/50 uppercase tracking-wider mb-1">
                        UTXOs
                    </div>
                    <div class="text-base-content font-medium text-lg">
                        {utxos.length}{utxos.length === count ? '+' : ''}
                    </div>
                </div>
            </div>
        </div>

        {#if errorMsg}
            <div class="mb-6">
                <ErrorState title="Error loading data" message={errorMsg} />
            </div>
        {/if}

        <!-- Tabs -->
        <div class="mb-6 bg-base-200 rounded-xl p-2">
            <div class="flex flex-wrap gap-2 border-b border-base-300">
                <button
                    class="px-5 py-3 text-sm font-medium transition-colors duration-200 border-b-2 {activeTabIndex === UTXOS_TAB
                        ? 'border-blue-600 text-blue-600'
                        : 'border-transparent text-base-content/60 hover:text-base-content/80 hover:border-gray-300'}"
                    on:click={() => switchTab(UTXOS_TAB)}
                >
                    UTXOs
                    {#if utxos.length > 0}
                        <span class="ml-1 bg-base-200 text-base-content/70 py-0.5 px-2 rounded-full text-xs">{utxos.length}</span>
                    {/if}
                </button>

                <button
                    class="px-5 py-3 text-sm font-medium transition-colors duration-200 border-b-2 {activeTabIndex === TXS_TAB
                        ? 'border-blue-600 text-blue-600'
                        : 'border-transparent text-base-content/60 hover:text-base-content/80 hover:border-gray-300'}"
                    on:click={() => switchTab(TXS_TAB)}
                >
                    Transactions
                    {#if transactions.length > 0}
                        <span class="ml-1 bg-base-200 text-base-content/70 py-0.5 px-2 rounded-full text-xs">{transactions.length}</span>
                    {/if}
                </button>

                <button
                    class="px-5 py-3 text-sm font-medium transition-colors duration-200 border-b-2 {activeTabIndex === TOKENS_TAB
                        ? 'border-blue-600 text-blue-600'
                        : 'border-transparent text-base-content/60 hover:text-base-content/80 hover:border-gray-300'}"
                    on:click={() => switchTab(TOKENS_TAB)}
                >
                    Tokens
                    {#if tokenCount > 0}
                        <span class="ml-1 bg-base-200 text-base-content/70 py-0.5 px-2 rounded-full text-xs">{tokenCount}</span>
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

            <!-- UTXOs Tab -->
            {#if activeTabIndex === UTXOS_TAB}
                <div class="w-full animate-fade-in">
                    {#if utxos.length === 0}
                        <EmptyState title="No UTXOs" message="No unspent transaction outputs found for this address." />
                    {:else}
                        <div class="overflow-x-auto">
                            <table class="min-w-full divide-y divide-base-300">
                                <thead class="bg-base-200">
                                    <tr>
                                        <th class="px-4 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Tx Hash # Index</th>
                                        <th class="px-4 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Amounts</th>
                                        <th class="px-4 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Details</th>
                                    </tr>
                                </thead>
                                <tbody class="bg-base-100 divide-y divide-base-300">
                                    {#each utxos as utxo (utxo.tx_hash + '#' + utxo.output_index)}
                                        <tr class="hover:bg-base-200">
                                            <td class="px-4 py-4 text-sm">
                                                <a href="/transactions/{utxo.tx_hash}" class="text-blue-600 hover:underline">
                                                    {truncate(utxo.tx_hash, 20, '...')}
                                                </a>
                                                <span class="text-base-content/50">#{utxo.output_index}</span>
                                                {#if utxo.data_hash}
                                                    <div class="text-xs text-base-content/50 mt-1" title={utxo.data_hash}>
                                                        Datum: {truncate(utxo.data_hash, 20, '...')}
                                                    </div>
                                                {/if}
                                            </td>
                                            <td class="px-4 py-4 text-sm">
                                                <div class="space-x-1">
                                                    <AmountBadges amounts={utxo.amount} />
                                                </div>
                                            </td>
                                            <td class="px-4 py-4 text-sm">
                                                {#if utxo.inline_datum || utxo.script_ref}
                                                    <button
                                                        class="text-blue-500 hover:underline text-xs"
                                                        on:click={() => toggleDetails(utxo)}
                                                    >
                                                        View
                                                    </button>
                                                {:else}
                                                    <span class="text-gray-300">-</span>
                                                {/if}
                                            </td>
                                        </tr>
                                    {/each}
                                </tbody>
                            </table>
                        </div>

                        <!-- Pagination -->
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
            {/if}

            <!-- Transactions Tab -->
            {#if activeTabIndex === TXS_TAB}
                <div class="w-full animate-fade-in">
                    {#if transactions.length === 0}
                        <EmptyState title="No Transactions" message="No transactions found for this address." />
                    {:else}
                        <div class="overflow-x-auto">
                            <table class="min-w-full divide-y divide-base-300">
                                <thead class="bg-base-200">
                                    <tr>
                                        <th class="px-6 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Tx Hash</th>
                                        <th class="px-6 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Block</th>
                                        <th class="px-6 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Time</th>
                                    </tr>
                                </thead>
                                <tbody class="bg-base-100 divide-y divide-base-300">
                                    {#each transactions as tx (tx.tx_hash)}
                                        <tr class="hover:bg-base-200">
                                            <td class="px-6 py-4 whitespace-nowrap text-sm">
                                                <a href="/transactions/{tx.tx_hash}" class="text-blue-600 hover:underline">
                                                    {truncate(tx.tx_hash, 25, '...')}
                                                </a>
                                            </td>
                                            <td class="px-6 py-4 whitespace-nowrap text-sm text-base-content/60">
                                                <a href="/blocks/{tx.block_height}" class="text-blue-500 hover:underline">
                                                    {tx.block_height}
                                                </a>
                                            </td>
                                            <td class="px-6 py-4 whitespace-nowrap text-sm text-base-content/60">
                                                {getDate(tx.block_time)}
                                            </td>
                                        </tr>
                                    {/each}
                                </tbody>
                            </table>
                        </div>

                        <!-- Pagination -->
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
            {/if}

            <!-- Tokens Tab -->
            {#if activeTabIndex === TOKENS_TAB}
                <div class="w-full animate-fade-in">
                    {#if tokens.length === 0}
                        <EmptyState title="No Tokens" message="No native tokens found for this address." />
                    {:else}
                        <div class="overflow-x-auto">
                            <table class="min-w-full divide-y divide-base-300">
                                <thead class="bg-base-200">
                                    <tr>
                                        <th class="px-6 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Policy ID</th>
                                        <th class="px-6 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Asset Name</th>
                                        <th class="px-6 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Quantity</th>
                                    </tr>
                                </thead>
                                <tbody class="bg-base-100 divide-y divide-base-300">
                                    {#each tokens as token (token.unit)}
                                        {@const parsed = parseUnit(token.unit)}
                                        <tr class="hover:bg-base-200">
                                            <td class="px-6 py-4 text-sm text-base-content/80 font-mono" title={parsed.policyId}>
                                                <a href="/assets/policy/{parsed.policyId}" class="text-blue-600 hover:underline">
                                                    {truncate(parsed.policyId, 20, '...')}
                                                </a>
                                            </td>
                                            <td class="px-6 py-4 text-sm text-base-content/80" title={parsed.assetNameHex}>
                                                <a href="/assets/unit/{token.unit}" class="text-blue-600 hover:underline">
                                                    {parsed.assetNameUtf8 || '(empty)'}
                                                </a>
                                            </td>
                                            <td class="px-6 py-4 text-sm text-base-content font-medium">
                                                {token.quantity.toLocaleString()}
                                            </td>
                                        </tr>
                                    {/each}
                                </tbody>
                            </table>
                        </div>
                    {/if}
                </div>
            {/if}
        </div>
    </div>
</section>

<OutputDetails output={selectedUtxo} show={showDetails} on:closeDetails={closeDetails} />

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
