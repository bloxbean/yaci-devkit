<script lang="ts">
    import {goto} from '$app/navigation';
    import {onMount} from 'svelte';

    let poolId = '';
    let error = '';
    let loading = false;

    async function handleSubmit() {
        if (!poolId.trim()) {
            error = 'Please enter a pool ID';
            return;
        }

        loading = true;
        error = '';

        try {
            // Validate pool ID format (bech32)
            if (!poolId.startsWith('pool1')) {
                error = 'Invalid pool ID format. Pool ID should start with "pool1"';
                return;
            }

            // Redirect to pool details page
            await goto(`/pools/${poolId}`);
        } catch (e) {
            error = 'An error occurred while searching for the pool';
            console.error(e);
        } finally {
            loading = false;
        }
    }
</script>

<div class="container mx-auto px-4 py-8">
    <div class="max-w-2xl mx-auto">
        <h1 class="text-3xl font-bold mb-6">Pool Search</h1>
        
        <div class="card bg-base-100 shadow-xl">
            <div class="card-body">
                <form on:submit|preventDefault={handleSubmit} class="space-y-4">
                    <div class="form-control">
                        <label class="label" for="poolId">
                            <span class="label-text">Pool ID</span>
                        </label>
                        <input
                            type="text"
                            id="poolId"
                            bind:value={poolId}
                            placeholder="Enter pool ID (e.g., pool1...)"
                            class="input input-bordered w-full"
                            required
                        />
                    </div>

                    {#if error}
                        <div class="alert alert-error">
                            <svg xmlns="http://www.w3.org/2000/svg" class="stroke-current shrink-0 h-6 w-6" fill="none" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
                            <span>{error}</span>
                        </div>
                    {/if}

                    <div class="card-actions justify-end">
                        <button type="submit" class="btn btn-primary" disabled={loading}>
                            {#if loading}
                                <span class="loading loading-spinner"></span>
                                Searching...
                            {:else}
                                Search
                            {/if}
                        </button>
                    </div>
                </form>
            </div>
        </div>

        <div class="mt-8">
            <div class="alert alert-info">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" class="stroke-current shrink-0 w-6 h-6"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                <div>
                    <h3 class="font-bold">How to find a pool ID</h3>
                    <div class="text-sm">
                        <p>Pool IDs are unique identifiers for Cardano stake pools. They start with "pool1" followed by a series of characters.</p>
                        <p>You can find pool IDs on:</p>
                        <ul class="list-disc list-inside mt-2">
                            <li>Pool registration certificates</li>
                            <li>Pool explorer websites</li>
                            <li>Your wallet's delegation interface</li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div> 