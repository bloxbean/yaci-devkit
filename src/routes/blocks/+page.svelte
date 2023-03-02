<script>
    import Blocks from "./Blocks.svelte";
    import {
        Table,
        TableBody,
        TableBodyCell,
        TableBodyRow,
        TableHead,
        TableHeadCell,
        Checkbox,
        TableSearch,
        Card, Pagination, ChevronLeft, ChevronRight
    } from 'flowbite-svelte';
    import {goto} from "$app/navigation";
    import {page} from "$app/stores";

    export let data;

    $: activeUrl = $page.url.searchParams.get('page')
    let pages = [];
    console.log(data.total_pages);
    // for (let i = data.page; i <= data.total_pages; i++) {
    //
    //         // pages.push({name: i, href: '/blocks?page=' + i + '&count=' + data.count});
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
        goto(`/blocks?page=${prevPage}&count=${data.count}`)
        // alert('Previous btn clicked. Make a call to your server to fetch data.');
    };
    const next = () => {
        let currentPage = parseInt(data.page);
        let nextPage = currentPage + 1;

        if (nextPage > parseInt(data.total_pages))
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

<section class="text-gray-600 body-font">
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
                <TableHeadCell>Blocks</TableHeadCell>
                <TableHeadCell>Slot</TableHeadCell>
                <TableHeadCell>Issuer Vkey</TableHeadCell>
                <TableHeadCell>Size (kb)</TableHeadCell>
                <TableHeadCell># of Txs</TableHeadCell>
            </TableHead>
            <TableBody>
                {#each data.blocks as block}
                        <TableBodyRow noborder>
                            <TableBodyCell>
                                <a href="/blocks/{block.number}" class="">{block.number}</a>
                            </TableBodyCell>
                            <TableBodyCell>{block.slot}</TableBodyCell>
                            <TableBodyCell>{block.issuer_vkey} </TableBodyCell>
                            <TableBodyCell>{block.block_body_size/1000}</TableBodyCell>
                            <TableBodyCell>
                               {block.no_of_txs}
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
