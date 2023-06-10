<script>
    import {onDestroy, onMount} from 'svelte';

    let network_info = {};
    // let store;
    let unsubscribe;
    onMount(async () => {
        const store = await import('./blocks/store.js')
        console.log(store.subscribe);

        network_info = {};
        // store.sendMessage(messages);
        unsubscribe = store.blocksStore.subscribe(currentMessage => {
            network_info.epoch = currentMessage.epoch;
            network_info.number = currentMessage.number;
            network_info.block_time = currentMessage.block_time;
            network_info.slot = currentMessage.slot;
            network_info.epoch_slot = currentMessage.epoch_slot;
            network_info.slots_per_epoch = currentMessage.slots_per_epoch;
            if (network_info.epoch_slot &&  network_info.slots_per_epoch) {
                network_info.epoch_progress = Math.round((network_info.epoch_slot / network_info.slots_per_epoch) * 100);
            }
        });

        // const unsubscribeAggregateStore = store.aggregateStore.subscribe(aggrData => {
        //     aggregates = aggrData;
        // })

        //TODO call unsubscribe
        onDestroy(unsubscribe);
    })
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
            <p class="text-gray-700">{network_info.epoch_slot}/{network_info.slots_per_epoch}</p>
        </div>
        <div class="bg-gray-100 rounded-lg p-4">
            <h3 class="text-xl font-semibold mb-2">Epoch Progress</h3>
            <div class="flex items-center">
                <div class="w-3/4 bg-gray-300 rounded-lg overflow-hidden">
                    <div class="bg-blue-500 h-2" style="width: {network_info.epoch_progress}%;"></div>
                </div>
                <p class="ml-2 text-gray-700">{network_info.epoch_progress}%</p>
            </div>
        </div>
    </div>
</section>

<style>
</style>
