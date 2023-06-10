<script>
    import moment from 'moment';
    import {getDate} from "../../util/util.js";

    export let blocks;
    export let noOfBlocks = 10;
</script>

<div class="bg-white rounded-lg shadow-lg p-6">
    <h2 class="text-xl font-bold mb-4">Recent Blocks</h2>
    <div class="space-y-4">
        {#each [...blocks].slice(0, noOfBlocks) as block}
            {#if block.number}
                <!-- Block Card 1 -->
                <div class="bg-gray-100 p-4 rounded-md">
                    <h3 class="text-sm font-bold">
                        <span class="font-bold">Block</span>
                        <a href="/blocks/{block.number}" class="text-blue-500 hover:underline">{block.number}</a>
                    </h3>
                    <p class="text-sm text-gray-600"><span class="font-bold">Slot</span> {block.slot}</p>
                    <p class="text-sm text-gray-600"><span class="font-bold">Txs</span> {block.ntx}</p>
                    <p class="text-sm text-gray-600"><span class="font-bold">Size</span> {block.size / 1000}</p>
                    <p class="text-sm text-gray-600"><span class="font-bold">Pool</span> {block.slot_leader}</p>
                    <p class="text-sm text-gray-600">
                        <span class="text-xs text-gray-500">
                          {getDate(block.block_time)}, {moment(block.block_time * 1000).fromNow()}
                        </span>
                    </p>
                </div>

            {/if}
        {/each}
    </div>
</div>


<style>

</style>
