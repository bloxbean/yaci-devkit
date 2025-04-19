<script lang="ts">
    import { createEventDispatcher } from 'svelte';
    
    const dispatch = createEventDispatcher<{
        search: { startEpoch: number | null; endEpoch: number | null };
    }>();
    
    let startEpoch: number | null = null;
    let endEpoch: number | null = null;
    
    function handleSearch() {
        dispatch('search', { startEpoch, endEpoch });
    }
    
    function handleReset() {
        startEpoch = null;
        endEpoch = null;
        dispatch('search', { startEpoch: null, endEpoch: null });
    }
</script>

<div class="bg-white rounded-lg shadow p-6 mb-6">
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <!-- Start Epoch Filter -->
        <div>
            <label for="startEpoch" class="block text-sm font-medium text-gray-700 mb-1">
                Start Epoch
            </label>
            <input
                type="number"
                id="startEpoch"
                bind:value={startEpoch}
                placeholder="Enter start epoch"
                class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
            />
        </div>
        
        <!-- End Epoch Filter -->
        <div>
            <label for="endEpoch" class="block text-sm font-medium text-gray-700 mb-1">
                End Epoch
            </label>
            <input
                type="number"
                id="endEpoch"
                bind:value={endEpoch}
                placeholder="Enter end epoch"
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