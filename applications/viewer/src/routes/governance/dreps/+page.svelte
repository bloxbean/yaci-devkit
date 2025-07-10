<script lang="ts">
    import { goto } from "$app/navigation";
    import { page } from "$app/stores";
    import { formatAda } from "$lib/util";
    import { onMount } from 'svelte';

    export let data;

    interface Drep {
        drep_id: string | null;
        drep_hash: string | null;
        deposit: number;
        status: string;
        voting_power: number | null;
        registration_slot: number;
        drep_type: string | null;
    }

    // Add toast state
    let showToast = false;
    let toastTimeout: number;
    let toastMessage = '';

    function getStatusClass(status: string): string {
        switch (status.toUpperCase()) {
            case 'ACTIVE':
                return 'badge badge-success';
            case 'INACTIVE':
                return 'badge badge-error';
            default:
                return 'badge badge-ghost';
        }
    }

    function formatVotingPower(power: number | null): string {
        if (power === null) return '';
        return formatAda(power);
    }

    function truncateHash(hash: string | null): string {
        if (!hash) return '';
        return hash.substring(0, 8) + '...' + hash.substring(hash.length - 8);
    }

    async function copyToClipboard(text: string | null, message: string) {
        if (!text) return;
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

    const previous = () => {
        let currentPage = parseInt(data.page);
        let prevPage = currentPage - 1;
        if (prevPage <= 0)
            prevPage = 1;
        goto(`/governance/dreps?page=${prevPage}&count=${data.count}`)
    };

    const next = () => {
        let currentPage = parseInt(data.page);
        let nextPage = currentPage + 1;

        if (data.dreps.length == 0)
            nextPage = currentPage;

        goto(`/governance/dreps?page=${nextPage}&count=${data.count}`)
    };

    const itemsPerPage = parseInt(data.count);
    const currentPage = parseInt(data.page);
    const totalPages = Math.ceil(data.dreps.length / itemsPerPage);

    $: hasMore = data.dreps.length === itemsPerPage;
</script>

<section class="container mx-auto text-sm">
    <!-- Toast Notification -->
    {#if showToast}
        <div class="fixed bottom-4 right-4 bg-green-500 text-white px-4 py-2 rounded-lg shadow-lg transition-opacity duration-300 z-50">
            {toastMessage}
        </div>
    {/if}

    <h2 class="text-xl font-bold text-center text-gray-500 mb-4">DReps</h2>
    <div class="flex justify-end mt-4">
        <div class="join">
            <button 
                class="join-item btn btn-sm" 
                disabled={currentPage === 1}
                on:click={previous}
            >
                «
            </button>
            <button class="join-item btn btn-sm">Page {currentPage}</button>
            <button 
                class="join-item btn btn-sm" 
                disabled={!hasMore}
                on:click={next}
            >
                »
            </button>
        </div>
    </div>
    <div class="overflow-x-auto bg-white shadow-md rounded-lg mb-4">
        <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50">
            <tr>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">DRep ID</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">DRep Hash</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Deposit</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Voting Power</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Type</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Registration Slot</th>
            </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
            {#each data.dreps as drep}
                <tr class="hover:bg-gray-50">
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                        {#if drep.drep_id}
                            <div class="flex items-center gap-2">
                                <span>{drep.drep_id}</span>
                                <button 
                                    class="text-gray-400 hover:text-gray-600"
                                    on:click={() => copyToClipboard(drep.drep_id, 'DRep ID copied to clipboard!')}
                                    title="Copy DRep ID"
                                >
                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
                                    </svg>
                                </button>
                            </div>
                        {/if}
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {#if drep.drep_hash}
                            <div class="flex items-center gap-2">
                                <span>{truncateHash(drep.drep_hash)}</span>
                                <button 
                                    class="text-gray-400 hover:text-gray-600"
                                    on:click={() => copyToClipboard(drep.drep_hash, 'DRep Hash copied to clipboard!')}
                                    title="Copy DRep Hash"
                                >
                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
                                    </svg>
                                </button>
                            </div>
                        {/if}
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        <span class={getStatusClass(drep.status)}>{drep.status}</span>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {formatAda(drep.deposit)}
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {formatVotingPower(drep.voting_power)}
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {drep.drep_type || ''}
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {drep.registration_slot}
                    </td>
                </tr>
            {/each}
            </tbody>
        </table>
    </div>

    <div class="flex justify-end mt-4">
        <div class="join">
            <button 
                class="join-item btn btn-sm" 
                disabled={currentPage === 1}
                on:click={previous}
            >
                «
            </button>
            <button class="join-item btn btn-sm">Page {currentPage}</button>
            <button 
                class="join-item btn btn-sm" 
                disabled={!hasMore}
                on:click={next}
            >
                »
            </button>
        </div>
    </div>
</section> 