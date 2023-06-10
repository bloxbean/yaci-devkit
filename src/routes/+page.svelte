<script>
    import Blocks from "./blocks/Blocks.svelte";
    import Transactions from "./transactions/Transactions.svelte";
    import NetworkInfo from "./NetworkInfo.svelte";
</script>

<section class="bg-gradient-to-r from-gray-900 to-black text-white py-20">
    <div class="container mx-auto text-center">
        <h1 class="text-4xl font-bold mb-4">A Blockchain DataViewer For Developers</h1>
        <p class="text-xl">Explore the latest blocks and transactions on the blockchain</p>
    </div>
</section>

<NetworkInfo></NetworkInfo>


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
