<script>

    import Blocks from "./blocks/Blocks.svelte";
    import Transactions from "./transactions/Transactions.svelte";
</script>

<!-- <div class="grid grid-cols-2 gap-4">
    <Blocks></Blocks>
</div> -->
<div class="py-20" style="background: linear-gradient(90deg, #667eea 0%, #764ba2 100%)"
>
    <div class="container mx-auto px-6">
        <h2 class="text-4xl font-bold mb-2 text-white">
            A Blockchain Data Viewer For Developers
        </h2>
        <h3 class="text-2xl mb-8 text-gray-200">
            Live data from a connected Cardano node
        </h3>
    </div>
</div>

<section class="text-gray-600 body-font overflow-hidden">
    <div class="container px-5 py-24 mx-auto">
        <div class="flex flex-wrap -m-12">
            <div class="p-12 md:w-1/2 flex flex-col items-start">
                <span class="inline-block py-1 px-2 rounded bg-blue-50 text-blue-500 text-lg font-medium tracking-widest">Recent Blocks</span>
                <Blocks></Blocks>

                <a class="text-blue-500 inline-flex items-center" href="/transactions">More
                    <svg class="w-4 h-4 ml-2" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" fill="none"
                         stroke-linecap="round" stroke-linejoin="round">
                        <path d="M5 12h14"></path>
                        <path d="M12 5l7 7-7 7"></path>
                    </svg>
                </a>
            </div>
            <div class="p-12 md:w-1/2 flex flex-col items-start">
                <span class="inline-block py-1 px-2 rounded bg-blue-50 text-blue-500 text-lg font-medium tracking-widest">Recent Transactions</span>
                <Transactions></Transactions>
                <a class="text-blue-500 inline-flex items-center" href="/transactions">More
                    <svg class="w-4 h-4 ml-2" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" fill="none"
                         stroke-linecap="round" stroke-linejoin="round">
                        <path d="M5 12h14"></path>
                        <path d="M12 5l7 7-7 7"></path>
                    </svg>
                </a>
            </div>
        </div>
    </div>
</section>
