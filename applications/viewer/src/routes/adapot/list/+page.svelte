<script lang="ts">
    import { formatAda, formatLovelace } from '$lib/util';
    import { page } from '$app/stores';
    import { onMount } from 'svelte';
    import { goto } from '$app/navigation';
    import { env } from '$env/dynamic/public';

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

    interface ApiResponse {
        epoch: number;
        rewards_pot: number;
        deposits_stake: number;
        fees: number;
        treasury: number;
        reserves: number;
        circulation: number;
        distributed_rewards: number;
        undistributed_rewards: number;
        pool_rewards_pot: number;
    }

    let adapots: AdaPotData[] = [];
    let currentPage = 1;
    let itemsPerPage = 20;
    let totalPages = 1;
    let loading = true;
    let error: string | null = null;
    let initialLoad = true;

    // Initialize from URL parameters
    $: {
        const params = new URLSearchParams($page.url.search);
        const pageParam = parseInt(params.get('page') || '1');
        const countParam = parseInt(params.get('count') || '15');
        
        // Only update and reload if the parameters have actually changed
        if (pageParam !== currentPage || countParam !== itemsPerPage) {
            currentPage = pageParam;
            itemsPerPage = countParam;
            if (!initialLoad) {
                // Use a small timeout to prevent multiple rapid reloads
                setTimeout(() => loadAdapots(currentPage), 0);
            }
        }
    }

    onMount(() => {
        // Load initial data
        loadAdapots(currentPage);
        initialLoad = false;
    });

    function validateApiResponse(data: any[]): data is ApiResponse[] {
        if (!Array.isArray(data)) return false;
        return data.every(item => 
            typeof item === 'object' &&
            typeof item.epoch === 'number' &&
            typeof item.rewards_pot === 'number' &&
            typeof item.deposits_stake === 'number' &&
            typeof item.fees === 'number' &&
            typeof item.treasury === 'number' &&
            typeof item.reserves === 'number' &&
            typeof item.circulation === 'number' &&
            typeof item.distributed_rewards === 'number' &&
            typeof item.undistributed_rewards === 'number' &&
            typeof item.pool_rewards_pot === 'number'
        );
    }

    async function loadAdapots(page: number) {
        loading = true;
        error = null;
        try {
            console.log('Loading page:', page);
            const baseUrl = env.PUBLIC_INDEXER_BASE_URL;
            const response = await fetch(`${baseUrl}/adapot/list?page=${page}&count=${itemsPerPage}`);
            console.log('Response status:', response.status);
            
            if (!response.ok) {
                const errorData = await response.json().catch(() => ({ error: 'Unknown error occurred' }));
                console.error('Error response:', errorData);
                throw new Error(errorData.error || `Failed to fetch AdaPot list (Status: ${response.status})`);
            }
            
            const data = await response.json();
            console.log('Received data:', data);

            if (!validateApiResponse(data)) {
                throw new Error('Invalid data format received from server');
            }

            adapots = data.map((item: ApiResponse) => ({
                epoch: item.epoch,
                totalAmount: item.rewards_pot,
                currentEpoch: item.epoch,
                details: {
                    depositsStake: item.deposits_stake,
                    fees: item.fees,
                    treasury: item.treasury,
                    reserves: item.reserves,
                    circulation: item.circulation,
                    distributedRewards: item.distributed_rewards,
                    undistributedRewards: item.undistributed_rewards,
                    poolRewardsPot: item.pool_rewards_pot
                }
            }));
            currentPage = page;
        } catch (e) {
            console.error('Error loading AdaPots:', e);
            error = e instanceof Error ? e.message : 'An error occurred while loading AdaPots';
            adapots = [];
        } finally {
            loading = false;
        }
    }

    function goToPage(page: number) {
        if (page < 1 || (page > currentPage && adapots.length < itemsPerPage)) return;
        const params = new URLSearchParams();
        params.set('page', page.toString());
        params.set('count', itemsPerPage.toString());
        goto(`?${params.toString()}`);
    }
</script>

<div class="container mx-auto px-4 py-8">
    <h1 class="text-2xl font-bold mb-6">AdaPot History</h1>

    {#if loading}
        <div class="flex justify-center items-center h-32">
            <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-gray-900"></div>
        </div>
    {:else if error}
        <div class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded relative mb-4" role="alert">
            <strong class="font-bold">Error: </strong>
            <span class="block sm:inline">{error}</span>
            <button class="bg-red-500 text-white px-4 py-2 rounded mt-2" on:click={() => loadAdapots(currentPage)}>
                Retry
            </button>
        </div>
    {:else if adapots.length === 0}
        <div class="text-center py-8">
            <p class="text-gray-600">No AdaPot data available for this page.</p>
        </div>
    {:else}

        <div class="overflow-x-auto">
            <table class="min-w-full bg-white border border-gray-200">
                <thead class="bg-gray-50">
                    <tr>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Epoch</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Treasury</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Reserves</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Circulation</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Fees</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Total Rewards</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Distributed</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Undistributed</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Pool Rewards</th>
                    </tr>
                </thead>
                <tbody class="bg-white divide-y divide-gray-200">
                    {#each adapots as adapot}
                        <tr class="hover:bg-gray-50">
                            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                                {adapot.epoch}
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                <div class="tooltip" data-tip={formatLovelace(adapot.details.treasury)}>
                                    {formatAda(adapot.details.treasury)}
                                </div>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                <div class="tooltip" data-tip={formatLovelace(adapot.details.reserves)}>
                                    {formatAda(adapot.details.reserves)}
                                </div>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                <div class="tooltip" data-tip={formatLovelace(adapot.details.circulation)}>
                                    {formatAda(adapot.details.circulation)}
                                </div>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                <div class="tooltip" data-tip={formatLovelace(adapot.details.fees)}>
                                    {formatAda(adapot.details.fees)}
                                </div>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                <div class="tooltip" data-tip={formatLovelace(adapot.totalAmount)}>
                                    {formatAda(adapot.totalAmount)}
                                </div>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                <div class="tooltip" data-tip={formatLovelace(adapot.details.distributedRewards)}>
                                    {formatAda(adapot.details.distributedRewards)}
                                </div>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                <div class="tooltip" data-tip={formatLovelace(adapot.details.undistributedRewards)}>
                                    {formatAda(adapot.details.undistributedRewards)}
                                </div>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
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
                disabled={currentPage <= 1 || loading}
            >
                «
            </button>
            <span class="join-item btn btn-sm">Page {currentPage}</span>
            <button 
                class="join-item btn btn-sm"
                on:click={() => goToPage(currentPage + 1)}
                disabled={adapots.length < itemsPerPage || loading} 
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