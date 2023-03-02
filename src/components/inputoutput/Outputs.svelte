<script>
    import AmountBadges from "../AmountBadges.svelte";

    export let outputs = [];
</script>

<div class="md:flex-grow">
    {#each outputs as output}
        <div class="mb-14">
            <div>
                {output.owner_addr}
            </div>
            <div class="float-right">
                <AmountBadges amounts={output.amounts}></AmountBadges>
            </div>
        </div>
    {/each}
</div>
