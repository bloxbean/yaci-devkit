<script lang="ts">
    import {onDestroy, onMount} from 'svelte';
    import { formatAda, formatLovelace } from '$lib/util';
    import { env } from '$env/dynamic/public';

    interface NetworkInfo {
        epoch: number;
        number: number;
        block_time: number;
        slot: number;
        epoch_slot: number;
        slots_per_epoch: number;
        epoch_progress?: string;
        total_active_stake?: string;
        treasury?: string;
        reserves?: string;
        circulating_supply?: string;
    }

    let network_info: NetworkInfo = {
        epoch: 0,
        number: 0,
        block_time: 0,
        slot: 0,
        epoch_slot: 0,
        slots_per_epoch: 0
    };
    let loading = true;
    let error: string | null = null;
    let lastFetchedEpoch: number = 0;
    let store: any;

    async function fetchEpochData() {
        try {
            console.log('Fetching network data');
            const baseUrl = env.PUBLIC_INDEXER_BASE_URL;
            const response = await fetch(`${baseUrl}/network`);
            if (!response.ok) {
                console.error('Failed to fetch network data:', response.status, response.statusText);
                throw new Error(`Failed to fetch network data: ${response.status} ${response.statusText}`);
            }
            const data = await response.json();
            console.log('Network data response:', data);
            return data;
        } catch (e) {
            console.error('Error fetching network data:', e);
            return null;
        }
    }

    async function handleStoreMessage(currentMessage: any) {
        if (currentMessage && typeof currentMessage === 'object') {
            const newEpoch = currentMessage.epoch || 0;
            console.log('New epoch received:', newEpoch);
            network_info.epoch = newEpoch;
            network_info.number = currentMessage.number || 0;
            network_info.block_time = currentMessage.block_time || 0;
            network_info.slot = currentMessage.slot || 0;
            network_info.epoch_slot = currentMessage.epoch_slot || 0;
            network_info.slots_per_epoch = currentMessage.slots_per_epoch || 0;

            if (network_info.epoch_slot && network_info.slots_per_epoch) {
                network_info.epoch_progress = ((network_info.epoch_slot / network_info.slots_per_epoch) * 100).toFixed(2);
            }

            // Only fetch data if epoch has changed
            if (newEpoch !== lastFetchedEpoch) {
                console.log('Epoch changed, fetching new data. Last epoch:', lastFetchedEpoch, 'New epoch:', newEpoch);
                const networkData = await fetchEpochData();

                if (networkData) {
                    network_info.total_active_stake = networkData.stake.active.toString();
                    network_info.treasury = networkData.supply.treasury.toString();
                    network_info.reserves = networkData.supply.reserves.toString();
                    network_info.circulating_supply = networkData.supply.circulating.toString();
                }
                lastFetchedEpoch = newEpoch;
            }

            loading = false;
        }
    }

    onMount(async () => {
        const importedStore = await import('./blocks/store.js');
        store = importedStore;
        store.blocksStore.subscribe(handleStoreMessage);
    });

    onDestroy(() => {
        if (store) {
            store.blocksStore.subscribe(() => {})();
        }
    });
</script>

<section class="bg-white py-12">
    <div class="container mx-auto grid grid-cols-1 md:grid-cols-4 gap-4">
        <div class="bg-gray-100 rounded-lg p-4">
            <h3 class="text-xl font-semibold mb-2">Epoch</h3>
            <p class="text-gray-700">{network_info.epoch}</p>
        </div>
        <div class="bg-gray-100 rounded-lg p-4">
            <h3 class="text-xl font-semibold mb-2">Current Block</h3>
            <p class="text-gray-700">{network_info.number}</p>
        </div>
        <div class="bg-gray-100 rounded-lg p-4">
            <h3 class="text-xl font-semibold mb-2">Slot</h3>
            {#if network_info.epoch_slot}
                <p class="text-gray-700">{network_info.epoch_slot}/{network_info.slots_per_epoch}</p>
            {/if}
        </div>
        <div class="bg-gray-100 rounded-lg p-4">
            <h3 class="text-xl font-semibold mb-2">Epoch Progress</h3>
            {#if network_info.epoch_progress}
                <div class="flex items-center">
                    <div class="w-3/4 bg-gray-300 rounded-lg overflow-hidden">
                        <div class="bg-blue-500 h-2" style="width: {network_info.epoch_progress}%;"></div>
                    </div>
                    <p class="ml-2 text-gray-700">{network_info.epoch_progress}%</p>
                </div>
            {/if}
        </div>
        <div class="bg-gray-100 rounded-lg p-4">
            <h3 class="text-xl font-semibold mb-2">Total Active Stake</h3>
            {#if network_info.total_active_stake}
                <div class="tooltip" data-tip={formatLovelace(parseInt(network_info.total_active_stake))}>
                    <p class="text-gray-700">{formatAda(parseInt(network_info.total_active_stake))} ADA</p>
                </div>
            {:else}
                 <span>-</span>
            {/if}
        </div>
        <div class="bg-gray-100 rounded-lg p-4">
            <h3 class="text-xl font-semibold mb-2">Treasury</h3>
            {#if network_info.treasury}
                <div class="tooltip" data-tip={formatLovelace(parseInt(network_info.treasury))}>
                    <p class="text-gray-700">{formatAda(parseInt(network_info.treasury))} ADA</p>
                </div>
            {:else}
                 <span>-</span>
            {/if}
        </div>
        <div class="bg-gray-100 rounded-lg p-4">
            <h3 class="text-xl font-semibold mb-2">Reserves</h3>
            {#if network_info.reserves}
                <div class="tooltip" data-tip={formatLovelace(parseInt(network_info.reserves))}>
                    <p class="text-gray-700">{formatAda(parseInt(network_info.reserves))} ADA</p>
                </div>
            {:else}
                 <span>-</span>
            {/if}
        </div>
        <div class="bg-gray-100 rounded-lg p-4">
            <h3 class="text-xl font-semibold mb-2">Circulating Supply</h3>
            {#if network_info.circulating_supply}
                <div class="tooltip" data-tip={formatLovelace(parseInt(network_info.circulating_supply))}>
                    <p class="text-gray-700">{formatAda(parseInt(network_info.circulating_supply))} ADA</p>
                </div>
            {:else}
                <span>-</span>
            {/if}
        </div>
    </div>
</section>

<style>
    .container {
        max-width: 1400px;
    }
</style>
