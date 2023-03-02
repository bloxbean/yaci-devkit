<script>
    import {Card, Heading, Badge, Secondary, Span} from "flowbite-svelte";

    export let aggregateData;

    function formatDecimal(number) {
       return (Math.round(number * 100) / 100).toFixed(2);
    }
</script>

<Heading tag="h3" class="mb-4" customSize="text-5xl font-extrabold  md:text-3xl lg:text-3xl"><Span gradient>Last</Span> 5 mins</Heading>
<Card padding="none" size="xl" class="grid md:grid-cols-4">

    <figure class="flex flex-col justify-center items-center p-8 text-center bg-white rounded-t-lg border-b border-gray-200 md:rounded-t-none md:rounded-tl-lg md:border-r dark:bg-gray-800 dark:border-gray-700">
        <blockquote class="mx-auto mb-4 max-w-2xl text-gray-500 dark:text-gray-400">
            <h3 class="p-0 text-lg font-semibold text-gray-900 dark:text-white"><Span gradient>Average Block Size</Span></h3>
        </blockquote>
        <figcaption class="flex justify-center items-center space-x-3">
            <div class="space-y-0.5 font-medium dark:text-white text-left">
                <div>{aggregateData.avgBlockSize/1000} KB</div>
            </div>
        </figcaption>
    </figure>
    <figure class="flex flex-col justify-center items-center p-8 text-center bg-white rounded-tr-lg border-b border-gray-200 md:border-r dark:bg-gray-800 dark:border-gray-700">
        <blockquote class="mx-auto mb-4 max-w-2xl text-gray-500 dark:text-gray-400">
            <h3 class="p-0 text-lg font-semibold text-gray-900 dark:text-white"><Span gradient>Block Size / Capacity (%)</Span></h3>
        </blockquote>
        <figcaption class="flex justify-center items-center space-x-3">
            <div class="space-y-0.5 font-medium dark:text-white text-left">
                <div>{aggregateData.blockSizePerct}%</div>
            </div>
        </figcaption>
    </figure>
    <figure class="flex flex-col justify-center items-center p-8 text-center bg-white rounded-bl-lg border-b border-gray-200 md:border-b-0 md:border-r dark:bg-gray-800 dark:border-gray-700">
        <blockquote class="mx-auto mb-4 max-w-2xl text-gray-500 dark:text-gray-400">
            <h3 class="p-0 text-lg font-semibold text-gray-900 dark:text-white"><Span gradient>Transactions / sec</Span></h3>
        </blockquote>
        <figcaption class="flex justify-center items-center space-x-3">
            <div class="space-y-0.5 font-medium dark:text-white text-left">
                <div>{formatDecimal(aggregateData.tps)}</div>
            </div>
        </figcaption>
    </figure>
    <figure class="flex flex-col justify-center items-center p-8 text-center bg-white rounded-b-lg border-gray-200 md:rounded-br-lg dark:bg-gray-800 dark:border-gray-700">
        <blockquote class="mx-auto mb-4 max-w-2xl text-gray-500 dark:text-gray-400">
            <h3 class="p-0 text-lg font-semibold text-gray-900 dark:text-white"><Span gradient>Transaction Outputs / sec</Span></h3>
        </blockquote>
        <figcaption class="flex justify-center items-center space-x-3">
            <div class="space-y-0.5 font-medium dark:text-white text-left">
                <div>{formatDecimal(aggregateData.tops)}</div>
            </div>
        </figcaption>
    </figure>
</Card>


<style>

</style>
