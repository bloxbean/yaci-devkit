<script>
    import {
        Table,
        TableBody,
        TableBodyCell,
        TableBodyRow,
        TableHead,
        TableHeadCell,
        Checkbox,
        TableSearch,
        Card
    } from 'flowbite-svelte';

    import moment from 'moment';

    export let blocks;
    export let noOfBlocks = 10;
</script>

<div class="mt-2">

        <Table noborder={true}>
            <TableHead
                    class="text-xs text-gray-700 uppercase bg-gray-100 dark:bg-gray-700 dark:text-gray-400"
            >
                <TableHeadCell>Block</TableHeadCell>
                <TableHeadCell>Slot</TableHeadCell>
                <TableHeadCell>Txns</TableHeadCell>
                <TableHeadCell>Size (KB)</TableHeadCell>
                <TableHeadCell>Time</TableHeadCell>
            </TableHead>
            <TableBody>
                {#each [...blocks].slice(0, noOfBlocks) as block}
                    {#if block.number}
                        <TableBodyRow noborder>
                            <TableBodyCell><a href="/blocks/{block.number}"> {block.number}</a></TableBodyCell>
                            <TableBodyCell>{block.slot}</TableBodyCell>
                            <TableBodyCell>{block.ntx}</TableBodyCell>
                            <TableBodyCell>{block.size / 1000}</TableBodyCell>
                            <TableBodyCell>{moment(block.time).fromNow()}</TableBodyCell>
                        </TableBodyRow>
                    {/if}
                {/each}
            </TableBody>
        </Table>

</div>


<style>

</style>
