<script lang="ts">
    import { page } from '$app/stores';
    import { formatAda, formatLovelace } from '$lib/util';
    import moment from 'moment';

    interface AdaPotData {
        epoch: number;
        totalAmount: number;
        details: {
            treasury: number;
            reserves: number;
            circulation: number;
            fees: number;
            distributedRewards: number;
            undistributedRewards: number;
            poolRewardsPot: number;
        };
    }

    let loading = false;
    let error: string | null = null;
    let searchEpoch = '';
    let searchResult: AdaPotData | null = null;
    let searchError: string | null = null;

    async function searchByEpoch() {
        if (!searchEpoch) return;

        loading = true;
        searchError = null;
        searchResult = null;

        try {
            const response = await fetch(`/api/adapot/${searchEpoch}`);
            if (!response.ok) {
                throw new Error('Failed to fetch AdaPot data');
            }
            const data = await response.json();
            searchResult = data;
        } catch (err) {
            searchError = err instanceof Error ? err.message : 'Failed to fetch AdaPot data';
        } finally {
            loading = false;
        }
    }
</script>

<div class="container mx-auto px-4 py-8">
    {#if $page.data.error}
        <div class="alert alert-error mb-4">
            <svg xmlns="http://www.w3.org/2000/svg" class="stroke-current shrink-0 h-6 w-6" fill="none" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>{$page.data.error}</span>
        </div>
    {:else}
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-8">
            <!-- Current Epoch -->
            <div class="card bg-base-100 shadow-xl">
                <div class="card-body">
                    <h2 class="card-title">Current Epoch</h2>
                    <p class="text-2xl font-bold">{$page.data.epoch}</p>
                </div>
            </div>

            <!-- Treasury -->
            <div class="card bg-base-100 shadow-xl">
                <div class="card-body">
                    <h2 class="card-title">Treasury</h2>
                    <p class="text-2xl font-bold" title={formatLovelace($page.data.details.treasury)}>
                        {formatAda($page.data.details.treasury)}
                    </p>
                </div>
            </div>

            <!-- Reserves -->
            <div class="card bg-base-100 shadow-xl">
                <div class="card-body">
                    <h2 class="card-title">Reserves</h2>
                    <p class="text-2xl font-bold" title={formatLovelace($page.data.details.reserves)}>
                        {formatAda($page.data.details.reserves)}
                    </p>
                </div>
            </div>

            <!-- Circulation -->
            <div class="card bg-base-100 shadow-xl">
                <div class="card-body">
                    <h2 class="card-title">Circulation</h2>
                    <p class="text-2xl font-bold" title={formatLovelace($page.data.details.circulation)}>
                        {formatAda($page.data.details.circulation)}
                    </p>
                </div>
            </div>

            <!-- Fees -->
            <div class="card bg-base-100 shadow-xl">
                <div class="card-body">
                    <h2 class="card-title">Fees</h2>
                    <p class="text-2xl font-bold" title={formatLovelace($page.data.details.fees)}>
                        {formatAda($page.data.details.fees)}
                    </p>
                </div>
            </div>

            <!-- Distributed Rewards -->
            <div class="card bg-base-100 shadow-xl">
                <div class="card-body">
                    <h2 class="card-title">Distributed Rewards</h2>
                    <p class="text-2xl font-bold" title={formatLovelace($page.data.details.distributedRewards)}>
                        {formatAda($page.data.details.distributedRewards)}
                    </p>
                </div>
            </div>

            <!-- Undistributed Rewards -->
            <div class="card bg-base-100 shadow-xl">
                <div class="card-body">
                    <h2 class="card-title">Undistributed Rewards</h2>
                    <p class="text-2xl font-bold" title={formatLovelace($page.data.details.undistributedRewards)}>
                        {formatAda($page.data.details.undistributedRewards)}
                    </p>
                </div>
            </div>

            <!-- Pool Rewards Pot -->
            <div class="card bg-base-100 shadow-xl">
                <div class="card-body">
                    <h2 class="card-title">Pool Rewards Pot</h2>
                    <p class="text-2xl font-bold" title={formatLovelace($page.data.details.poolRewardsPot)}>
                        {formatAda($page.data.details.poolRewardsPot)}
                    </p>
                </div>
            </div>
        </div>

        <!-- Search Section -->
        <div class="card bg-base-100 shadow-xl">
            <div class="card-body">
                <h2 class="card-title">Search by Epoch</h2>
                <div class="flex gap-2">
                    <input
                        type="number"
                        placeholder="Enter epoch number"
                        class="input input-bordered w-32"
                        bind:value={searchEpoch}
                        min="0"
                        max="999"
                    />
                    <button
                        class="btn btn-primary"
                        on:click={searchByEpoch}
                        disabled={loading || !searchEpoch}
                    >
                        {#if loading}
                            <span class="loading loading-spinner"></span>
                        {/if}
                        Search
                    </button>
                </div>

                {#if searchError}
                    <div class="alert alert-error mt-4">
                        <svg xmlns="http://www.w3.org/2000/svg" class="stroke-current shrink-0 h-6 w-6" fill="none" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                        <span>{searchError}</span>
                    </div>
                {/if}

                {#if searchResult}
                    <div class="mt-4">
                        <h3 class="text-lg font-semibold mb-2">Search Results for Epoch {searchResult.epoch}</h3>
                        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                            <!-- Treasury -->
                            <div class="card bg-base-200 shadow-xl">
                                <div class="card-body">
                                    <h2 class="card-title">Treasury</h2>
                                    <p class="text-2xl font-bold" title={formatLovelace(searchResult.details.treasury)}>
                                        {formatAda(searchResult.details.treasury)}
                                    </p>
                                </div>
                            </div>

                            <!-- Reserves -->
                            <div class="card bg-base-200 shadow-xl">
                                <div class="card-body">
                                    <h2 class="card-title">Reserves</h2>
                                    <p class="text-2xl font-bold" title={formatLovelace(searchResult.details.reserves)}>
                                        {formatAda(searchResult.details.reserves)}
                                    </p>
                                </div>
                            </div>

                            <!-- Circulation -->
                            <div class="card bg-base-200 shadow-xl">
                                <div class="card-body">
                                    <h2 class="card-title">Circulation</h2>
                                    <p class="text-2xl font-bold" title={formatLovelace(searchResult.details.circulation)}>
                                        {formatAda(searchResult.details.circulation)}
                                    </p>
                                </div>
                            </div>

                            <!-- Fees -->
                            <div class="card bg-base-200 shadow-xl">
                                <div class="card-body">
                                    <h2 class="card-title">Fees</h2>
                                    <p class="text-2xl font-bold" title={formatLovelace(searchResult.details.fees)}>
                                        {formatAda(searchResult.details.fees)}
                                    </p>
                                </div>
                            </div>

                            <!-- Distributed Rewards -->
                            <div class="card bg-base-200 shadow-xl">
                                <div class="card-body">
                                    <h2 class="card-title">Distributed Rewards</h2>
                                    <p class="text-2xl font-bold" title={formatLovelace(searchResult.details.distributedRewards)}>
                                        {formatAda(searchResult.details.distributedRewards)}
                                    </p>
                                </div>
                            </div>

                            <!-- Undistributed Rewards -->
                            <div class="card bg-base-200 shadow-xl">
                                <div class="card-body">
                                    <h2 class="card-title">Undistributed Rewards</h2>
                                    <p class="text-2xl font-bold" title={formatLovelace(searchResult.details.undistributedRewards)}>
                                        {formatAda(searchResult.details.undistributedRewards)}
                                    </p>
                                </div>
                            </div>

                            <!-- Pool Rewards Pot -->
                            <div class="card bg-base-200 shadow-xl">
                                <div class="card-body">
                                    <h2 class="card-title">Pool Rewards Pot</h2>
                                    <p class="text-2xl font-bold" title={formatLovelace(searchResult.details.poolRewardsPot)}>
                                        {formatAda(searchResult.details.poolRewardsPot)}
                                    </p>
                                </div>
                            </div>
                        </div>
                    </div>
                {/if}
            </div>
        </div>
    {/if}
</div> 