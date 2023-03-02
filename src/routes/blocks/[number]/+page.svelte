<script>
    import {DescriptionList, List} from "flowbite-svelte";

    export let data;
    let {block} = data;
</script>

<section class="text-gray-600 body-font">
    <div class="container flex flex-wrap px-5 py-24 mx-auto items-center">
        <div class="md:w-1/2 md:pr-12 md:py-8 md:border-r md:border-b-0 mb-10 md:mb-0 pb-10 border-b border-gray-200">
            <List tag="dl" color="text-gray-900 dark:text-white">
                <div class="flex flex-col pb-3">
                    <DescriptionList tag="dt" class="mb-1">Hash</DescriptionList>
                    <DescriptionList tag="dd" class="text-sm">{block.hash}</DescriptionList>
                </div>
                <div class="flex flex-col pb-3">
                    <DescriptionList tag="dt" class="mb-1">Number</DescriptionList>
                    <DescriptionList tag="dd" class="text-sm">{block.number}</DescriptionList>
                </div>
                <div class="flex flex-col pb-3">
                    <DescriptionList tag="dt" class="mb-1">Slot</DescriptionList>
                    <DescriptionList tag="dd" class="text-sm">{block.slot}</DescriptionList>
                </div>
                <div class="flex flex-col pb-3">
                    <DescriptionList tag="dt" class="mb-1">Era</DescriptionList>
                    <DescriptionList tag="dd" class="text-sm">{block.era}</DescriptionList>
                </div>
                <div class="flex flex-col pb-3">
                    <DescriptionList tag="dt" class="mb-1">Size</DescriptionList>
                    <DescriptionList tag="dd" class="text-sm">{block.block_body_size/1000} KB</DescriptionList>
                </div>
            </List>
        </div>
        <div class="flex flex-col md:w-1/2 md:pl-12">
            <List tag="dl" color="text-gray-900 dark:text-white">
                <div class="flex flex-col pb-3">
                    <DescriptionList tag="dt" class="mb-1">Prev Hash</DescriptionList>
                    <DescriptionList tag="dd" class="text-sm">{block.prev_hash}</DescriptionList>
                </div>
                <div class="flex flex-col pb-3">
                    <DescriptionList tag="dt" class="mb-1"># of Txs</DescriptionList>
                    <DescriptionList tag="dd" class="text-sm">{block.no_of_txs}</DescriptionList>
                </div>
                <div class="flex flex-col pb-3">
                    <DescriptionList tag="dt" class="mb-1">Protocol version</DescriptionList>
                    <DescriptionList tag="dd" class="text-sm">{block.protocol_version}</DescriptionList>
                </div>
            </List>
        </div>
    </div>
</section>
