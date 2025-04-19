<script lang="ts">
    import {goto} from "$app/navigation";
    import {page} from "$app/stores";
    import DRepDetails from "../../../components/drep/DRepDetails.svelte";
    import { onMount } from 'svelte';

    export let data;

    // Define an interface for DRep data
    interface DRepData {
        drep_id: string;
        cred_type: string;
        block_number: number;
        tx_hash: string;
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
    $: hasMore = data.updates && data.updates.length > 0;

    // --- Navigation Function --- 
    function goToPage(targetPage: number) {
        if (targetPage < 1 || loading) return;
        loading = true;
        const url = `/governance/drep-updates?page=${targetPage}&count=${ITEMS_PER_PAGE}`;
        goto(url);
    }

    // --- Reactive Update Block --- 
    $: if (data && $page) {
        currentPage = parseInt(data.page || "1", 10);
        loading = false;
    }

    // Initialize updates array if undefined
    if (!data.updates) data.updates = [];

    let showDetails = false;
    let selectedDRep: DRepData | {} = {};

    const toggleDRep = (drep: DRepData) => {
        selectedDRep = drep;
        showDetails = !showDetails;
    };

    function closeDetails() {
        showDetails = false;
    }
</script>


<section class="container mx-auto text-sm">
    <h2 class="text-xl font-bold text-center text-gray-500 mb-4">DRep Updates</h2>
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
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">DRep Id</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Type</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Block</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Txn Hash</th>
            </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
            <!-- Iterate over drep updates data -->
            {#each data.updates as drep, index}
                <tr class="hover:bg-gray-50">
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                        <a href="#" class="text-blue-500">
                            <span on:click={() => {toggleDRep(drep); }}>{drep.drep_id}</span>
                        </a>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{drep.cred_type}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500"><a href="/blocks/{drep.block_number}" class="text-blue-500 hover:underline">{drep.block_number}</a></td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500"><a href="/transactions/{drep.tx_hash}" class="text-blue-500 hover:underline">{drep.tx_hash}</a></td>
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

<DRepDetails drep={selectedDRep} show={showDetails} on:closeDetails={closeDetails}/>
