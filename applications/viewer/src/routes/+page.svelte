<script lang="ts">
    import Blocks from "./blocks/Blocks.svelte";
    import Transactions from "./transactions/Transactions.svelte";
    import NetworkInfo from "./NetworkInfo.svelte";
    import {onMount} from "svelte";

    interface Block {
        [key: string]: any;
    }

    let blocks: Block[] = [];
    let unsubscribe: () => void;
    onMount(async () => {
        const store = await import('./blocks/store.js')
        blocks = store.blocksCache;
        unsubscribe = store.recentTxStore.subscribe(currentBlocks => {
            if (blocks.length > 30)
                blocks = [currentBlocks, ...blocks.slice(0, 30)];
            else
                blocks = [currentBlocks, ...blocks];
        });
    })
</script>

<!-- Hero Section -->
<section class="bg-gradient-to-br from-gray-900 via-gray-800 to-black text-white py-16">
    <div class="container mx-auto px-4">
        <div class="max-w-4xl mx-auto text-center">
            <div class="flex justify-center mb-4">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                </svg>
            </div>
            <h1 class="text-4xl font-bold mb-4">Blockchain DataViewer</h1>
            <p class="text-lg opacity-90 mb-6">Explore real-time blockchain data with our developer-friendly interface</p>
            <div class="flex flex-wrap justify-center gap-4">
                <a href="/blocks" class="btn btn-lg bg-white text-gray-900 hover:bg-gray-100">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
                    </svg>
                    View Blocks
                </a>
                <a href="/transactions" class="btn btn-outline btn-lg text-white border-white hover:bg-white hover:text-gray-900">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                    </svg>
                    View Transactions
                </a>
            </div>
        </div>
    </div>
</section>

{#if blocks.length == 0 || (blocks.length > 0 && Object.keys(blocks[0]).length === 0)}
    <section class="flex flex-col items-center mt-24">
        <div class="card bg-white shadow-xl w-96">
            <div class="card-body items-center text-center">
                <span class="text-xl font-bold mb-2 text-gray-800">Waiting for live data</span>
                <span class="loading loading-ring loading-lg text-gray-900"></span>
            </div>
        </div>
    </section>
{:else}
    <NetworkInfo></NetworkInfo>

    <!-- Content Section -->
    <section class="container mx-auto px-4 py-12">
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
            <!-- Recent Blocks -->
            <div class="bg-white rounded-lg">
                <div class="p-6">
                    <div class="flex items-center mb-4">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
                        </svg>
                        <h2 class="text-2xl font-bold text-gray-900">Recent Blocks</h2>
                    </div>
                    <Blocks></Blocks>
                </div>
            </div>

            <!-- Recent Transactions -->
            <div class="bg-white rounded-lg">
                <div class="p-6">
                    <div class="flex items-center mb-4">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                        </svg>
                        <h2 class="text-2xl font-bold text-gray-900">Recent Transactions</h2>
                    </div>
                    <Transactions></Transactions>
                </div>
            </div>
        </div>
    </section>
{/if}

<style>
    .container {
        max-width: 1400px;
    }
</style>
