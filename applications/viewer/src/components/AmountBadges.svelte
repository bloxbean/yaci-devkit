<script>
    import {lovelaceToAda} from "../util/ada_util.js";

    export let amounts = []

    const colors = ['badge-neutral', 'badge-primary', 'badge-secondary', 'badge-accent', 'badge-ghost']

    function random_color() {
        return colors[Math.floor(Math.random() * colors.length)];
    }
</script>

{#if amounts}
    {#each amounts as amount}
        {#if amount.asset_name === 'lovelace'}
            <span class="badge badge-red">{lovelaceToAda(amount.quantity)} Ada</span>
        {:else}
    <span class="badge {random_color()}">
      {amount.quantity} {amount.asset_name}
    </span>
        {/if}
    {/each}
{/if}
