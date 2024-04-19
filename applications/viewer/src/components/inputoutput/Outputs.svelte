<script>
    import AmountBadges from "../AmountBadges.svelte";

    export let outputs = [];
</script>

<div>
    {#each outputs as output}
        <div class="mt-4">
            <div class="text-sm font-medium text-gray-700 md:ml-2 break-words">{output.address}</div>
            <div class="flex justify-end mt-2">
                <div class="space-x-2">
                    <AmountBadges amounts={output.amount}></AmountBadges>
                </div>
            </div>
        </div>
        <hr class="my-4 border-gray-100">
    {/each}
</div>

