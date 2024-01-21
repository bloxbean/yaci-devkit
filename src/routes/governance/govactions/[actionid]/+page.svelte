<script>
    import moment from 'moment';
    import JsonContent from "../../../../components/JsonContent.svelte";
    import {lovelaceToAda} from "../../../../util/ada_util.js";

    export let data;
    let {govAction} = data;

    function getDate(time) {
        if (!time) return '';

        const date = new Date(time * 1000);
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
<section class="container mx-auto text-sm">
    <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <article class="bg-gray-50 rounded-lg shadow-md p-4">
            <div class="flex flex-col">
                <div class="bg-gray-200 py-2 px-4 mb-2">
                    <strong>Gov Action Id</strong>
                </div>
                <div class="bg-gray-100 py-2 px-4 break-words">
                    <p>
                        <small>{govAction.tx_hash}#{govAction.index}</small>
                    </p>
                </div>
            </div>
            <div class="flex flex-col">
                <div class="bg-gray-200 py-2 px-4 mb-2">
                    <strong>Return Address</strong>
                </div>
                <div class="bg-gray-100 py-2 px-4">
                    <p>
                        <small>{govAction.return_address}</small>
                    </p>
                </div>
            </div>
            <div class="flex flex-col">
                <div class="bg-gray-200 py-2 px-4 mb-2">
                    <strong>Deposit</strong>
                </div>
                <div class="bg-gray-100 py-2 px-4">
                    <p>
                        <small>{lovelaceToAda(govAction.deposit, 2)} Ada</small>
                    </p>
                </div>
            </div>
            <div class="flex flex-col">
                <div class="bg-gray-200 py-2 px-4 mb-2">
                    <strong>Block</strong>
                </div>
                <div class="bg-gray-100 py-2 px-4">
                    <p>
                        <small>{govAction.block_number}</small>
                    </p>
                </div>
            </div>
            <div class="flex flex-col">
                <div class="bg-gray-200 py-2 px-4 mb-2">
                    <strong>Slot</strong>
                </div>
                <div class="bg-gray-100 py-2 px-4">
                    <p>
                        <small>{govAction.slot}</small>
                    </p>
                </div>
            </div>
        </article>
        <article class="bg-gray-50 rounded-lg shadow-md p-4">
            <div class="flex flex-col">
                <div class="bg-gray-200 py-2 px-4 mb-2">
                    <strong>Type</strong>
                </div>
                <div class="bg-gray-100 py-2 px-4 break-words">
                    <p>
                        <small>{govAction.type}</small>
                    </p>
                </div>
            </div>
            <div class="flex flex-col">
                <div class="bg-gray-200 py-2 px-4 mb-2">
                    <strong>Anchor Url</strong>
                </div>
                <div class="bg-gray-100 py-2 px-4">
                    <p>
                        <small><a href="{govAction.anchor_url}" target="_blank">{govAction.anchor_url}</a></small>
                    </p>
                </div>
            </div>
            <div class="flex flex-col">
                <div class="bg-gray-200 py-2 px-4 mb-2">
                    <strong>Anchor Hash</strong>
                </div>
                <div class="bg-gray-100 py-2 px-4">
                    <p>
                        <small>{govAction.anchor_hash}</small>
                    </p>
                </div>
            </div>
            <div class="flex flex-col">
                <div class="bg-gray-200 py-2 px-4 mb-2">
                    <strong>Time</strong>
                </div>
                <div class="bg-gray-100 py-2 px-4">
                    <p>
                        <small>{getDate(govAction.block_time)}, {moment(govAction.block_time * 1000).fromNow()}</small>
                    </p>
                </div>
            </div>
        </article>
    </div>
</section>

<section class="container mx-auto text-sm mt-10 mb-10">
    <div class="flex flex-col">
        <div class="bg-gray-200 py-2 px-4 mb-2">
            <strong>Details</strong>
        </div>
    </div>
    <div class="overflow-x-auto">
        <JsonContent text={govAction.details}></JsonContent>
    </div>
</section>
