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
    import {truncate} from "../../util/util.js";
    import {lovelaceToAda} from "../../util/ada_util.js";

    export let txs;
    export let noOfTxs = 10;
</script>

<div class="mt-2">

        <Table noborder={true}>
            <TableHead
                    class="text-xs text-gray-700 uppercase bg-gray-100 dark:bg-gray-700 dark:text-gray-400"
            >
                <TableHeadCell>Tx Hash</TableHeadCell>
                <TableHeadCell>Block / Slot</TableHeadCell>
                <TableHeadCell>Output Addresses</TableHeadCell>
                <TableHeadCell>Output (Ada)</TableHeadCell>
<!--                <TableHeadCell>Size (KB)</TableHeadCell>-->
<!--                <TableHeadCell>Time</TableHeadCell>-->
            </TableHead>
            <TableBody>
                {#each [...txs].slice(0, noOfTxs) as tx}
                    {#if tx.hash}
                        <TableBodyRow noborder>
                            <TableBodyCell><a href="/transactions/{tx.hash}">{truncate(tx.hash, 20, '...')}</a></TableBodyCell>
                            <TableBodyCell><a href="/blocks/{tx.block}">{tx.block}</a>/{tx.slot}</TableBodyCell>
                            <TableBodyCell>
                                {#each tx.output_addresses as address}
                                    {truncate(address, 25, "...")}<br/>
                                {/each}
                            </TableBodyCell>
                            <TableBodyCell>{lovelaceToAda(tx.output, 2)}</TableBodyCell>
<!--                            <TableBodyCell>{block.size / 1000}</TableBodyCell>-->
<!--                            <TableBodyCell>{moment(block.time).fromNow()}</TableBodyCell>-->
                        </TableBodyRow>
                    {/if}
                {/each}
            </TableBody>
        </Table>

</div>


<style>

</style>
