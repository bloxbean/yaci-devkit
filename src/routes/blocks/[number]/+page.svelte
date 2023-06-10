<script>
    import moment from 'moment';

    export let data;
    let {block} = data;

    function getDate(blockTime) {
        if (!blockTime) return '';

        const date = new Date(blockTime * 1000);
        const options = {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: 'numeric',
            minute: 'numeric',
            second: 'numeric',
            hour12: false
        };

        return date.toLocaleString(undefined, options);
    }
</script>
<div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
    <article class="bg-gray-50 rounded-lg shadow-md p-4">
        <div class="flex flex-col">
            <div class="bg-gray-200 py-2 px-4 mb-2">
                <strong>Hash</strong>
            </div>
            <div class="bg-gray-100 py-2 px-4">
                <p>
                    <small>{block.hash}</small>
                </p>
            </div>
        </div>
        <div class="flex flex-col">
            <div class="bg-gray-200 py-2 px-4 mb-2">
                <strong>Number</strong>
            </div>
            <div class="bg-gray-100 py-2 px-4">
                <p>
                    <small>{block.number}</small>
                </p>
            </div>
        </div>
        <div class="flex flex-col">
            <div class="bg-gray-200 py-2 px-4 mb-2">
                <strong>Epoch</strong>
            </div>
            <div class="bg-gray-100 py-2 px-4">
                <p>
                    <small>{block.epoch_number}</small>
                </p>
            </div>
        </div>
        <div class="flex flex-col">
            <div class="bg-gray-200 py-2 px-4 mb-2">
                <strong>Slot</strong>
            </div>
            <div class="bg-gray-100 py-2 px-4">
                <p>
                    <small>{block.slot}</small>
                </p>
            </div>
        </div>
        <div class="flex flex-col">
            <div class="bg-gray-200 py-2 px-4 mb-2">
                <strong>Era</strong>
            </div>
            <div class="bg-gray-100 py-2 px-4">
                <p>
                    <small>{block.era}</small>
                </p>
            </div>
        </div>
    </article>
    <article class="bg-gray-50 rounded-lg shadow-md p-4">
        <div class="flex flex-col">
            <div class="bg-gray-200 py-2 px-4 mb-2">
                <strong>Prev Hash</strong>
            </div>
            <div class="bg-gray-100 py-2 px-4">
                <p>
                    <small>{block.prev_hash}</small>
                </p>
            </div>
        </div>
        <div class="flex flex-col">
            <div class="bg-gray-200 py-2 px-4 mb-2">
                <strong># of Txs</strong>
            </div>
            <div class="bg-gray-100 py-2 px-4">
                <p>
                    <small>{block.no_of_txs}</small>
                </p>
            </div>
        </div>
        <div class="flex flex-col">
            <div class="bg-gray-200 py-2 px-4 mb-2">
                <strong>Size</strong>
            </div>
            <div class="bg-gray-100 py-2 px-4">
                <p>
                    <small>{block.block_body_size}</small>
                </p>
            </div>
        </div>
        <div class="flex flex-col">
            <div class="bg-gray-200 py-2 px-4 mb-2">
                <strong>Protocol version</strong>
            </div>
            <div class="bg-gray-100 py-2 px-4">
                <p>
                    <small>{block.protocol_version}</small>
                </p>
            </div>
        </div>
        <div class="flex flex-col">
            <div class="bg-gray-200 py-2 px-4 mb-2">
                <strong>Timestamp</strong>
            </div>
            <div class="bg-gray-100 py-2 px-4">
                <p>
                    <small>{getDate(block.block_time)}, {moment(block.block_time * 1000).fromNow()}</small>
                </p>
            </div>
        </div>
    </article>
</div>
