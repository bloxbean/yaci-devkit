<script lang="ts">
    import {goto} from "$app/navigation";
    import {page} from "$app/stores";
    import {truncate} from "$lib/util";
    import { onMount } from 'svelte';

    export let data;

    interface StakeDeregistration {
        stake_address: string;
        block_number: number;
        tx_hash: string;
    }

    interface PageLink {
        href: string;
        active: boolean;
    }

    // Add toast state
    let showToast = false;
    let toastTimeout: number;
    let toastMessage = '';

    // Add clipboard function with toast
    async function copyToClipboard(text: string, message: string) {
        try {
            await navigator.clipboard.writeText(text);
            showToast = true;
            toastMessage = message;
            // Clear any existing timeout
            if (toastTimeout) {
                clearTimeout(toastTimeout);
            }
            // Hide toast after 2 seconds
            toastTimeout = setTimeout(() => {
                showToast = false;
            }, 2000);
        } catch (err) {
            console.error('Failed to copy text: ', err);
        }
    }

    // Cleanup timeout on component destroy
    onMount(() => {
        return () => {
            if (toastTimeout) {
                clearTimeout(toastTimeout);
            }
        };
    });

    $: activeUrl = $page.url.searchParams.get('page');
    let pages: PageLink[] = [];

    // ... rest of existing script ...
</script>

<section class="container mx-auto text-sm">
    <!-- Toast Notification -->
    {#if showToast}
        <div class="fixed bottom-4 right-4 bg-green-500 text-white px-4 py-2 rounded-lg shadow-lg transition-opacity duration-300 z-50">
            {toastMessage}
        </div>
    {/if}

    <h2 class="text-xl font-bold text-center text-gray-500 mb-4">Stake Deregistrations</h2>
    <!-- ... existing buttons ... -->
    <div class="overflow-x-auto bg-white shadow-md rounded-lg mb-4">
        <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50">
            <tr>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Address</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Block</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Transaction Hash</th>
            </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
            {#each data.stakeDeregistrations as deregistration: StakeDeregistration, index}
                <tr class="hover:bg-gray-50">
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                        <div class="flex items-center gap-2">
                            <span>{truncate(deregistration.address, 20, "...")}</span>
                            <button 
                                class="text-gray-400 hover:text-gray-600" 
                                on:click={() => copyToClipboard(deregistration.address, 'Address copied to clipboard!')}
                                title="Copy address"
                            >
                                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
                                </svg>
                            </button>
                        </div>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        <a href="/blocks/{deregistration.block_number}" class="text-blue-500 hover:underline">
                            {deregistration.block_number}
                        </a>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                        <div class="flex items-center gap-2">
                            <a href="/transactions/{deregistration.tx_hash}" class="text-blue-500 hover:underline">
                                <span>{truncate(deregistration.tx_hash, 20)}</span>
                            </a>
                            <button 
                                class="text-gray-400 hover:text-gray-600" 
                                on:click={() => copyToClipboard(deregistration.tx_hash, 'Transaction hash copied to clipboard!')}
                                title="Copy transaction hash"
                            >
                                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
                                </svg>
                            </button>
                        </div>
                    </td>
                </tr>
            {/each}
            </tbody>
        </table>
    </div>
    <!-- ... rest of existing code ... -->
</section>

<style>
    /* Add transition for smooth appearance/disappearance */
    .fixed {
        transition: opacity 0.3s ease-in-out;
        z-index: 50;
    }
</style> 