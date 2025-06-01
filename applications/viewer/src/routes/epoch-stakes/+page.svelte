<script lang="ts">
    import { goto } from "$app/navigation";
    import { page } from "$app/stores";
    import { onMount } from 'svelte';
    import { formatAda } from "$lib/util";
    import { env } from '$env/dynamic/public';

    export let data;

    interface StakeInfo {
        address: string;
        epoch: number;
        amount: number;
        pool_hash: string;
        pool_id: string;
        delegation_epoch: number;
    }

    interface TotalStake {
        epoch: number;
        active_stake: number;
    }

    // State
    let loading = false;
    let error: string | null = null;
    let currentEpoch = data?.currentEpoch || 0;
    let searchEpochInput = currentEpoch.toString();
    let searchEpoch = currentEpoch;
    let totalStake: TotalStake | null = null;
    let stakes: StakeInfo[] = [];
    let currentPage = parseInt(data.page);
    const itemsPerPage = parseInt(data.count);

    // Fetch total stake for an epoch
    async function fetchTotalStake(epoch: number) {
        try {
            const url = `${env.PUBLIC_INDEXER_BASE_URL}/epochs/${epoch}/total-stake`;
            console.log('Fetching total stake:', url);
            const response = await fetch(url);
            console.log('Total stake response status:', response.status);
            if (!response.ok) throw new Error('Failed to fetch total stake');
            const data = await response.json();
            console.log('Total stake data:', data);
            totalStake = data;
            error = null;
        } catch (err) {
            console.error('Total stake error:', err);
            error = 'Failed to fetch total stake data';
        }
    }

    // Fetch stakes for an epoch with pagination
    async function fetchStakes(epoch: number, page: number) {
        loading = true;
        try {
            const url = `${env.PUBLIC_INDEXER_BASE_URL}/epochs/${epoch}/stake?count=${itemsPerPage}&page=${page}`;
            console.log('Fetching stakes:', url);
            const response = await fetch(url);
            console.log('Stakes response status:', response.status);
            if (!response.ok) throw new Error('Failed to fetch stakes');
            const data = await response.json();
            console.log('Stakes data:', data);
            stakes = data;
            error = null;
        } catch (err) {
            console.error('Stakes error:', err);
            error = 'Failed to fetch stake data';
            stakes = [];
        } finally {
            loading = false;
        }
    }

    // Handle epoch search
    async function handleSearch() {
        const epochNum = parseInt(searchEpochInput);
        if (isNaN(epochNum)) return;
        searchEpoch = epochNum;
        const url = `/epoch-stakes?epoch=${searchEpoch}&page=1&count=${itemsPerPage}`;
        goto(url);
    }

    // Handle enter key in search input
    function handleKeydown(event: KeyboardEvent) {
        if (event.key === 'Enter') {
            handleSearch();
        }
    }

    // Navigation functions
    const previous = () => {
        if (currentPage <= 1 || loading) return;
        const url = `/epoch-stakes?epoch=${searchEpoch}&page=${currentPage - 1}&count=${itemsPerPage}`;
        goto(url);
    };

    const next = () => {
        if (!hasMore || loading) return;
        const url = `/epoch-stakes?epoch=${searchEpoch}&page=${currentPage + 1}&count=${itemsPerPage}`;
        goto(url);
    };

    // Watch for URL changes and update data
    $: if ($page.url.searchParams) {
        const urlEpoch = $page.url.searchParams.get('epoch');
        const urlPage = $page.url.searchParams.get('page') || '1';
        
        if (urlEpoch) {
            searchEpoch = parseInt(urlEpoch);
            searchEpochInput = urlEpoch;
            currentPage = parseInt(urlPage);
            console.log('URL changed, fetching data for epoch:', searchEpoch, 'page:', currentPage);
            Promise.all([
                fetchTotalStake(searchEpoch),
                fetchStakes(searchEpoch, currentPage)
            ]);
        } else {
            searchEpoch = currentEpoch;
            searchEpochInput = currentEpoch.toString();
            currentPage = parseInt(urlPage);
        }
    }

    $: hasMore = stakes.length === itemsPerPage;

    // Log environment variable on mount for debugging
    onMount(() => {
        console.log('INDEXER_BASE_URL:', env.PUBLIC_INDEXER_BASE_URL);
    });
