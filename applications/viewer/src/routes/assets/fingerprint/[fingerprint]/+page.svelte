<script lang="ts">
    import { page } from '$app/stores';
    import { goto } from '$app/navigation';
    import { truncate, getDate } from '$lib/util';
    import { parseUnit } from '$lib/utils/asset';
    import TruncateCopy from '../../../../components/TruncateCopy.svelte';
    import EmptyState from '../../../../components/EmptyState.svelte';

    export let data: any;

    let loading = false;
    let fingerprint = '';
    let supply: any = null;
    let history: any[] = [];
    let unit: string | null = null;
    let currentPage = 1;
    let count = 15;

    $: supplyValue = supply?.supply ?? '—';
    $: hasMore = history.length === count;

    function goToPage(newPage: number) {
        if (newPage < 1 || loading) return;
        loading = true;
        goto(`/assets/fingerprint/${fingerprint}?page=${newPage}&count=${count}`);
    }

    function decodeAssetName(u: string): string {
        try {
            const parsed = parseUnit(u);
            return parsed.assetNameUtf8 || parsed.assetNameHex || '(empty)';
        } catch {
            return u;
        }
    }

    $: if (data && $page) {
        loading = false;
        fingerprint = data.fingerprint;
        supply = data.supply;
        history = data.history || [];
        unit = data.unit;
        currentPage = data.currentPage;
        count = data.count;
    }
</script>

<section class="py-8 px-4 md:px-6 min-h-screen">
    <div class="container mx-auto max-w-6xl">
        <!-- Header -->
        <div class="bg-base-100 rounded-xl shadow-sm border border-base-300 overflow-hidden mb-8">
            <div class="bg-base-200/50 p-6 border-b border-base-200">
                <h2 class="text-xs font-bold text-base-content/60 uppercase tracking-wider mb-2">
                    Asset Fingerprint
                </h2>
                <div class="flex items-center gap-3">
                    <TruncateCopy text={fingerprint} max={60} />
                </div>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 divide-y md:divide-y-0 md:divide-x divide-base-200">
                <div class="p-6">
                    <div class="text-xs font-bold text-base-content/50 uppercase tracking-wider mb-1">
                        Total Supply
                    </div>
                    <div class="text-base-content font-medium text-lg">
                        {supplyValue}
                    </div>
                </div>
                <div class="p-6">
                    <div class="text-xs font-bold text-base-content/50 uppercase tracking-wider mb-1">
                        Full Details
                    </div>
                    <div class="text-base-content text-sm">
                        {#if unit}
                            <a href="/assets/unit/{unit}" class="btn btn-primary btn-sm">
                                View Asset Detail
                            </a>
                        {:else}
                            <span class="text-base-content/50">No asset data available</span>
                        {/if}
                    </div>
                </div>
            </div>
        </div>

        <!-- History Table -->
        <div class="bg-base-100 rounded-xl shadow-sm border border-base-300 p-6 min-h-[300px] relative">
            {#if loading}
                <div class="absolute inset-0 bg-base-100 bg-opacity-75 flex justify-center items-center z-10">
                    <span class="loading loading-spinner loading-lg"></span>
                </div>
            {/if}

            <h3 class="text-lg font-semibold text-base-content mb-4">Mint/Burn History</h3>

            {#if history.length === 0}
                <EmptyState title="No History" message="No mint or burn events found for this fingerprint." />
            {:else}
                <div class="overflow-x-auto">
                    <table class="min-w-full divide-y divide-base-300">
                        <thead class="bg-base-200">
                            <tr>
                                <th class="px-4 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Tx Hash</th>
                                <th class="px-4 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Asset</th>
                                <th class="px-4 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Type</th>
                                <th class="px-4 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Quantity</th>
                                <th class="px-4 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Block</th>
                                <th class="px-4 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Time</th>
                            </tr>
                        </thead>
                        <tbody class="bg-base-100 divide-y divide-base-300">
                            {#each history as event (event.tx_hash + event.quantity)}
                                <tr class="hover:bg-base-200">
                                    <td class="px-4 py-4 text-sm">
                                        <a href="/transactions/{event.tx_hash}" class="text-blue-600 hover:underline">
                                            {truncate(event.tx_hash, 20, '...')}
                                        </a>
                                    </td>
                                    <td class="px-4 py-4 text-sm">
                                        <a href="/assets/unit/{event.unit}" class="text-blue-600 hover:underline">
                                            {decodeAssetName(event.unit)}
                                        </a>
                                    </td>
                                    <td class="px-4 py-4 text-sm">
                                        {#if event.mint_type === 'MINT'}
                                            <span class="badge badge-success badge-sm">MINT</span>
                                        {:else}
                                            <span class="badge badge-error badge-sm">BURN</span>
                                        {/if}
                                    </td>
                                    <td class="px-4 py-4 text-sm font-medium text-base-content">
                                        {event.quantity}
                                    </td>
                                    <td class="px-4 py-4 text-sm text-base-content/60">
                                        <a href="/blocks/{event.block_number}" class="text-blue-500 hover:underline">
                                            {event.block_number}
                                        </a>
                                    </td>
                                    <td class="px-4 py-4 text-sm text-base-content/60">
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
                    <span class="text-sm text-base-content/80">Page {currentPage}</span>
                    <button class="btn btn-outline btn-sm" on:click={() => goToPage(currentPage + 1)} disabled={!hasMore}>
                        Next &gt;
                    </button>
                </div>
            {/if}
        </div>
    </div>
</section>
