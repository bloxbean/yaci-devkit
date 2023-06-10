<script>
    import {onMount} from 'svelte';
    import RecentTxs from "./RecentTxs.svelte";

    let message;
    let messages = [];
    let aggregates = {};

    // let store;
    let unsubscribe;
    onMount(async () => {
        const store = await import('../blocks/store.js')
        console.log(store.subscribe);

        messages = store.txCache;

        // store.sendMessage(messages);
        unsubscribe = store.recentTxStore.subscribe(currentMessage => {
            if (messages.length > 30)
                messages = [currentMessage, ...messages.slice(0, 30)];
            else
                messages = [currentMessage, ...messages];
        });

        //TODO call unsubscribe
        // onDestroy(unsubscribe);
    })
</script>
        <RecentTxs txs={messages} noOfTxs="10"/>
<style>

</style>