</script>

<div class="container mx-auto px-4 py-8">
    <h1 class="text-2xl font-bold text-gray-900 mb-6">Epoch Stakes</h1>

    <!-- Search Section -->
    <div class="bg-white rounded-lg shadow-md p-6 mb-6">
        <div class="flex items-end gap-2">
            <div class="w-48">
                <label for="epoch" class="block text-sm font-medium text-gray-700 mb-2">Epoch Number</label>
                <div class="flex gap-2">
                    <input
                        type="number"
                        id="epoch"
                        bind:value={searchEpochInput}
                        class="input input-bordered input-sm w-full h-8"
                        placeholder="Enter epoch number"
                        min="0"
                        on:keydown={handleKeydown}
                    />
                    <button
                        class="btn btn-primary btn-sm h-8 min-h-0"
                        on:click={handleSearch}
                        disabled={loading}
                    >
                        {loading ? 'Loading...' : 'Search'}
                    </button>
                </div>
            </div>
        </div>
    </div>

    {#if error}
        <div class="alert alert-error mb-6">
            <span>{error}</span>
        </div>
    {/if}

    <!-- Total Stake Display -->
    {#if totalStake !== null}
        <div class="bg-white rounded-lg shadow-md p-6 mb-6">
            <h2 class="text-lg font-semibold text-gray-800 mb-2">Total Active Stake</h2>
            <p class="text-3xl font-bold text-primary">
                {formatAda(totalStake.active_stake)} ₳
            </p>
        </div>
    {/if}

    <!-- Stakes Table -->
    <div class="bg-white rounded-lg shadow-md overflow-hidden">
        {#if loading}
            <div class="flex justify-center items-center h-32">
                <span class="loading loading-spinner loading-lg"></span>
            </div>
        {:else if stakes.length === 0}
            <div class="text-center py-8">
                <p class="text-gray-600">No stake data available for this epoch.</p>
            </div>
        {:else}
        <!-- Pagination -->
        <div class="flex justify-end mt-6">
            <div class="join">
                <button 
                    class="join-item btn btn-sm" 
                    disabled={currentPage <= 1 || loading}
                    on:click={previous}
                >
                    «
                </button>
                <button class="join-item btn btn-sm">Page {currentPage}</button>
                <button 
                    class="join-item btn btn-sm" 
                    disabled={!hasMore || loading}
                    on:click={next}
                >
                    »
                </button>
            </div>
        </div>
            <div class="overflow-x-auto">
                <table class="table w-full">
                    <thead>
                        <tr>
                            <th class="text-left">Stake Address</th>
                            <th class="text-right">Amount (₳)</th>
                            <th class="text-left">Pool ID</th>
                            <th class="text-center">Delegation Epoch</th>
                        </tr>
                    </thead>
                    <tbody>
                        {#each stakes as stake}
                            <tr class="hover:bg-gray-50">
                                <td class="text-sm">
                                    <a 
                                        href="/epoch-stakes/accounts?address={stake.address}&epoch={searchEpoch}" 
                                        class="text-primary hover:underline"
                                    >
                                        {stake.address}
                                    </a>
                                </td>
                                <td class="text-right">{formatAda(stake.amount)}</td>
                                <td>
                                    <a 
                                        href="/epoch-stakes/pools?id={stake.pool_id}&epoch={searchEpoch}" 
                                        class="text-primary hover:underline"
                                    >
                                        {stake.pool_id}
                                    </a>
                                </td>
                                <td class="text-center">{stake.delegation_epoch}</td>
                            </tr>
                        {/each}
                    </tbody>
                </table>
            </div>

            
        {/if}
    </div>
</div> 