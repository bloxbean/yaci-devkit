import {writable} from 'svelte/store';
import {ws_url} from '../../constant.js'

export const blocksStore = writable({});
export const aggregateStore = writable({});

const socket = new WebSocket(ws_url);

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
        if (data.resType == 'INIT_DATA') {
            console.log("Inside init data")
            data.blocks.forEach(value => blocksStore.set(value));
            if (data.aggregateData)
                aggregateStore.set(data.aggregateData);
        } else if (data.resType == 'BLOCK_DATA') {
            blocksStore.set(data);
        } else if (data.resType == 'AGGR_DATA') {
            aggregateStore.set(data);
        }
        console.log(data);

    } catch (error) {

    }
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
