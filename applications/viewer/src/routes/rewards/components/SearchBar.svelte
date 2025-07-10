<script lang="ts">
    import { createEventDispatcher } from 'svelte';
    
    const dispatch = createEventDispatcher<{
        search: { stakeAddress: string; epoch: number | null };
    }>();
    
    let stakeAddress = '';
    let epoch: number | null = null;
    
    function handleSearch() {
        dispatch('search', { stakeAddress, epoch });
    }
    
    function handleReset() {
        stakeAddress = '';
        epoch = null;
        dispatch('search', { stakeAddress: '', epoch: null });
    }
</script>

<div class="bg-white rounded-lg shadow p-6 mb-6">
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <!-- Stake Address Search -->
        <div>
            <label for="stakeAddress" class="block text-sm font-medium text-gray-700 mb-1">
                Stake Address
            </label>
            <input
                type="text"
                id="stakeAddress"
                bind:value={stakeAddress}
                placeholder="Enter stake address"
                class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
            />
        </div>
        
        <!-- Epoch Filter -->
        <div>
            <label for="epoch" class="block text-sm font-medium text-gray-700 mb-1">
                Epoch
            </label>
            <input
                type="number"
                id="epoch"
                bind:value={epoch}
                placeholder="Enter epoch number"
                class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
            />
        </div>
    </div>
    
    <div class="mt-4 flex justify-end space-x-3">
        <button
            type="button"
            on:click={handleReset}
            class="px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
        >
            Reset
        </button>
        <button
            type="button"
            on:click={handleSearch}
            class="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
        >
            Search
        </button>
    </div>
</div> 