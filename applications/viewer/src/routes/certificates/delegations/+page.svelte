<script lang="ts">
    import {goto} from "$app/navigation";
    import {page} from "$app/stores";
    import {truncate, getDate} from "$lib/util";
    import { onMount } from 'svelte';

    export let data;

    interface Delegation {
        block_number: number;
        block_time: number;
        credential: string;
        credential_type: string;
        address: string;
        pool_id: string;
        tx_hash: string;
        cert_index: number;
        tx_index: number;
        epoch: number;
        slot: number;
        block_hash: string;
    }

    interface PageLink {
        href: string;
        active: boolean;
    }

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

    // --- Pagination State --- 
    let loading = false;
    let currentPage = 1;
    const ITEMS_PER_PAGE = 15;

    onMount(() => {
        // Initialize currentPage from URL or data
        currentPage = parseInt(data.page || "1", 10);
        loading = false;
    });

    // --- Has More Logic --- 
    $: hasMore = data.delegations && data.delegations.length > 0;

    // --- Navigation Function --- 
    function goToPage(targetPage: number) {
        if (targetPage < 1 || loading) return;
        loading = true;
        const url = `/certificates/delegations?page=${targetPage}&count=${ITEMS_PER_PAGE}`;
        goto(url);
    }

    // --- Reactive Update Block --- 
    $: if (data && $page) {
        currentPage = parseInt(data.page || "1", 10);
        loading = false;
    }

    // Initialize delegations array if undefined
    if (!data.delegations) data.delegations = [];

    function truncateHash(hash: string | null | undefined): string {
        if (!hash) return '';
        return hash.length > 15 ? `${hash.substring(0, 8)}...${hash.substring(hash.length - 7)}` : hash;
    }

    function truncateAddress(address: string | null | undefined): string {
        if (!address) return '';
        return address.length > 15 ? `${address.substring(0, 8)}...${address.substring(address.length - 7)}` : address;
    }
</script>


<section class="container mx-auto text-sm">
    <!-- Toast Notification -->
    {#if showToast}
        <div class="fixed bottom-4 right-4 bg-green-500 text-white px-4 py-2 rounded-lg shadow-lg transition-opacity duration-300 z-50">
            {toastMessage}
        </div>
    {/if}

    <h2 class="text-xl font-bold text-center text-gray-500 mb-4">Delegations</h2>
    <div class="flex justify-end mt-6">
        <div class="join">
            <button 
                class="join-item btn btn-sm" 
                disabled={currentPage <= 1 || loading}
                on:click={() => goToPage(currentPage - 1)}
            >
                «
            </button>
            <button class="join-item btn btn-sm">
                Page {currentPage}
            </button>
            <button 
                class="join-item btn btn-sm" 
                disabled={!hasMore || loading}
                on:click={() => goToPage(currentPage + 1)}
            >
                »
            </button>
        </div>
    </div>
    <div class="overflow-x-auto bg-white shadow-md rounded-lg mb-4 relative">
        {#if loading}
            <div class="absolute inset-0 bg-white bg-opacity-75 flex justify-center items-center z-10">
                <span class="loading loading-spinner loading-lg"></span>
            </div>
        {/if}
        <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50">
            <tr>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Stake Address</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Pool ID</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Block</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Transaction Hash</th>
            </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
            {#each data.delegations as delegation}
                <tr class="hover:bg-gray-50">
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                        <div class="flex items-center gap-2">
                            <a href="/rewards/account/{delegation.address}" class="text-blue-500 hover:underline">
                                <span>{truncateAddress(delegation.address)}</span>
                            </a>
                            {#if delegation.address}
                                <button
                                    class="text-gray-400 hover:text-gray-600"
                                    on:click={() => {
                                        copyToClipboard(delegation.address, 'Stake address copied to clipboard');
                                    }}
                                    title="Copy stake address"
                                >
                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
                                    </svg>
                                </button>
                            {/if}
                        </div>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        <div class="flex items-center gap-2">
                            <a href="/pools/{delegation.pool_id}" class="text-blue-500 hover:underline">
                                <span>{truncateHash(delegation.pool_id)}</span>
                            </a>
                            {#if delegation.pool_id}
                                <button
                                    class="text-gray-400 hover:text-gray-600"
                                    on:click={() => {
                                        copyToClipboard(delegation.pool_id, 'Pool ID copied to clipboard');
                                    }}
                                    title="Copy pool ID"
                                >
                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
                                    </svg>
                                </button>
                            {/if}
                        </div>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        <a href="/blocks/{delegation.block_number}" class="text-blue-500 hover:underline">
                            {delegation.block_number}
                        </a>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                        <div class="flex items-center gap-2">
                            <a href="/transactions/{delegation.tx_hash}" class="text-blue-500 hover:underline">
                                <span>{truncateHash(delegation.tx_hash)}</span>
                            </a>
                            {#if delegation.tx_hash}
                                <button
                                    class="text-gray-400 hover:text-gray-600"
                                    on:click={() => {
                                        copyToClipboard(delegation.tx_hash, 'Transaction hash copied to clipboard');
                                    }}
                                    title="Copy transaction hash"
                                >
                                    <i class="fas fa-copy text-xs"></i>
                                </button>
                            {/if}
                        </div>
                    </td>
                </tr>
            {/each}
            </tbody>
        </table>
    </div>
    <div class="flex justify-end mt-6">
        <div class="join">
            <button 
                class="join-item btn btn-sm" 
                disabled={currentPage <= 1 || loading}
                on:click={() => goToPage(currentPage - 1)}
            >
                «
            </button>
            <button class="join-item btn btn-sm">
                Page {currentPage}
            </button>
            <button 
                class="join-item btn btn-sm" 
                disabled={!hasMore || loading}
                on:click={() => goToPage(currentPage + 1)}
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
