<script>
    import {lovelaceToAda} from "../util/ada_util.js";
    import {Badge} from "flowbite-svelte";

    export let amounts = []

    const colors = ['blue', 'red', 'green', 'yellow', 'indigo']
    function random_color() {
        return colors[Math.floor(Math.random() * colors.length)];
    }
</script>

{#each amounts as amount}
    {#if amount.asset_name === 'lovelace'}
        <Badge color={random_color()} class="ml-2"> {lovelaceToAda(amount.quantity)} Ada</Badge>
    {:else}

        <Badge color={random_color()} class="ml-2">
            {amount.quantity} {amount.asset_name}
        </Badge>
        <!--                                    <br/> {amount.policyId}-->

    {/if}

{/each}
