<script>
    import {onDestroy, onMount} from 'svelte';
    import RecentBlocks from "./RecentBlocks.svelte";
    import BlocksAggr from "./BlocksAggr.svelte";

    let message;
    let messages = [];
    let aggregates = {};

    // let store;
    let unsubscribe;
    onMount(async () => {
        const store = await import('./store.js')
        console.log(store.subscribe);

        // store.sendMessage(messages);
        unsubscribe = store.blocksStore.subscribe(currentMessage => {
            if (messages.length > 30)
                messages = [currentMessage, ...messages.slice(0, 30)];
            else
                messages = [currentMessage, ...messages];
        });

        const unsubscribeAggregateStore = store.aggregateStore.subscribe(aggrData => {
            aggregates = aggrData;
        })

        //TODO call unsubscribe
        // onDestroy(unsubscribe);
    })
</script>

<!-- <div class="grid grid-flow-row-dense grid-cols-5 grid-rows-1 gap-x-4">
    <div class=" col-span-3"> -->

        <RecentBlocks blocks={messages} noOfBlocks="10"/>
    <!-- </div>
</div> -->
<style>

</style>
