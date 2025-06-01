<script lang="ts">
    import {goto} from "$app/navigation";
    import {page} from "$app/stores";
    import {truncate} from "$lib/util";
    import { EyeIcon} from 'svelte-feather-icons'
    import { onMount } from 'svelte';

    export let data;

    // Define an interface for GovAction data
    interface GovAction {
        tx_hash: string;
        index: number;
        block_number: number;
        type: string;
        return_address: string;
        // Assuming anchor_url and anchor_hash might be present
        anchor_url?: string;
        anchor_hash?: string;
    }

    // Define a type for the page objects (if used, otherwise remove)
    interface PageLink {
        href: string;
        active: boolean;
    }

    $: activeUrl = $page.url.searchParams.get('page');
    let pages: PageLink[] = []; // Explicitly type pages

    // This reactive block seems unused based on the template. Consider removing.
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
    $: hasMore = data.govactions && data.govactions.length > 0;

    // --- Navigation Function --- 
    function goToPage(targetPage: number) {
        if (targetPage < 1 || loading) return;
        loading = true;
        const url = `/governance/govactions?page=${targetPage}&count=${ITEMS_PER_PAGE}`;
        goto(url);
    }

    // --- Reactive Update Block --- 
    $: if (data && $page) {
        currentPage = parseInt(data.page || "1", 10);
        loading = false;
    }

    // Initialize govactions array if undefined
    if (!data.govactions) data.govactions = [];

    let showModal = false;
    let selectedDetails: GovAction | null = null;

    function showDetails(details: GovAction) {
        selectedDetails = details;
        showModal = true;
    }

    function closeModal() {
        showModal = false;
    }
</script>


<section class="container mx-auto text-sm">
    <h2 class="text-xl font-bold text-center text-gray-500 mb-4">Proposals</h2>
    <div class="flex justify-end mt-6">
        <div class="join">
            <button 
                class="join-item btn btn-sm" 
                disabled={currentPage <= 1 || loading}
                on:click={() => goToPage(currentPage - 1)}
            >
                «
            </button>
            <button class="join-item btn btn-sm no-animation">
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
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Transaction Hash</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Index</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Block</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Action Type</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Return Address</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Details</th>
            </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
            {#each data.govactions as govaction: GovAction, index}
                <tr class="hover:bg-gray-50">
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                        <a href="/transactions/{govaction.tx_hash}" class="text-blue-500 hover:underline">
                            <span>{truncate(govaction.tx_hash, 30, "...")}</span>
                        </a>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{govaction.index}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        <a href="/blocks/{govaction.block_number}" class="text-blue-500 hover:underline">
                            {govaction.block_number}
                        </a>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{govaction.type}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{truncate(govaction.return_address, 30, "...")}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500 text-center">
                         <a href="/governance/govactions/{govaction.tx_hash}_{govaction.index}" target="_blank" title="View Details">
                             <EyeIcon size="1.2x" class="inline-block"/>
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
            <button class="join-item btn btn-sm no-animation">
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
