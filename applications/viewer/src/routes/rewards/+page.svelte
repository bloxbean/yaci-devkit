<script lang="ts">
    import { goto } from '$app/navigation';
    
    let searchEpoch = '';
    let searchAccount = '';
    let searchPool = '';
    let searchPoolEpoch = '';

    // Functions to navigate based on form input
    function navigateToEpochRewards() {
        if (searchEpoch) {
            goto(`/rewards/epoch/${searchEpoch}`);
        }
    }

    function navigateToAccountRewards() {
        if (searchAccount) {
            goto(`/rewards/account/${searchAccount}`);
        }
    }

    function navigateToPoolEpochRewards() {
        if (searchPool && searchPoolEpoch) {
            goto(`/rewards/pool/${searchPool}/epoch/${searchPoolEpoch}`);
        }
    }
</script>

<div class="container mx-auto px-4 py-8">
    <h1 class="text-3xl font-bold mb-8 text-center">Cardano Rewards Explorer</h1>

    <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
        <!-- Search by Epoch -->
        <div class="card bg-base-100 shadow-xl">
            <div class="card-body">
                <h2 class="card-title">Search by Epoch</h2>
                <p>View all rewards distributed within a specific epoch.</p>
                <div class="form-control mt-4">
                    <label class="label" for="epoch-input">
                        <span class="label-text">Epoch Number</span>
                    </label>
                    <input
                        id="epoch-input"
                        type="number"
                        placeholder="Enter epoch number"
                        class="input input-bordered w-full"
                        bind:value={searchEpoch}
                        min="0"
                        on:keydown={(e) => e.key === 'Enter' && navigateToEpochRewards()}
                    />
                </div>
                <div class="card-actions justify-end mt-4">
                    <button 
                       class="btn btn-primary"
                       on:click={navigateToEpochRewards}
                       disabled={!searchEpoch}
                    >
                        View Epoch Rewards
                    </button>
                </div>
            </div>
        </div>

        <!-- Search by Account -->
        <div class="card bg-base-100 shadow-xl">
            <div class="card-body">
                <h2 class="card-title">Search by Account</h2>
                <p>View the rewards history for a specific stake address.</p>
                <div class="form-control mt-4">
                    <label class="label" for="account-input">
                        <span class="label-text">Stake Address</span>
                    </label>
                    <input
                        id="account-input"
                        type="text"
                        placeholder="Enter stake address (stake1...)"
                        class="input input-bordered w-full"
                        bind:value={searchAccount}
                        on:keydown={(e) => e.key === 'Enter' && navigateToAccountRewards()}
                    />
                </div>
                <div class="card-actions justify-end mt-4">
                    <button 
                       class="btn btn-primary"
                       on:click={navigateToAccountRewards}
                       disabled={!searchAccount}
                    >
                        View Account Rewards
                    </button>
                </div>
            </div>
        </div>

        <!-- Search by Pool & Epoch -->
        <div class="card bg-base-100 shadow-xl">
            <div class="card-body">
                <h2 class="card-title">Search by Pool & Epoch</h2>
                <p>View rewards distributed by a specific pool in an epoch.</p>
                <div class="form-control mt-4">
                    <label class="label" for="pool-input">
                        <span class="label-text">Pool ID (bech32)</span>
                    </label>
                    <input
                        id="pool-input"
                        type="text"
                        placeholder="Enter pool ID (pool1...)"
                        class="input input-bordered w-full"
                        bind:value={searchPool}
                    />
                </div>
                 <div class="form-control mt-4">
                    <label class="label" for="pool-epoch-input">
                        <span class="label-text">Epoch Number</span>
                    </label>
                    <input
                        id="pool-epoch-input"
                        type="number"
                        placeholder="Enter epoch number"
                        class="input input-bordered w-full"
                        bind:value={searchPoolEpoch}
                        min="0"
                        on:keydown={(e) => e.key === 'Enter' && navigateToPoolEpochRewards()}
                    />
                </div>
                <div class="card-actions justify-end mt-4">
                    <button 
                       class="btn btn-primary"
                       on:click={navigateToPoolEpochRewards}
                       disabled={!searchPool || !searchPoolEpoch}
                    >
                        View Pool Rewards
                    </button>
                </div>
            </div>
        </div>
    </div>

    <!-- REMOVED SUMMARY CARDS AND RECENT REWARDS TABLE -->

</div> 