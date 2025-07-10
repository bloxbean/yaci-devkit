<script lang="ts">
    import {goto} from "$app/navigation";
    import {page} from "$app/stores";
    import {truncate, getDate} from "$lib/util";
    import { onMount } from 'svelte';

    export let data;

    interface StakeRegistration {
        address: string;
        block_number: number;
        block_time: number;
        tx_hash: string;
    }

    interface PageLink {
        href: string;
        active: boolean;
    }

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
    $: hasMore = data.registrations && data.registrations.length > 0;

    // --- Navigation Function --- 
    function goToPage(targetPage: number) {
        if (targetPage < 1 || loading) return;
        loading = true;
        const url = `/certificates/stakekey-registrations?page=${targetPage}&count=${ITEMS_PER_PAGE}`;
        goto(url);
    }

    // --- Reactive Update Block --- 
    $: if (data && $page) {
        currentPage = parseInt(data.page || "1", 10);
        loading = false;
    }

    // Initialize registrations array if undefined
    if (!data.registrations) data.registrations = [];
</script>


<section class="container mx-auto text-sm">
    <h2 class="text-xl font-bold text-center text-gray-500 mb-4">Stake Key Registrations</h2>
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
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Address</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Block</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Time</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Transaction Hash</th>
            </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
            {#each data.registrations as registration: StakeRegistration, index}
                <tr class="hover:bg-gray-50">
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{registration.address}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        <a href="/blocks/{registration.block_number}" class="text-blue-500 hover:underline">
                            {registration.block_number}
                        </a>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{getDate(registration.block_time)}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                        <a href="/transactions/{registration.tx_hash}" class="text-blue-500 hover:underline">
                            <span>{truncate(registration.tx_hash, 30, "...")}</span>
                        </a>
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
