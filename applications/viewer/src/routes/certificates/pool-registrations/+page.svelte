<script lang="ts">
    import {goto} from "$app/navigation";
    import {page} from "$app/stores";
    import {truncate, lovelaceToAda} from "$lib/util";
    import { onMount } from 'svelte';

    export let data;

    interface PoolRegistration {
        pool_id_bech32: string;
        pledge: number;
        cost: number;
        margin: number;
        pool_owners: string[];
        block_number: number;
        tx_hash: string;
    }

    interface PageLink {
        href: string;
        active: boolean;
    }

    // Add loading state
    let loading = false;
    let error: string | null = null;

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
            if (toastTimeout) {
                clearTimeout(toastTimeout);
            }
            toastTimeout = setTimeout(() => {
                showToast = false;
            }, 2000);
        } catch (err) {
            console.error('Failed to copy text: ', err);
        }
    }

    // Cleanup timeout on component destroy
    onMount(() => {
        return () => {
            if (toastTimeout) {
                clearTimeout(toastTimeout);
            }
        };
    });

    $: activeUrl = $page.url.searchParams.get('page');
    let pages: PageLink[] = [];

    $:{
        pages.forEach((pageLink) => {
            let splitUrl = pageLink.href.split('?');
            let queryString = splitUrl.slice(1).join('?');
            const hrefParams = new URLSearchParams(queryString);
            let hrefValue = hrefParams.get('page');
            if (hrefValue === activeUrl) {
                pageLink.active = true
            } else {
                pageLink.active = false
            }
        })
        pages = pages
    }

    const previous = async () => {
        loading = true;
        error = null;
        try {
            let currentPage = parseInt(data.page);
            let prevPage = currentPage - 1;
            if (prevPage <= 0)
                prevPage = 1;
            await goto(`/certificates/pool-registrations?page=${prevPage}&count=${data.count}`);
        } catch (e) {
            error = 'Failed to load previous page';
            console.error(e);
        } finally {
            loading = false;
        }
    };

    const next = async () => {
        loading = true;
        error = null;
        try {
            let currentPage = parseInt(data.page);
            let nextPage = currentPage + 1;

            if (data.registrations.length == 0)
                nextPage = currentPage;

            await goto(`/certificates/pool-registrations?page=${nextPage}&count=${data.count}`);
        } catch (e) {
            error = 'Failed to load next page';
            console.error(e);
        } finally {
            loading = false;
        }
    };

    if (!data.registrations)
        data.registrations = []
</script>

<section class="container mx-auto text-sm">
    <!-- Toast Notification -->
    {#if showToast}
        <div class="fixed bottom-4 right-4 bg-green-500 text-white px-4 py-2 rounded-lg shadow-lg transition-opacity duration-300 z-50">
            {toastMessage}
        </div>
    {/if}

    <h2 class="text-2xl font-bold text-center text-gray-700 mb-6">Pool Registrations</h2>

    {#if error}
        <div class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded relative mb-4" role="alert">
            <span class="block sm:inline">{error}</span>
        </div>
    {/if}

    <div class="flex justify-end mt-6">
        <div class="join">
            <button 
                class="join-item btn btn-sm" 
                disabled={parseInt(data.page) === 1}
                on:click={previous}
            >
                «
            </button>
            <button class="join-item btn btn-sm">Page {data.page}</button>
            <button 
                class="join-item btn btn-sm" 
                disabled={data.registrations.length === 0}
                on:click={next}
            >
                »
            </button>
        </div>
    </div>

    <div class="overflow-x-auto bg-white shadow-md rounded-lg mb-4">
        <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50">
            <tr>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Pool</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Pledge (ADA)</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Cost (ADA)</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Margin (%)</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Owner(s)</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Block</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Transaction Hash</th>
            </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
            {#if loading}
                <tr>
                    <td colspan="7" class="px-6 py-4 text-center">
                        <div class="flex justify-center items-center">
                            <svg class="animate-spin h-8 w-8 text-blue-500" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                            </svg>
                        </div>
                    </td>
                </tr>
            {:else if data.registrations.length === 0}
                <tr>
                    <td colspan="7" class="px-6 py-4 text-center text-gray-500">
                        No pool registrations found
                    </td>
                </tr>
            {:else}
                {#each data.registrations as registration: PoolRegistration, index}
                    <tr class="hover:bg-gray-50 transition-colors duration-150">
                        <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                            <div class="flex items-center gap-2">
                                <a href="/pools/{registration.pool_id_bech32}" class="text-blue-500 hover:underline" title="View pool details">
                                    <span>{truncate(registration.pool_id_bech32, 35, "...")}</span>
                                </a>
                                <button 
                                    class="text-gray-400 hover:text-gray-600 transition-colors duration-150" 
                                    on:click={() => copyToClipboard(registration.pool_id_bech32, 'Pool ID copied to clipboard!')}
                                    title="Copy pool ID"
                                >
                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
                                    </svg>
                                </button>
                            </div>
                        </td>
                        <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500" title="{registration.pledge} lovelace">
                            {lovelaceToAda(registration.pledge, 2)}
                        </td>
                        <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500" title="{registration.cost} lovelace">
                            {lovelaceToAda(registration.cost, 0)}
                        </td>
                        <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                            {(registration.margin * 100).toFixed(2)}
                        </td>
                        <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                            {#each registration.pool_owners as owner}
                                <div class="flex items-center gap-2 mb-1 last:mb-0">
                                    <span>{truncate(owner, 20, "...")}</span>
                                    <button 
                                        class="text-gray-400 hover:text-gray-600 transition-colors duration-150" 
                                        on:click={() => copyToClipboard(owner, 'Stake address copied to clipboard!')}
                                        title="Copy stake address"
                                    >
                                        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
                                        </svg>
                                    </button>
                                </div>
                            {/each}
                        </td>
                        <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                            <a href="/blocks/{registration.block_number}" class="text-blue-500 hover:underline" title="View block details">
                                {registration.block_number}
                            </a>
                        </td>
                        <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                            <div class="flex items-center gap-2">
                                <a href="/transactions/{registration.tx_hash}" class="text-blue-500 hover:underline" title="View transaction details">
                                    <span>{truncate(registration.tx_hash, 20)}</span>
                                </a>
                                <button 
                                    class="text-gray-400 hover:text-gray-600 transition-colors duration-150" 
                                    on:click={() => copyToClipboard(registration.tx_hash, 'Transaction hash copied to clipboard!')}
                                    title="Copy transaction hash"
                                >
                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
                                    </svg>
                                </button>
                            </div>
                        </td>
                    </tr>
                {/each}
            {/if}
            </tbody>
        </table>
    </div>

    <div class="flex justify-end mt-6">
        <div class="join">
            <button 
                class="join-item btn btn-sm" 
                disabled={parseInt(data.page) === 1}
                on:click={previous}
            >
                «
            </button>
            <button class="join-item btn btn-sm">Page {data.page}</button>
            <button 
                class="join-item btn btn-sm" 
                disabled={data.registrations.length === 0}
                on:click={next}
            >
                »
            </button>
        </div>
    </div>
</section>

<style>
    /* Add transition for smooth appearance/disappearance */
    .fixed {
        transition: opacity 0.3s ease-in-out;
        z-index: 50;
    }
</style>
