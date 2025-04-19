<script lang="ts">
    import {goto} from "$app/navigation";
    import {page} from "$app/stores";
    import {truncate} from "$lib/util";
    import {EyeIcon} from "svelte-feather-icons";
    import { onMount } from 'svelte';

    export let data;

    interface Vote {
        tx_hash: string;
        block_number: number;
        gov_action_tx_hash: string;
        gov_action_index: number;
        voter_type: string;
        vote: string;
        voter_hash: string;
        anchor_url?: string;
        anchor_hash?: string;
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
    $: hasMore = data.votes && data.votes.length > 0;

    // --- Navigation Function --- 
    function goToPage(targetPage: number) {
        if (targetPage < 1 || loading) return;
        loading = true;
        const url = `/governance/votes?page=${targetPage}&count=${ITEMS_PER_PAGE}`;
        goto(url);
    }

    // --- Reactive Update Block --- 
    $: if (data && $page) {
        currentPage = parseInt(data.page || "1", 10);
        loading = false;
    }

    // Initialize votes array if undefined
    if (!data.votes) data.votes = [];
</script>


<section class="container mx-auto text-sm">
    <h2 class="text-xl font-bold text-center text-gray-500 mb-4">Votes</h2>
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
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Block</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Gov Action Id</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Voter Type</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Vote</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Voter Hash</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Anchor URL</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Anchor Hash</th>
            </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
            {#each data.votes as vote: Vote, index}
                <tr class="hover:bg-gray-50">
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                        <a href="/transactions/{vote.tx_hash}" class="text-blue-500 hover:underline">
                            <span>{truncate(vote.tx_hash, 30, "...")}</span>
                        </a>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        <a href="/blocks/{vote.block_number}" class="text-blue-500 hover:underline">
                            {vote.block_number}
                        </a>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{truncate(vote.gov_action_tx_hash, 10, "...")}#{vote.gov_action_index}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{vote.voter_type}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{vote.vote}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{truncate(vote.voter_hash, 30, "...")}</td>
                    {#if vote.anchor_url}
                        <td class="px-6 py-4 whitespace-nowrap text-sm text-blue-500">
                            <a href="{vote.anchor_url}" target="_blank" class="hover:underline">{truncate(vote.anchor_url, 20, "...")}</a>
                        </td>
                        <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{truncate(vote.anchor_hash, 20, "...")}</td>
                    {:else }
                        <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500 text-center">-</td>
                        <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500 text-center">-</td>
                    {/if}
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
