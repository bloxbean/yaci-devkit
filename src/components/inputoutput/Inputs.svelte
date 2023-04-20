<script>
    import AmountBadges from "../AmountBadges.svelte";

    export let inputs = []
</script>

<div class="md:flex-grow">
    {#each inputs as input}
        <div class="mb-14">
            <div>
                {input.address}
            </div>
            <div class="text-xs">
                {input.tx_hash}#{input.output_index}
            </div>

            <div class="float-right">
                <AmountBadges amounts={input.amount}></AmountBadges>
            </div>
        </div>
    {/each}
</div>
