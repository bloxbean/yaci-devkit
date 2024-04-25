import {writable} from 'svelte/store';
import { variables } from '../../lib/variables'

export const blocksStore = writable({});
export const aggregateStore = writable({});
export const recentTxStore = writable({});

export let blocksCache = [];
export let txCache = [];

const socket = new WebSocket(variables.wsUrl);

// Connection opened
socket.addEventListener('open', function (event) {
    console.log("It's open !!!");
    keepAlive(30000);
});

socket.addEventListener('close', function (event) {
    console.log("It's close. try to reopen !!!");
});

// Listen for messages
socket.addEventListener('message', function (event) {
    try {
        const data = JSON.parse(event.data);
        if (data.res_type == 'INIT_DATA') {
            console.log("Inside init data")
            data.blocks.forEach(value => blocksStore.set(value));
            data.recent_txs.forEach(value => recentTxStore.set(value));
            if (data.aggregateData)
                aggregateStore.set(data.aggregateData);
        } else if (data.res_type == 'BLOCK_DATA') {
            blocksStore.set(data);
        } else if (data.res_type == 'AGGR_DATA') {
            aggregateStore.set(data);
        }
        else if (data.res_type == 'RECENT_TXS_DATA') {
            console.log(data);
            data.recent_txs.forEach(tx => recentTxStore.set(tx));
        }
        console.log(data);

    } catch (error) {

    }
});

blocksStore.subscribe(currentMessage => {
    if (blocksCache.length > 30)
        blocksCache = [currentMessage, ...blocksCache.slice(0, 30)];
    else
        blocksCache = [currentMessage, ...blocksCache];
});

recentTxStore.subscribe(currentMessage => {
    if (txCache.length > 30)
        txCache = [currentMessage, ...txCache.slice(0, 30)];
    else
        txCache = [currentMessage, ...txCache];
});

export const sendMessage = (message) => {
    if (socket.readyState <= 1) {
        socket.send(message);
    }
}

export const subscribeBlocks = () => {
    blocksStore.subscribe();
}

export const subscribeAggregates = () => {
    aggregateStore.subscribe();
}

let timerId = 0;
function keepAlive(timeout = 20000) {
    sendMessage('PING')
    timerId = setTimeout(keepAlive, timeout);
}

function cancelKeepAlive() {
    if (timerId) {
        clearTimeout(timerId);
    }
}

// export default {
//     messageStore,
//     subscribe,
//     // subscribe: messageStore.subscribe,
//     sendMessage
// }
