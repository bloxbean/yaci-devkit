<script lang="ts">
    import { goto } from '$app/navigation';
    import { classifyAssetId } from '$lib/utils/asset';

    let input = '';
    let error = '';
    let loading = false;

    async function handleSubmit() {
        const trimmed = input.trim();
        if (!trimmed) {
            error = 'Please enter an asset identifier';
            return;
        }

        loading = true;
        error = '';

        const type = classifyAssetId(trimmed);
        switch (type) {
            case 'unit':
                await goto(`/assets/unit/${trimmed}`);
                break;
            case 'policy':
                await goto(`/assets/policy/${trimmed}`);
                break;
            case 'fingerprint':
                await goto(`/assets/fingerprint/${trimmed}`);
                break;
            default:
                error = 'Invalid asset identifier. Please enter a valid unit (57+ hex chars), policy ID (56 hex chars), or fingerprint (asset1...).';
                loading = false;
                return;
        }
        loading = false;
    }
</script>

<div class="container mx-auto px-4 py-8">
    <div class="max-w-2xl mx-auto">
        <h1 class="text-3xl font-bold mb-6">Asset Explorer</h1>

        <div class="card bg-base-100 shadow-xl">
            <div class="card-body">
                <form on:submit|preventDefault={handleSubmit} class="space-y-4">
                    <div class="form-control">
                        <label class="label" for="asset-id">
                            <span class="label-text">Asset Identifier</span>
                        </label>
                        <input
                            type="text"
                            id="asset-id"
                            bind:value={input}
                            placeholder="Enter unit, policy ID, or fingerprint"
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
                    <h3 class="font-bold">Supported formats</h3>
                    <div class="text-sm">
                        <ul class="list-disc list-inside mt-2">
                            <li><strong>Unit</strong> — Policy ID + hex asset name (57+ hex chars)</li>
                            <li><strong>Policy ID</strong> — 56 hex characters</li>
                            <li><strong>Fingerprint</strong> — Bech32 asset fingerprint (asset1...)</li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
