<script>
    import {lovelaceToAda} from "../../util/ada_util.js";

    import {page} from '$app/stores';
    import {
        Table,
        TableBody,
        TableBodyCell,
        TableBodyRow,
        TableHead,
        TableHeadCell,
        Checkbox,
        TableSearch,
        Card, Pagination, ChevronLeft, ChevronRight, Search, Button
    } from 'flowbite-svelte';
    import {redirect} from "@sveltejs/kit";
    import {goto} from "$app/navigation";

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
    console.log(data.total_pages);
    // for (let i = 1; i <= data.total_pages; i++) {
    //     if ( i < 4)
    //         pages.push({name: i, href: '/transactions?page=' + i + '&count=' + data.count});
    //     else if (i > data.total_pages - 4)
    //         pages.push({name: i, href: '/transactions?page=' + i + '&count=' + data.count});
    //
    // }

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
            prevPage= 1;
        goto(`/transactions?page=${prevPage}&count=${data.count}`)
        // alert('Previous btn clicked. Make a call to your server to fetch data.');
    };
    const next = () => {
        let currentPage = parseInt(data.page);
        let nextPage = currentPage + 1;

        //if (nextPage > parseInt(data.total_pages))
        if (data.txs.length == 0)
            nextPage = currentPage;

        goto(`/transactions?page=${nextPage}&count=${data.count}`)
    };

    //Tx search
    let searchTx = '';
    const handleSearch = () => {
        console.log("serach" + searchTx)
        goto(`/transactions/${searchTx}`)
    };

</script>

<section class="text-gray-600 body-font">
    <div class="container px-5 py-10 mx-auto">
        <form on:submit|preventDefault="{handleSearch}">
            <label for="search" class="mb-2 text-sm font-medium text-gray-900 sr-only dark:text-white">Search</label>
            <div class="relative">
                <div class="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none">
                    <svg aria-hidden="true" class="w-5 h-5 text-gray-500 dark:text-gray-400" fill="none"
                         stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                              d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                    </svg>
                </div>
                <input type="search" id="search" bind:value={searchTx}
                       class="block w-full p-4 pl-10 text-sm text-gray-900 border border-gray-300 rounded-lg bg-gray-50 focus:ring-blue-500 focus:border-blue-500 dark:bg-gray-700 dark:border-gray-600 dark:placeholder-gray-400 dark:text-white dark:focus:ring-blue-500 dark:focus:border-blue-500"
                       placeholder="Transaction Hash" required>
                <button type="submit"
                        class="text-white absolute right-2.5 bottom-2.5 bg-blue-700 hover:bg-blue-800 focus:ring-4 focus:outline-none focus:ring-blue-300 font-medium rounded-lg text-sm px-4 py-2 dark:bg-blue-600 dark:hover:bg-blue-700 dark:focus:ring-blue-800">
                    Search
                </button>
            </div>
        </form>
    </div>
    <div class="container px-5 py-1 mx-auto">
        <Pagination {pages} on:previous={previous} on:next={next} icon>
            <svelte:fragment slot="prev">
                <span class="sr-only">Previous</span>
                <ChevronLeft class="w-5 h-5"/>
            </svelte:fragment>
            <svelte:fragment slot="next">
                <span class="sr-only">Next</span>
                <ChevronRight class="w-5 h-5"/>
            </svelte:fragment>
        </Pagination>
    </div>
    <div class="container px-5 py-1 mx-auto">

        <Table noborder={true}>
            <TableHead
                    class="text-xs text-gray-700 uppercase bg-gray-100 dark:bg-gray-700 dark:text-gray-400"
            >
                <TableHeadCell>Tx Hash</TableHeadCell>
                <TableHeadCell>Block / Slot</TableHeadCell>
                <TableHeadCell>Total Output (Ada)</TableHeadCell>
                <TableHeadCell>Fee (Ada)</TableHeadCell>
                <TableHeadCell>Output Addresses</TableHeadCell>
            </TableHead>
            <TableBody>
                {#each data.txs as tx}
                    <TableBodyRow noborder>
                        <TableBodyCell>
                            <a href="/transactions/{tx.tx_hash}" class="">{tx.tx_hash}</a>
                        </TableBodyCell>
                        <TableBodyCell><a href="/blocks/{tx.block_number}" class="">{tx.block_number}</a> /
                            <br/>{tx.slot}</TableBodyCell>
                        <TableBodyCell>{lovelaceToAda(tx.total_output)} </TableBodyCell>
                        <TableBodyCell>{lovelaceToAda(tx.fee)}</TableBodyCell>
                        <TableBodyCell>
                            {#each tx.output_addresses as address}
                                {truncate(address, 25, "...")}<br/>
                            {/each}
                        </TableBodyCell>
                    </TableBodyRow>

                {/each}
            </TableBody>
        </Table>

        <Pagination {pages} on:previous={previous} on:next={next} icon>
            <svelte:fragment slot="prev">
                <span class="sr-only">Previous</span>
                <ChevronLeft class="w-5 h-5"/>
            </svelte:fragment>
            <svelte:fragment slot="next">
                <span class="sr-only">Next</span>
                <ChevronRight class="w-5 h-5"/>
            </svelte:fragment>
        </Pagination>
    </div>
</section>
