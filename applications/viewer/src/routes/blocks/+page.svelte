<script>
    import {goto} from "$app/navigation";
    import {page} from "$app/stores";

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
        goto(`/blocks?page=${prevPage}&count=${data.count}`)
    };
    const next = () => {
        let currentPage = parseInt(data.page);
        let nextPage = currentPage + 1;

        if (data.blocks.length == 0)
            nextPage = currentPage;

        goto(`/blocks?page=${nextPage}&count=${data.count}`)
    };

    //Block search
    let blockNo = '';
    const handleSearch = () => {
        console.log("serach" + blockNo)
        goto(`/blocks/${blockNo}`)
    };
</script>

<section class="container mx-auto mt-4 text-sm">
    <div class="mb-4 flex justify-center">
        <form class="flex items-center w-full max-w-lg" on:submit|preventDefault="{handleSearch}">
            <input type="search" id="search" bind:value={blockNo}
                   placeholder="Block Number" required
                   class="px-4 py-2 border border-gray-300 rounded-l-md focus:outline-none focus:ring-2 focus:ring-blue-500 w-96 mr-2">
            <button type="submit"
                    class="px-4 py-2 bg-blue-500 text-white rounded-r-md hover:bg-blue-600 focus:outline-none focus:ring-2 focus:ring-blue-500">
                Search
            </button>
        </form>
    </div>
    <!-- Rest of the table code -->
</section>

<section class="container mx-auto text-sm">
    <div class="flex flex-wrap justify-between mb-2">
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
                <th class="py-2 px-4 bg-gray-100 font-bold text-center">Blocks</th>
                <th class="py-2 px-4 bg-gray-100 font-bold text-center">Slot</th>
                <th class="py-2 px-4 bg-gray-100 font-bold text-center">Pool</th>
                <th class="py-2 px-4 bg-gray-100 font-bold text-center">Size (kb)</th>
                <th class="py-2 px-4 bg-gray-100 font-bold text-center"># of Txs</th>
            </tr>
            </thead>
            <tbody>
            {#each data.blocks as block, index}
                <tr class="{index % 2 === 0 ? 'bg-gray-50' : 'bg-white'}">
                    <td class="py-2 px-4 text-center">
                        <a href="/blocks/{block.number}" class="text-blue-500">{block.number}</a>
                    </td>
                    <td class="py-2 px-4 text-center">{block.slot}</td>
                    <td class="py-2 px-4 text-center">{block.slot_leader}</td>
                    <td class="py-2 px-4 text-center">{block.size / 1000}</td>
                    <td class="py-2 px-4 text-center">{block.tx_count}</td>
                </tr>
            {/each}
            </tbody>
        </table>
    </div>
    <div class="flex flex-wrap justify-between mt-2 mb-4">
        <a href="#"
           class="px-4 py-2 text-blue-500 font-medium rounded-md bg-gray-100 hover:bg-gray-200 transition-colors"
           role="button" on:click={previous}>&lt; Previous</a>
        <a href="#"
           class="px-4 py-2 text-blue-500 font-medium rounded-md bg-gray-100 hover:bg-gray-200 transition-colors"
           role="button" on:click={next}>Next &gt;</a>
    </div>
</section>
