<script>
    import { lovelaceToAda } from "../../util/ada_util.js";
    import { page } from '$app/stores';
    import { goto } from "$app/navigation";

    export let data;

    function join(array) {
        return array.join('\n');
    }

    var truncate = function (fullStr, strLen, separator) {
        if (fullStr.length <= strLen) return fullStr;

        separator = separator || '...';

        var sepLen = separator.length,
            charsToShow = strLen - sepLen,
            frontChars = Math.ceil(charsToShow / 2),
            backChars = Math.floor(charsToShow / 2);

        return fullStr.substr(0, frontChars) +
            separator +
            fullStr.substr(fullStr.length - backChars);
    };

    $: activeUrl = $page.url.searchParams.get('page')

    let pages = [];

    $: {
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
        goto(`/transactions?page=${prevPage}&count=${data.count}`)
    };

    const next = () => {
        let currentPage = parseInt(data.page);
        let nextPage = currentPage + 1;

        if (data.txs.length == 0)
            nextPage = currentPage;

        goto(`/transactions?page=${nextPage}&count=${data.count}`)
    };

    // Tx search
    let searchTx = '';
    const handleSearch = () => {
        goto(`/transactions/${searchTx}`)
    };
</script>

<section class="container mx-auto mt-4 px-4">
    <div class="mb-4 flex justify-center">
        <form class="flex items-center w-full max-w-lg" on:submit|preventDefault={handleSearch}>
            <input type="search" id="search" bind:value={searchTx}
                   placeholder="Transaction Hash" required
                   class="px-4 py-2 border border-gray-300 rounded-l-md focus:outline-none focus:ring-2 focus:ring-blue-500 w-full">
            <button type="submit" class="px-4 py-2 bg-blue-500 text-white rounded-r-md hover:bg-blue-600 focus:outline-none focus:ring-2 focus:ring-blue-500">
                Search
            </button>
        </form>
    </div>
</section>

<section class="container mx-auto text-sm px-4">
    <div class="flex flex-wrap justify-between mb-2">
        <button class="px-4 py-2 text-blue-500 font-medium rounded-md bg-gray-100 hover:bg-gray-200 transition-colors" on:click={previous}>
            &lt; Previous
        </button>
        <button class="px-4 py-2 text-blue-500 font-medium rounded-md bg-gray-100 hover:bg-gray-200 transition-colors" on:click={next}>
            Next &gt;
        </button>
    </div>
    <div class="overflow-x-auto">
        <table class="w-full bg-white border border-gray-300">
            <thead>
            <tr>
                <th class="py-2 px-4 bg-gray-100 font-bold">Tx Hash</th>
                <th class="py-2 px-4 bg-gray-100 font-bold text-center">Block / Slot</th>
                <th class="py-2 px-4 bg-gray-100 font-bold text-center">Total Output (Ada)</th>
                <th class="py-2 px-4 bg-gray-100 font-bold text-center">Fee (Ada)</th>
                <th class="py-2 px-4 bg-gray-100 font-bold">Output Addresses</th>
            </tr>
            </thead>
            <tbody>
            {#each data.txs as tx, index}
                <tr class="{index % 2 === 0 ? 'bg-gray-50' : 'bg-white'}">
                    <td class="py-2 px-4">
                        <a href="/transactions/{tx.tx_hash}" class="text-blue-500">
                            <span class="block md:hidden">{tx.tx_hash.slice(0, 6)}...</span>
                            <span class="hidden md:block break-all">{tx.tx_hash}</span>
                        </a>
                    </td>
                    <td class="py-2 px-4 text-center">
                        <a href="/blocks/{tx.block_number}" class="text-blue-500">{tx.block_number}</a> / <br>{tx.slot}
                    </td>
                    <td class="py-2 px-4 text-center">{lovelaceToAda(tx.total_output)}</td>
                    <td class="py-2 px-4 text-center">{lovelaceToAda(tx.fee)}</td>
                    <td class="py-2 px-4">
                        {#each tx.output_addresses as address}
                            {truncate(address, 25, "...")}<br>
                        {/each}
                    </td>
                </tr>
            {/each}
            </tbody>
        </table>
    </div>
    <div class="flex flex-wrap justify-between mt-2 mb-4">
        <button class="px-4 py-2 text-blue-500 font-medium rounded-md bg-gray-100 hover:bg-gray-200 transition-colors" on:click={previous}>
            &lt; Previous
        </button>
        <button class="px-4 py-2 text-blue-500 font-medium rounded-md bg-gray-100 hover:bg-gray-200 transition-colors" on:click={next}>
            Next &gt;
        </button>
    </div>
</section>
