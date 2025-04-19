<script lang="ts">
    import { formatAda, formatLovelace } from '$lib/util';
    import { onMount } from 'svelte';

    interface AdaPotData {
        epoch: number;
        totalAmount: number;
        details: {
            fees: number;
            treasury: number;
            reserves: number;
            circulation: number;
            distributedRewards: number;
            undistributedRewards: number;
            poolRewardsPot: number;
        };
    }

    let adapot: AdaPotData | null = null;
    let loading = true;
    let error: string | null = null;

    async function loadCurrentAdapot() {
        loading = true;
        error = null;
        try {
            const response = await fetch('http://localhost:8080/api/v1/adapot'); 
            if (!response.ok) {
                throw new Error('Failed to fetch current AdaPot');
            }
            const data = await response.json();
            adapot = {
                epoch: data.epoch,
                totalAmount: data.rewards_pot,
                details: {
                    fees: data.fees,
                    treasury: data.treasury,
                    reserves: data.reserves,
                    circulation: data.circulation,
                    distributedRewards: data.distributed_rewards,
                    undistributedRewards: data.undistributed_rewards,
                    poolRewardsPot: data.pool_rewards_pot
                }
            };
        } catch (e) {
            error = e instanceof Error ? e.message : 'An error occurred while loading AdaPot';
        } finally {
            loading = false;
        }
    }

    onMount(() => {
        loadCurrentAdapot();
    });
</script>

<div class="container mx-auto px-4 py-8">
    <div class="flex items-center gap-4 mb-8">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 text-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <h1 class="text-3xl font-bold">Current AdaPot Details</h1>
    </div>

    {#if error}
        <div class="alert alert-error mb-6">
            <svg xmlns="http://www.w3.org/2000/svg" class="stroke-current shrink-0 h-6 w-6" fill="none" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
            <span>{error}</span>
        </div>
    {/if}

    {#if loading}
        <div class="flex justify-center items-center py-8">
            <span class="loading loading-spinner loading-lg text-primary"></span>
        </div>
    {:else if adapot}
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            <!-- Epoch Card -->
            <div class="card bg-base-100 shadow-xl">
                <div class="card-body">
                    <div class="flex items-center gap-2 mb-2">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                        </svg>
                        <h3 class="text-lg font-semibold">Current Epoch</h3>
                    </div>
                    <p class="text-2xl font-bold text-primary">{adapot.epoch}</p>
                </div>
            </div>

            <!-- Treasury Card -->
            <div class="card bg-base-100 shadow-xl">
                <div class="card-body">
                    <div class="flex items-center gap-2 mb-2">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                        <h3 class="text-lg font-semibold">Treasury</h3>
                    </div>
                    <div class="tooltip" data-tip={formatLovelace(adapot.details.treasury)}>
                        <p class="text-2xl font-bold text-primary">
                            {formatAda(adapot.details.treasury)} ADA
                        </p>
                    </div>
                </div>
            </div>

            <!-- Reserves Card -->
            <div class="card bg-base-100 shadow-xl">
                <div class="card-body">
                    <div class="flex items-center gap-2 mb-2">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                        <h3 class="text-lg font-semibold">Reserves</h3>
                    </div>
                    <div class="tooltip" data-tip={formatLovelace(adapot.details.reserves)}>
                        <p class="text-2xl font-bold text-primary">
                            {formatAda(adapot.details.reserves)} ADA
                        </p>
                    </div>
                </div>
            </div>

            <!-- Circulation Card -->
            <div class="card bg-base-100 shadow-xl">
                <div class="card-body">
                    <div class="flex items-center gap-2 mb-2">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
                        </svg>
                        <h3 class="text-lg font-semibold">Circulation</h3>
                    </div>
                    <div class="tooltip" data-tip={formatLovelace(adapot.details.circulation)}>
                        <p class="text-2xl font-bold text-primary">
                            {formatAda(adapot.details.circulation)} ADA
                        </p>
                    </div>
                </div>
            </div>

            <!-- Fees Card -->
            <div class="card bg-base-100 shadow-xl">
                <div class="card-body">
                    <div class="flex items-center gap-2 mb-2">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                        <h3 class="text-lg font-semibold">Fees</h3>
                    </div>
                    <div class="tooltip" data-tip={formatLovelace(adapot.details.fees)}>
                        <p class="text-2xl font-bold text-primary">
                            {formatAda(adapot.details.fees)} ADA
                        </p>
                    </div>
                </div>
            </div>

            <!-- Total Rewards Pot Card -->
            <div class="card bg-base-100 shadow-xl">
                <div class="card-body">
                    <div class="flex items-center gap-2 mb-2">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                        <h3 class="text-lg font-semibold">Total Rewards Pot</h3>
                    </div>
                    <div class="tooltip" data-tip={formatLovelace(adapot.totalAmount)}>
                        <p class="text-2xl font-bold text-primary">
                            {formatAda(adapot.totalAmount)} ADA
                        </p>
                    </div>
                </div>
            </div>

            <!-- Distributed Rewards Card -->
            <div class="card bg-base-100 shadow-xl">
                <div class="card-body">
                    <div class="flex items-center gap-2 mb-2">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                        <h3 class="text-lg font-semibold">Distributed Rewards</h3>
                    </div>
                    <div class="tooltip" data-tip={formatLovelace(adapot.details.distributedRewards)}>
                        <p class="text-2xl font-bold text-primary">
                            {formatAda(adapot.details.distributedRewards)} ADA
                        </p>
                    </div>
                </div>
            </div>

            <!-- Undistributed Rewards Card -->
            <div class="card bg-base-100 shadow-xl">
                <div class="card-body">
                    <div class="flex items-center gap-2 mb-2">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                        <h3 class="text-lg font-semibold">Undistributed Rewards</h3>
                    </div>
                    <div class="tooltip" data-tip={formatLovelace(adapot.details.undistributedRewards)}>
                        <p class="text-2xl font-bold text-primary">
                            {formatAda(adapot.details.undistributedRewards)} ADA
                        </p>
                    </div>
                </div>
            </div>

            <!-- Pool Rewards Pot Card -->
            <div class="card bg-base-100 shadow-xl">
                <div class="card-body">
                    <div class="flex items-center gap-2 mb-2">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                        <h3 class="text-lg font-semibold">Pool Rewards Pot</h3>
                    </div>
                    <div class="tooltip" data-tip={formatLovelace(adapot.details.poolRewardsPot)}>
                        <p class="text-2xl font-bold text-primary">
                            {formatAda(adapot.details.poolRewardsPot)} ADA
                        </p>
                    </div>
                </div>
            </div>
        </div>
    {:else}
        <div class="text-center py-8 text-gray-500">No AdaPot data available</div>
    {/if}
</div> 