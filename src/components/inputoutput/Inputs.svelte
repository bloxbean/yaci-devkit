<script>
    import AmountBadges from "../AmountBadges.svelte";

    export let inputs = []
</script>

<div>
    {#each inputs as input}
        <div class="mt-4">
            <div class="text-sm font-medium text-gray-700 md:ml-2 break-words">{input.address}</div>
            <div class="text-xs text-gray-500">{input.tx_hash}#{input.output_index}</div>
            <div class="flex justify-end mt-2">
                <div class="space-x-2">
                    <AmountBadges amounts={input.amount}></AmountBadges>
                </div>
            </div>
        </div>
        <hr class="my-4 border-gray-100">
    {/each}
</div>
