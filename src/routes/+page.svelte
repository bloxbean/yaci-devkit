<script>
    import Blocks from "./blocks/Blocks.svelte";
    import Transactions from "./transactions/Transactions.svelte";
</script>

<section class="bg-gradient-to-r from-gray-900 to-black text-white py-20">
    <div class="container mx-auto text-center">
        <h1 class="text-4xl font-bold mb-4">A Blockchain DataViewer For Developers</h1>
        <p class="text-xl">Explore the latest blocks and transactions on the blockchain</p>
    </div>
</section>

<section class="bg-white py-12">
    <div class="container mx-auto grid grid-cols-1 md:grid-cols-4 gap-4">
        <div class="bg-gray-100 rounded-lg p-4">
            <h3 class="text-xl font-semibold mb-2">Epoch</h3>
            <p class="text-gray-700">45</p>
        </div>
        <div class="bg-gray-100 rounded-lg p-4">
            <h3 class="text-xl font-semibold mb-2">Current Block</h3>
            <p class="text-gray-700">987899</p>
        </div>
        <div class="bg-gray-100 rounded-lg p-4">
            <h3 class="text-xl font-semibold mb-2">Slot</h3>
            <p class="text-gray-700">26697/432000</p>
        </div>
        <div class="bg-gray-100 rounded-lg p-4">
            <h3 class="text-xl font-semibold mb-2">Epoch Progress</h3>
            <div class="flex items-center">
                <div class="w-3/4 bg-gray-300 rounded-lg overflow-hidden">
                    <div class="bg-blue-500 h-2" style="width: 23%;"></div>
                </div>
                <p class="ml-2 text-gray-700">23%</p>
            </div>
        </div>
    </div>
</section>


<!-- Content Section -->
<section class="container mx-auto py-10">
    <div class="flex flex-col md:flex-row md:space-x-8">
        <div class="w-full md:w-1/2">
            <!-- Recent Blocks -->
            <Blocks></Blocks>
        </div>
        <div class="w-full md:w-1/2 mt-4 md:mt-0">
            <Transactions></Transactions>
        </div>
    </div>
</section>
