<script>
    export let aggregateData;

    function formatDecimal(number) {
       return (Math.round(number * 100) / 100).toFixed(2);
    }
</script>

<section class="container mx-auto">
    <h3 class="mb-4 text-5xl font-extrabold md:text-3xl lg:text-3xl">
        <span class="gradient">Last</span> 5 mins
    </h3>
    <div class="grid grid-cols-1 gap-4 md:grid-cols-4">
        <div class="flex flex-col justify-center items-center p-8 text-center bg-white rounded-lg border-b border-gray-200 md:rounded-t-none md:rounded-tl-lg md:border-r dark:bg-gray-800 dark:border-gray-700">
            <h3 class="mb-4 text-lg font-semibold text-gray-900 dark:text-white">
                <span class="gradient">Average Block Size</span>
            </h3>
            <div class="space-y-0.5 font-medium dark:text-white text-left">
                <div>{aggregateData.avgBlockSize / 1000} KB</div>
            </div>
        </div>
        <div class="flex flex-col justify-center items-center p-8 text-center bg-white rounded-lg border-b border-gray-200 md:border-r dark:bg-gray-800 dark:border-gray-700">
            <h3 class="mb-4 text-lg font-semibold text-gray-900 dark:text-white">
                <span class="gradient">Block Size / Capacity (%)</span>
            </h3>
            <div class="space-y-0.5 font-medium dark:text-white text-left">
                <div>{aggregateData.blockSizePerct}%</div>
            </div>
        </div>
        <div class="flex flex-col justify-center items-center p-8 text-center bg-white rounded-lg border-b border-gray-200 md:border-b-0 md:border-r dark:bg-gray-800 dark:border-gray-700">
            <h3 class="mb-4 text-lg font-semibold text-gray-900 dark:text-white">
                <span class="gradient">Transactions / sec</span>
            </h3>
            <div class="space-y-0.5 font-medium dark:text-white text-left">
                <div>{formatDecimal(aggregateData.tps)}</div>
            </div>
        </div>
        <div class="flex flex-col justify-center items-center p-8 text-center bg-white rounded-lg border-gray-200 md:rounded-br-lg dark:bg-gray-800 dark:border-gray-700">
            <h3 class="mb-4 text-lg font-semibold text-gray-900 dark:text-white">
                <span class="gradient">Transaction Outputs / sec</span>
            </h3>
            <div class="space-y-0.5 font-medium dark:text-white text-left">
                <div>{formatDecimal(aggregateData.tops)}</div>
            </div>
        </div>
    </div>
</section>

<style>

</style>
