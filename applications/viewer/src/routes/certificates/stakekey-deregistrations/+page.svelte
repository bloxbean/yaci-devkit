<script>
    import {goto} from "$app/navigation";
    import {page} from "$app/stores";
    import {truncate, getDate} from "../../../util/util.js";

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
        goto(`/certificates/stakekey-deregistrations?page=${prevPage}&count=${data.count}`)
    };
    const next = () => {
        let currentPage = parseInt(data.page);
        let nextPage = currentPage + 1;

        if (data.deregistrations.length == 0)
            nextPage = currentPage;

        goto(`/certificates/stakekey-deregistrations?page=${nextPage}&count=${data.count}`)
    };

    console.log(data);
    if (!data.deregistrations)
        data.deregistrations = []
</script>


<section class="container mx-auto text-sm">
    <h2 class="text-xl font-bold text-center text-gray-500 mb-4">Stake Key De-registrations</h2>
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
                <th class="py-2 px-4 bg-gray-100 font-bold text-center">Address</th>
                <th class="py-2 px-4 bg-gray-100 font-bold text-center">Block</th>
                <th class="py-2 px-4 bg-gray-100 font-bold text-center">Time</th>
                <th class="py-2 px-4 bg-gray-100 font-bold text-center">Transaction Hash</th>
            </tr>
            </thead>
            <tbody>
            <!-- Iterate over stake deregistrations data -->
            {#each data.deregistrations as deregistration, index}
                <tr class="{index % 2 === 0 ? 'bg-gray-50' : 'bg-white'}">
                    <td class="py-2 px-4">{deregistration.address}</td>
                    <td class="py-2 px-4 text-center">
                        <a href="/blocks/{deregistration.block_number}" class="text-blue-500">
                            {deregistration.block_number}
                        </a>
                    </td>
                    <td class="py-2 px-4 text-center">{getDate(deregistration.block_time)}</td>
                    <td class="py-2 px-4">
                        <a href="/transactions/{deregistration.tx_hash}" class="text-blue-500">
                            <span class="ml-2">{truncate(deregistration.tx_hash, 30, "...")}</span>
                        </a>
                    </td>
                </tr>
            {/each}
            </tbody>
        </table>
    </div>
    <div class="flex flex-wrap justify-between mb-2">
        <a href="#"
           class="px-4 py-2 text-blue-500 font-medium rounded-md bg-gray-100 hover:bg-gray-200 transition-colors"
           role="button" on:click={previous}>&lt; Previous</a>
        <a href="#"
           class="px-4 py-2 text-blue-500 font-medium rounded-md bg-gray-100 hover:bg-gray-200 transition-colors"
           role="button" on:click={next}>Next &gt;</a>
    </div>
</section>
