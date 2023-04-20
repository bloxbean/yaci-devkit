<script>
    import AmountBadges from "../AmountBadges.svelte";

    export let outputs = [];
</script>

<div class="md:flex-grow">
    {#each outputs as output}
        <div class="mb-14">
            <div>
                {output.address}
            </div>
            <div class="float-right">
                <AmountBadges amounts={output.amount}></AmountBadges>
            </div>
        </div>
    {/each}
</div>
