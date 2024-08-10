<script>
    import {goto} from "$app/navigation";
    import {page} from "$app/stores";
    import {truncate, getDate} from "../../../util/util.js";
    import {EyeIcon} from "svelte-feather-icons";

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
        goto(`/governance/votes?page=${prevPage}&count=${data.count}`)
    };
    const next = () => {
        let currentPage = parseInt(data.page);
        let nextPage = currentPage + 1;

        if (data.votes.length == 0)
            nextPage = currentPage;

        goto(`/governance/votes?page=${nextPage}&count=${data.count}`)
    };

    console.log(data);
    if (!data.votes)
        data.votes = []
</script>


<section class="container mx-auto text-sm">
    <h2 class="text-xl font-bold text-center text-gray-500 mb-4">Votes</h2>
    <div class="flex flex-wrap justify-between mt-4 mb-2">
        <button
           class="px-4 py-2 text-blue-500 font-medium rounded-md bg-gray-100 hover:bg-gray-200 transition-colors"
           role="button" on:click={previous}>&lt; Previous</button>
        <button
           class="px-4 py-2 text-blue-500 font-medium rounded-md bg-gray-100 hover:bg-gray-200 transition-colors"
           role="button" on:click={next}>Next &gt;</button>
    </div>
    <div class="overflow-x-auto">
        <table class="w-full bg-white border border-gray-300">
            <thead>
            <tr>
                <th class="py-2 px-4 bg-gray-100 font-bold text-center">Transaction Hash</th>
                <th class="py-2 px-4 bg-gray-100 font-bold text-center">Block</th>
                <th class="py-2 px-4 bg-gray-100 font-bold text-center">Gov Action Id</th>
                <th class="py-2 px-4 bg-gray-100 font-bold text-center">Voter Type</th>
                <th class="py-2 px-4 bg-gray-100 font-bold text-center">Vote</th>
                <th class="py-2 px-4 bg-gray-100 font-bold text-center">Voter Hash</th>
                <th class="py-2 px-4 bg-gray-100 font-bold text-center">Anchor URL</th>
                <th class="py-2 px-4 bg-gray-100 font-bold text-center">Anchor Hash</th>
            </tr>
            </thead>
            <tbody>
            <!-- Iterate over stake registrations data -->
            {#each data.votes as vote, index}
                <tr class="{index % 2 === 0 ? 'bg-gray-50' : 'bg-white'}">
                    <td class="py-2 px-4">
                        <a href="/transactions/{vote.tx_hash}" class="text-blue-500">
                            <span class="ml-2">{truncate(vote.tx_hash, 30, "...")}</span>
                        </a>
                    </td>
                    <td class="py-2 px-4 text-center">
                        <a href="/blocks/{vote.block_number}" class="text-blue-500">
                            {vote.block_number}
                        </a>
                    </td>
                    <td class="py-2 px-4">{vote.gov_action_tx_hash}#{vote.gov_action_index}</td>
                    <td class="py-2 px-4 text-center">{vote.voter_type}</td>
                    <td class="py-2 px-4 text-center">{vote.vote}</td>
                    <td class="py-2 px-4">{truncate(vote.voter_hash, 30, "...")}</td>
                    {#if vote.anchor_url}
                        <td class="py-2 px-4"><a href="{vote.anchor_url}" target="_blank">{vote.anchor_url}</a></td>
                        <td class="py-2 px-4">{vote.anchor_hash}</td>
                    {:else }
                        <td class="py-2 px-4 text-center">_</td>
                        <td class="py-2 px-4 text-center">_</td>
                    {/if}
                </tr>
            {/each}
            </tbody>
        </table>
    </div>
    <div class="flex flex-wrap justify-between mt-2 mb-2">
        <button
           class="px-4 py-2 text-blue-500 font-medium rounded-md bg-gray-100 hover:bg-gray-200 transition-colors"
           role="button" on:click={previous}>&lt; Previous</button>
        <button
           class="px-4 py-2 text-blue-500 font-medium rounded-md bg-gray-100 hover:bg-gray-200 transition-colors"
           role="button" on:click={next}>Next &gt;</button>
    </div>
</section>
