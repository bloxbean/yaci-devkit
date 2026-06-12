<script lang="ts">
    import { formatAda, formatLovelace } from '$lib/util';
    import { goto } from '$app/navigation';

    interface AdaPotData {
        epoch: number;
        totalAmount: number;
        currentEpoch: number;
        details: {
            depositsStake: number;
            fees: number;
            treasury: number;
            reserves: number;
            circulation: number;
            distributedRewards: number;
            undistributedRewards: number;
            poolRewardsPot: number;
        };
    }

    export let data;

    let adapots: AdaPotData[] = [];
    let currentPage = 1;
    let itemsPerPage = 15;
    let error: string | null = null;

    $: adapots = data.adapots ?? [];
    $: currentPage = parseInt(data.page || '1');
    $: itemsPerPage = parseInt(data.count || '15');
    $: error = data.error ?? null;

    function goToPage(page: number) {
        if (page < 1 || (page > currentPage && adapots.length < itemsPerPage)) return;
        const params = new URLSearchParams();
        params.set('page', page.toString());
        params.set('count', itemsPerPage.toString());
        goto(`/adapot/list?${params.toString()}`);
    }
</script>

<div class="container mx-auto px-4 py-8">
    <h1 class="text-2xl font-bold mb-6">AdaPot History</h1>

    {#if error}
        <div class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded relative mb-4" role="alert">
            <strong class="font-bold">Error: </strong>
            <span class="block sm:inline">{error}</span>
        </div>
    {:else if adapots.length === 0}
        <div class="text-center py-8">
            <p class="text-base-content/70">No AdaPot data available for this page.</p>
        </div>
    {:else}

        <div class="overflow-x-auto">
            <table class="min-w-full bg-base-100 border border-base-300">
                <thead class="bg-base-200">
                    <tr>
                        <th class="px-6 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Epoch</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Treasury</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Reserves</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Circulation</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Fees</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Total Rewards</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Distributed</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Undistributed</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-base-content/60 uppercase tracking-wider">Pool Rewards</th>
                    </tr>
                </thead>
                <tbody class="bg-base-100 divide-y divide-base-300">
                    {#each adapots as adapot}
                        <tr class="hover:bg-base-200">
                            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-base-content">
                                {adapot.epoch}
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-base-content/60">
                                <div class="tooltip" data-tip={formatLovelace(adapot.details.treasury)}>
                                    {formatAda(adapot.details.treasury)}
                                </div>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-base-content/60">
                                <div class="tooltip" data-tip={formatLovelace(adapot.details.reserves)}>
                                    {formatAda(adapot.details.reserves)}
                                </div>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-base-content/60">
                                <div class="tooltip" data-tip={formatLovelace(adapot.details.circulation)}>
                                    {formatAda(adapot.details.circulation)}
                                </div>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-base-content/60">
                                <div class="tooltip" data-tip={formatLovelace(adapot.details.fees)}>
                                    {formatAda(adapot.details.fees)}
                                </div>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-base-content/60">
                                <div class="tooltip" data-tip={formatLovelace(adapot.totalAmount)}>
                                    {formatAda(adapot.totalAmount)}
                                </div>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-base-content/60">
                                <div class="tooltip" data-tip={formatLovelace(adapot.details.distributedRewards)}>
                                    {formatAda(adapot.details.distributedRewards)}
                                </div>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-base-content/60">
                                <div class="tooltip" data-tip={formatLovelace(adapot.details.undistributedRewards)}>
                                    {formatAda(adapot.details.undistributedRewards)}
                                </div>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-base-content/60">
                                <div class="tooltip" data-tip={formatLovelace(adapot.details.poolRewardsPot)}>
                                    {formatAda(adapot.details.poolRewardsPot)}
                                </div>
                            </td>
                        </tr>
                    {/each}
                </tbody>
            </table>
        </div>

        <!-- Pagination Controls (Bottom) -->
        <div class="flex justify-center mt-6">
            <div class="join">
            <button 
                class="join-item btn btn-sm"
                on:click={() => goToPage(currentPage - 1)}
                disabled={currentPage <= 1}
            >
                «
            </button>
            <span class="join-item btn btn-sm">Page {currentPage}</span>
            <button 
                class="join-item btn btn-sm"
                on:click={() => goToPage(currentPage + 1)}
                disabled={adapots.length < itemsPerPage} 
            >
                »
            </button>
            </div>
        </div>
    
    {/if}
</div>

<style>
    .container {
        max-width: 1400px;
    }
    
    th {
        position: sticky;
        top: 0;
        z-index: 10;
        background-color: #f9fafb;
    }
</style> 