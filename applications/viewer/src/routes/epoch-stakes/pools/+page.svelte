<!-- Pool-specific epoch stakes view -->
<script lang="ts">
    import { page } from '$app/stores';
    import { goto } from '$app/navigation';
    import { formatAmount } from '$lib/utils/format';
    import { truncateAddress } from '$lib/utils/hash';
    import { onDestroy } from 'svelte';

    export let data;

    let loading = false;
    let toastMessage = '';
    let toastVisible = false;
    let toastTimeout: ReturnType<typeof setTimeout>;
    let searchPoolId = '';
    let searchEpoch = '';

    $: ({ stakes = [], currentPage = 1, itemsPerPage = 15, poolId, epoch, totalStake = 0 } = data);
    $: hasRequiredParams = !!poolId && !!epoch;

    function showToast(message: string) {
        if (toastTimeout) clearTimeout(toastTimeout);
        toastMessage = message;
        toastVisible = true;
        toastTimeout = setTimeout(() => {
            toastVisible = false;
        }, 3000);
    }

    async function copyToClipboard(text: string, type: string) {
        try {
            await navigator.clipboard.writeText(text);
            showToast(`${type} copied to clipboard`);
        } catch (err) {
            console.error('Failed to copy:', err);
            showToast('Failed to copy to clipboard');
        }
    }

    async function handlePageChange(newPage: number) {
        if (newPage < 1) return;
        
        loading = true;
        try {
            const url = new URL(window.location.href);
            url.searchParams.set('page', newPage.toString());
            await goto(url.toString(), { invalidateAll: true });
        } finally {
            loading = false;
        }
    }

    function handleSearch(e: Event) {
        e.preventDefault();
        if (!searchPoolId || !searchEpoch) {
            showToast('Please enter both Pool ID and Epoch');
            return;
        }
        
        const url = new URL(window.location.href);
        url.searchParams.set('id', searchPoolId);
        url.searchParams.set('epoch', searchEpoch);
        url.searchParams.delete('page'); // Reset to first page
        goto(url.toString());
    }

    function handleKeydown(e: KeyboardEvent) {
        if (e.key === 'Enter' && e.target instanceof HTMLInputElement) {
            e.preventDefault();
            const form = e.target.closest('form');
            if (form) {
                const submitButton = form.querySelector('button[type="submit"]');
                if (submitButton instanceof HTMLButtonElement) {
                    submitButton.click();
                }
            }
        }
    }

    onDestroy(() => {
        if (toastTimeout) clearTimeout(toastTimeout);
    });
</script>

<div class="container mx-auto px-4 py-8">
    {#if !hasRequiredParams}
        <div class="bg-white rounded-lg shadow-md p-6 mb-6 max-w-2xl mx-auto">
            <h2 class="text-2xl font-semibold mb-6">Search Pool Stakes</h2>
            <form id="searchForm" on:submit={handleSearch} class="space-y-4">
                <div>
                    <label for="poolId" class="block text-sm font-medium text-gray-700 mb-2">Pool ID</label>
                    <input
                        type="text"
                        id="poolId"
                        bind:value={searchPoolId}
                        class="input input-bordered w-full h-8"
                        placeholder="Enter pool ID"
                        on:keydown={handleKeydown}
                    />
                </div>
                <div>
                    <label for="epoch" class="block text-sm font-medium text-gray-700 mb-2">Epoch Number</label>
                    <div class="flex gap-2 items-center">
                        <input
                            type="number"
                            id="epoch"
                            bind:value={searchEpoch}
                            class="input input-bordered w-32 h-8"
                            placeholder="Enter epoch"
                            min="0"
                            on:keydown={handleKeydown}
                        />
                        <button
                            type="submit"
                            class="btn btn-primary px-4 h-8 min-h-0 flex items-center"
                            disabled={loading}
                        >
                            {loading ? 'Loading...' : 'Search'}
                        </button>
                    </div>
                </div>
            </form>
        </div>
    {:else}
        <div class="bg-white rounded-lg shadow-md p-6 mb-6">
            <h2 class="text-2xl font-semibold mb-4">Pool Epoch Stakes</h2>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
                <div>
                    <p class="text-sm text-gray-600">Pool ID</p>
                    <div class="flex items-center gap-2">
                        <p class="font-medium max-w-full overflow-hidden" title={poolId || ''}>
                            {#if poolId}
                                <span class="block md:hidden">{truncateAddress(poolId, 12, 12)}</span>
                                <span class="hidden md:block">{poolId}</span>
                            {/if}
                        </p>
                        <button
                            class="text-primary hover:text-primary-focus flex-shrink-0"
                            on:click={() => copyToClipboard(poolId || '', 'Pool ID')}
                            title="Copy Pool ID"
                        >
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
                            </svg>
                        </button>
                    </div>
                </div>
                <div>
                    <p class="text-sm text-gray-600">Epoch</p>
                    <p class="font-medium">{epoch}</p>
                </div>
                <div>
                    <p class="text-sm text-gray-600">Total Stake</p>
                    <p class="font-medium">{formatAmount(totalStake)} ₳</p>
                </div>
            </div>

            <!-- Pagination -->
            <div class="flex justify-end gap-2 mb-4">
                <div class="join">
                    <button
                        class="join-item btn btn-sm"
                        disabled={currentPage === 1 || loading}
                        on:click={() => handlePageChange(currentPage - 1)}
                    >
                        «
                    </button>
                    <button class="join-item btn btn-sm">Page {currentPage}</button>
                    <button
                        class="join-item btn btn-sm"
                        disabled={stakes.length === 0 || loading}
                        on:click={() => handlePageChange(currentPage + 1)}
                    >
                        »
                    </button>
                </div>
            </div>

            <!-- Stakes Table -->
            <div class="overflow-x-auto">
                <table class="table w-full">
                    <thead>
                        <tr>
                            <th>Stake Address</th>
                            <th class="text-right">Amount (₳)</th>
                        </tr>
                    </thead>
                    <tbody>
                        {#if stakes.length === 0}
                            <tr>
                                <td colspan="2" class="text-center py-4">No stakes found</td>
                            </tr>
                        {:else}
                            {#each stakes as stake}
                                <tr>
                                    <td>
                                        <div class="flex items-center gap-2">
                                            {#if stake.address}
                                                <a
                                                    href={`/epoch-stakes/accounts?address=${stake.address}&epoch=${epoch}`}
                                                    class="text-primary hover:text-primary-focus max-w-full overflow-hidden"
                                                    title={stake.address}
                                                >
                                                    <span class="block md:hidden">{truncateAddress(stake.address, 12, 12)}</span>
                                                    <span class="hidden md:block">{stake.address}</span>
                                                </a>
                                                <button
                                                    class="text-primary hover:text-primary-focus flex-shrink-0"
                                                    on:click={() => copyToClipboard(stake.address, 'Stake Address')}
                                                    title="Copy Stake Address"
                                                >
                                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
                                                    </svg>
                                                </button>
                                            {:else}
                                                <span class="text-gray-400">No stake address</span>
                                            {/if}
                                        </div>
                                    </td>
                                    <td class="text-right">{formatAmount(stake.amount)}</td>
                                </tr>
                            {/each}
                        {/if}
                    </tbody>
                </table>
            </div>
        </div>
    {/if}
</div>

{#if toastVisible}
    <div class="fixed bottom-4 right-4 bg-green-500 text-white px-4 py-2 rounded-lg shadow-lg transition-opacity duration-300">
        {toastMessage}
    </div>
{/if}