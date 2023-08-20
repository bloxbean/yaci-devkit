<script>
    import {truncate} from "../../util/util.js";
    import {lovelaceToAda} from "../../util/ada_util.js";

    export let txs;
    export let noOfTxs = 10;
</script>
<div class="bg-white rounded-lg shadow-lg p-6">
    <h2 class="text-xl font-bold mb-4">Transactions</h2>
    <div class="space-y-4">
        {#each [...txs].slice(0, noOfTxs) as tx}
            {#if tx.hash}
                <!-- Transaction Card 1 -->
                <div class="bg-gray-100 p-4 rounded-md">
                    <p class="text-sm text-gray-600 py-1">
                        <span class="font-bold">Tx Hash</span>
                        <span class="text-gray-600 text-right overflow-auto md:overflow-hidden">
                          <a href="/transactions/{tx.hash}"
                             class="text-blue-500 hover:underline">{truncate(tx.hash, 40, '...')}</a>
                        </span>
                    </p>
                    <p class="text-sm text-gray-600 py-1">
                        <span class="font-bold">Block / Slot</span>
                        <a href="/blocks/{tx.block}" class="text-blue-500 hover:underline">{tx.block}</a> / {tx.slot}
                    </p>
                    <p class="text-sm text-gray-600">
                        <span class="font-bold">Output Addresses</span>
                    </p>
                    <div class="flex flex-col items-begin py-1 break-words">
                        {#each tx.output_addresses as address}
                            <p class="text-sm text-gray-600">{truncate(address, 60, "...")}</p>
                        {/each}
                    </div>
                    <p class="text-sm text-gray-600 py-1">
                        <span class="font-bold">Output</span>
                        {lovelaceToAda(tx.output, 2)} Ada
                    </p>
                </div>
            {/if}
        {/each}

    </div>
</div>

<style>

</style>
