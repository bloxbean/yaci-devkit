<script lang="ts">
    import { goto } from '$app/navigation';

    let address = '';
    let error = '';
    let loading = false;

    function isValidAddress(addr: string): boolean {
        return addr.startsWith('addr') || addr.startsWith('stake') || /^[0-9a-fA-F]+$/.test(addr);
    }

    async function handleSubmit() {
        if (!address.trim()) {
            error = 'Please enter an address';
            return;
        }

        loading = true;
        error = '';

        try {
            if (!isValidAddress(address.trim())) {
                error = 'Invalid address format. Address should start with "addr", "stake", or be a hex string.';
                return;
            }

            await goto(`/addresses/${address.trim()}`);
        } catch (e) {
            error = 'An error occurred while searching';
            console.error(e);
        } finally {
            loading = false;
        }
    }
</script>

<div class="container mx-auto px-4 py-8">
    <div class="max-w-2xl mx-auto">
        <h1 class="text-3xl font-bold mb-6">Address Explorer</h1>

        <div class="card bg-base-100 shadow-xl">
            <div class="card-body">
                <form on:submit|preventDefault={handleSubmit} class="space-y-4">
                    <div class="form-control">
                        <label class="label" for="address">
                            <span class="label-text">Address</span>
                        </label>
                        <input
                            type="text"
                            id="address"
                            bind:value={address}
                            placeholder="Enter address (addr..., stake..., or hex)"
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
                    <h3 class="font-bold">Address types</h3>
                    <div class="text-sm">
                        <p>Supported address formats:</p>
                        <ul class="list-disc list-inside mt-2">
                            <li>Payment addresses (addr1..., addr_test1...)</li>
                            <li>Stake addresses (stake1..., stake_test1...)</li>
                            <li>Hex-encoded addresses</li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
