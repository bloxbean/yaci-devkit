<script lang="ts">
    import {lovelaceToAda} from "../util/ada_util.js";
    import {CopyIcon} from "svelte-feather-icons";

    interface Amount {
        asset_name: string;
        quantity: string;
    }

    export let amounts: Amount[] = [];

    const colors = ['badge-neutral', 'badge-primary', 'badge-secondary', 'badge-accent', 'badge-ghost'];

    function random_color() {
        return colors[Math.floor(Math.random() * colors.length)];
    }

    function truncateAssetName(name: string, maxLength: number = 20): string {
        if (name.length <= maxLength) return name;
        return name.substring(0, maxLength) + '...';
    }

    async function copyToClipboard(text: string) {
        try {
            await navigator.clipboard.writeText(text);
        } catch (err) {
            console.error('Failed to copy text: ', err);
        }
    }
</script>

{#if amounts}
    {#each amounts as amount}
        {#if amount.asset_name === 'lovelace'}
            <span class="badge badge-red">{lovelaceToAda(amount.quantity)} Ada</span>
        {:else}
            <div class="badge {random_color()} flex items-center gap-1">
                <span>{amount.quantity}</span>
                <div class="flex items-center gap-1">
                    <span class="tooltip" data-tip={amount.asset_name}>
                        {truncateAssetName(amount.asset_name)}
                    </span>
                    <button 
                        class="text-gray-400 hover:text-gray-600" 
                        on:click={() => copyToClipboard(amount.asset_name)}
                        title="Copy asset name"
                    >
                        <CopyIcon class="h-3 w-3"/>
                    </button>
                </div>
            </div>
        {/if}
    {/each}
{/if}
