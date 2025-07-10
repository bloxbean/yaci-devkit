<script lang="ts">
    import { goto } from '$app/navigation';
    import { formatAmount } from '$lib/utils/format';
    import { truncateAddress } from '$lib/utils/hash';
    import { onDestroy } from 'svelte';

    export let data;

    let loading = false;
    let toastMessage = '';
    let toastVisible = false;
    let toastTimeout: ReturnType<typeof setTimeout>;
    let searchAddress = '';
    let searchEpoch = '';

    $: ({ stakeData, address, epoch } = data);

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

    function handleSearch(e: Event) {
        e.preventDefault();
        if (!searchAddress || !searchEpoch) {
            showToast('Please enter both Stake Address and Epoch');
            return;
        }
        
        loading = true;
        const url = new URL(window.location.href);
        url.searchParams.set('address', searchAddress);
        url.searchParams.set('epoch', searchEpoch);
        goto(url.toString()).finally(() => {
            loading = false;
        });
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
    <!-- Toast Notification -->
    {#if toastVisible}
        <div class="fixed bottom-4 right-4 bg-green-500 text-white px-4 py-2 rounded-lg shadow-lg transition-opacity duration-300">
            {toastMessage}
        </div>
    {/if}

    <div class="bg-white rounded-lg shadow-md p-6 mb-6 max-w-2xl mx-auto">
        <h2 class="text-2xl font-semibold mb-6">Search Stake Address</h2>
        <form id="searchForm" on:submit={handleSearch} class="space-y-4">
            <div>
                <label for="address" class="block text-sm font-medium text-gray-700 mb-2">Stake Address</label>
                <input
                    type="text"
                    id="address"
                    bind:value={searchAddress}
                    class="input input-bordered w-full h-8"
                    placeholder="Enter stake address"
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

    {#if stakeData}
        <div class="bg-white rounded-lg shadow-md p-6">
            <h2 class="text-2xl font-semibold mb-4">Stake Address Details</h2>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
                <div>
                    <p class="text-sm text-gray-600">Stake Address</p>
                    <div class="flex items-center gap-2">
                        <p class="font-medium max-w-full overflow-hidden" title={stakeData.address}>
                            <span class="block md:hidden">{truncateAddress(stakeData.address, 12, 12)}</span>
                            <span class="hidden md:block">{stakeData.address}</span>
                        </p>
                        <button
                            class="text-primary hover:text-primary-focus flex-shrink-0"
                            on:click={() => copyToClipboard(stakeData.address, 'Stake Address')}
                            title="Copy Stake Address"
                        >
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
                            </svg>
                        </button>
                    </div>
                </div>
                <div>
                    <p class="text-sm text-gray-600">Epoch</p>
                    <p class="font-medium">{stakeData.epoch}</p>
                </div>
                <div>
                    <p class="text-sm text-gray-600">Amount</p>
                    <p class="font-medium">{formatAmount(stakeData.amount)} ₳</p>
                </div>
                <div>
                    <p class="text-sm text-gray-600">Delegation Epoch</p>
                    <p class="font-medium">{stakeData.delegation_epoch}</p>
                </div>
                <div class="md:col-span-2">
                    <p class="text-sm text-gray-600">Pool ID</p>
                    <div class="flex items-center gap-2">
                        <a
                            href={`/epoch-stakes/pools?id=${stakeData.pool_id}&epoch=${stakeData.epoch}`}
                            class="text-primary hover:text-primary-focus max-w-full overflow-hidden"
                            title={stakeData.pool_id}
                        >
                            <span class="block md:hidden">{truncateAddress(stakeData.pool_id, 12, 12)}</span>
                            <span class="hidden md:block">{stakeData.pool_id}</span>
                        </a>
                        <button
                            class="text-primary hover:text-primary-focus flex-shrink-0"
                            on:click={() => copyToClipboard(stakeData.pool_id, 'Pool ID')}
                            title="Copy Pool ID"
                        >
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
                            </svg>
                        </button>
                    </div>
                </div>
            </div>
        </div>
    {/if}
</div> 