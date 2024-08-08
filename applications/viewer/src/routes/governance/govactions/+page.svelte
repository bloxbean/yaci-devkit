<script>
    import {goto} from "$app/navigation";
    import {page} from "$app/stores";
    import {truncate, getDate} from "../../../util/util.js";
    import { AirplayIcon, AtSignIcon, EyeIcon} from 'svelte-feather-icons'


    export let data;

    $: activeUrl = $page.url.searchParams.get('page')
    let pages = [];

    $:{
        pages.forEach((page) => {
            let splitUrl = page.href.split('?');
            let queryString = splitUrl.slice(1).join('?');
            const hrefParams = new URLSearchParams(queryString);
            let hrefValue = hrefParams.get('page');
            if (hrefValue === activeUrl) {
                page.active = true
            } else {
                page.active = false
            }
        })
        pages = pages
    }

    const previous = () => {
        let currentPage = parseInt(data.page);
        let prevPage = currentPage - 1;
        if (prevPage <= 0)
            prevPage = 1;
        goto(`/governance/govactions?page=${prevPage}&count=${data.count}`)
    };
    const next = () => {
        let currentPage = parseInt(data.page);
        let nextPage = currentPage + 1;

        if (data.govactions.length == 0)
            nextPage = currentPage;

        goto(`/governance/govactions?page=${nextPage}&count=${data.count}`)
    };

    console.log(data);
    if (!data.govactions)
        data.govactions = []

    let showModal = false;
    let selectedDetails = null;

    function showDetails(details) {
        selectedDetails = details;
        showModal = true;
    }

    function closeModal() {
        showModal = false;
    }
</script>


<section class="container mx-auto text-sm">
    <h2 class="text-xl font-bold text-center text-gray-500 mb-4">Proposals</h2>
    <div class="flex flex-wrap justify-between mt-4 mb-2">
        <a href="#"
           class="px-4 py-2 text-blue-500 font-medium rounded-md bg-gray-100 hover:bg-gray-200 transition-colors"
           role="button" on:click={previous}>&lt; Previous</a>
        <a href="#"
           class="px-4 py-2 text-blue-500 font-medium rounded-md bg-gray-100 hover:bg-gray-200 transition-colors"
           role="button" on:click={next}>Next &gt;</a>
    </div>
    <div class="overflow-x-auto">
        <table class="w-full bg-white border border-gray-300">
            <thead>
            <tr>
                <th class="py-2 px-4 bg-gray-100 font-bold text-center">Transaction Hash</th>
                <th class="py-2 px-4 bg-gray-100 font-bold text-center">Index</th>
                <th class="py-2 px-4 bg-gray-100 font-bold text-center">Block</th>
                <th class="py-2 px-4 bg-gray-100 font-bold text-center">Action Type</th>
                <th class="py-2 px-4 bg-gray-100 font-bold text-center">Return Address</th>
                <th class="py-2 px-4 bg-gray-100 font-bold text-center">Details</th>
            </tr>
            </thead>
            <tbody>
            <!-- Iterate over stake registrations data -->
            {#each data.govactions as govaction, index}
                <tr class="{index % 2 === 0 ? 'bg-gray-50' : 'bg-white'}">
                    <td class="py-2 px-4">
                        <a href="/transactions/{govaction.tx_hash}" class="text-blue-500">
                            <span class="ml-2">{truncate(govaction.tx_hash, 30, "...")}</span>
                        </a>
                    </td>
                    <td class="py-2 px-4 text-center">{govaction.index}</td>
                    <td class="py-2 px-4 text-center">
                        <a href="/blocks/{govaction.block_number}" class="text-blue-500">
                            {govaction.block_number}
                        </a>
                    </td>
                    <td class="py-2 px-4 text-center">{govaction.type}</td>
                    <td class="py-2 px-4 tex">{govaction.return_address}</td>
                    <td class="py-2 px-4 text-center"> <a href="/governance/govactions/{govaction.tx_hash}_{govaction.index}" target="_blank"><EyeIcon size="1.5x"/></a></td>
                    <!--{#if govaction.anchor_url}-->
                    <!--    <td class="py-2 px-4"><a href="{govaction.anchor_url}" target="_blank">{govaction.anchor_url}</a></td>-->
                    <!--    <td class="py-2 px-4">{govaction.anchor_hash}</td>-->
                    <!--{:else }-->
                    <!--    <td class="py-2 px-4">_</td>-->
                    <!--    <td class="py-2 px-4">_</td>-->
                    <!--{/if}-->
                </tr>
            {/each}
            </tbody>
        </table>
    </div>
    <div class="flex flex-wrap justify-between mt-2 mb-2">
        <a href="#"
           class="px-4 py-2 text-blue-500 font-medium rounded-md bg-gray-100 hover:bg-gray-200 transition-colors"
           role="button" on:click={previous}>&lt; Previous</a>
        <a href="#"
           class="px-4 py-2 text-blue-500 font-medium rounded-md bg-gray-100 hover:bg-gray-200 transition-colors"
           role="button" on:click={next}>Next &gt;</a>
    </div>
</section>
