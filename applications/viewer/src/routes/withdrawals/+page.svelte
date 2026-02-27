<script lang="ts">
    import { goto } from '$app/navigation';
    import { page } from '$app/stores';
    import { lovelaceToAda, getDate } from '$lib/util';
    import TruncateCopy from '../../components/TruncateCopy.svelte';
    import AddressLink from '../../components/AddressLink.svelte';
    import EmptyState from '../../components/EmptyState.svelte';

    export let data: {
        withdrawals: any[];
        currentPage: number;
        count: number;
    };

    let loading = false;
    $: currentPage = data.currentPage;
    $: hasMore = data.withdrawals && data.withdrawals.length >= data.count;

    function goToPage(targetPage: number) {
        if (targetPage < 1 || loading) return;
        loading = true;
        goto(`/withdrawals?page=${targetPage}`);
    }

    $: if (data && $page) {
        loading = false;
    }
</script>

<div class="container mx-auto px-4 py-8">
    <h1 class="text-3xl font-bold mb-8">Recent Withdrawals</h1>

    {#if !data.withdrawals || data.withdrawals.length === 0}
        <EmptyState title="No withdrawals yet" message="Withdrawal events will appear here as they occur." />
    {:else}
        <div class="flex justify-end mb-4">
            <div class="join">
                <button
                    class="join-item btn btn-sm"
                    on:click={() => goToPage(currentPage - 1)}
                    disabled={currentPage <= 1 || loading}
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
                <div class="absolute inset-0 bg-white bg-opacity-75 flex justify-center items-center z-10">
                    <span class="loading loading-spinner loading-lg"></span>
                </div>
            {/if}
            <div class="overflow-x-auto">
                <table class="min-w-full divide-y divide-gray-200">
                    <thead class="bg-gray-50">
                        <tr>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Tx Hash</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Address</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Amount (ADA)</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Block</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Time</th>
                        </tr>
                    </thead>
                    <tbody class="bg-white divide-y divide-gray-200">
                        {#each data.withdrawals as w, i (w.tx_hash + w.address + i)}
                            <tr class="hover:bg-gray-50">
                                <td class="px-6 py-4 whitespace-nowrap text-sm">
                                    <a href="/transactions/{w.tx_hash}" class="text-blue-600 hover:underline">
                                        <TruncateCopy text={w.tx_hash} max={16} />
                                    </a>
                                </td>
                                <td class="px-6 py-4 whitespace-nowrap text-sm">
                                    <AddressLink address={w.address} maxLength={25} />
                                </td>
                                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500" title="{w.amount} lovelace">
                                    {lovelaceToAda(w.amount, 2)}
                                </td>
                                <td class="px-6 py-4 whitespace-nowrap text-sm">
                                    <a href="/blocks/{w.block_number}" class="text-blue-500 hover:underline">{w.block_number}</a>
                                </td>
                                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                    {getDate(w.block_time)}
                                </td>
                            </tr>
                        {/each}
                    </tbody>
                </table>
            </div>
        </div>

        <div class="flex justify-end mt-4">
            <div class="join">
                <button
                    class="join-item btn btn-sm"
                    on:click={() => goToPage(currentPage - 1)}
                    disabled={currentPage <= 1 || loading}
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
</div>
