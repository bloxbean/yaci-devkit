<script lang="ts">
    import { truncate } from '$lib/util';
    import { goto } from '$app/navigation';
    import { page } from '$app/stores';
    import { onMount } from 'svelte';

    interface Block {
        number: number;
        slot: number;
        slot_leader: string;
        size: number;
        tx_count: number;
    }

    interface SuccessData {
        blocks: Block[];
        total: number;
        total_pages: number;
        page: number; // 0-based from server
        count: number;
        status?: undefined;
        body?: undefined;
    }

    interface ErrorData {
        status: number;
        body: { error?: string };
        blocks?: undefined;
    }

    export let data: SuccessData | ErrorData;

    function isErrorData(data: SuccessData | ErrorData): data is ErrorData {
        return data?.status !== undefined && data.status >= 400;
    }

    let showToast = false;
    let toastTimeout: number;
    let loading = false;
    let currentPage = 1;
    let searchQuery = '';
    const ITEMS_PER_PAGE = 15;

    async function copyToClipboard(text: string) {
        try {
            await navigator.clipboard.writeText(text);
            showToast = true;
            if (toastTimeout) clearTimeout(toastTimeout);
            toastTimeout = setTimeout(() => showToast = false, 2000);
        } catch (err) {
            console.error('Failed to copy text: ', err);
        }
    }

    function goToPage(targetPage: number) {
        if (targetPage < 1 || loading) return;
        loading = true;
        goto(`/blocks?page=${targetPage}&count=${ITEMS_PER_PAGE}`);
    }

    function handleSearch() {
        const q = searchQuery.trim();
        if (!q) return;
        if (/^\d+$/.test(q)) {
            goto(`/blocks/${q}`);
        }
    }

    onMount(() => {
        if (!isErrorData(data)) {
            currentPage = Number(data.page) || 1;
        }
        loading = false;
        return () => { if (toastTimeout) clearTimeout(toastTimeout); };
    });

    $: hasMore = !isErrorData(data) && data.blocks && data.blocks.length === ITEMS_PER_PAGE;

    $: if (data && $page) {
        if (!isErrorData(data)) {
            currentPage = Number(data.page) || 1;
        }
        loading = false;
    }

    const iconCopy = `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg>`;
    const iconCube = `<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path><polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline><line x1="12" y1="22.08" x2="12" y2="12"></line></svg>`;
</script>

<section class="py-8 px-4 md:px-6 min-h-screen">
    <div class="container mx-auto px-4 py-8">

        {#if showToast}
            <div class="fixed bottom-6 right-6 bg-gray-900 text-white px-5 py-3 rounded-lg shadow-xl flex items-center gap-2 z-50 animate-fade-up">
                <svg class="w-5 h-5 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>
                <span class="font-medium text-sm">Copied to clipboard</span>
            </div>
        {/if}

        <div class="flex flex-col md:flex-row justify-between items-start md:items-end mb-6 gap-4">
            <div>
                <h1 class="text-3xl font-bold mb-1">Recent Blocks</h1>
            </div>

            <form on:submit|preventDefault={handleSearch} class="w-full md:w-[400px] relative">
                <input
                    type="text"
                    bind:value={searchQuery}
                    placeholder="Search by block number..."
                    class="input input-bordered w-full bg-white text-black pr-10 focus:outline-none focus:ring-2 focus:ring-blue-500/30 focus:border-blue-500 transition-shadow"
                    spellcheck="false"
                />
                <button type="submit" class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-blue-600 transition-colors" aria-label="Search">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                    </svg>
                </button>
            </form>
        </div>

        {#if !isErrorData(data) && data.blocks && data.blocks.length > 0}
            <div class="flex justify-end mt-4 mb-4">
                <div class="join">
                    <button class="join-item btn btn-sm" on:click={() => goToPage(currentPage - 1)} disabled={currentPage <= 1 || loading}>«</button>
                    <button class="join-item btn btn-sm">Page {currentPage}</button>
                    <button class="join-item btn btn-sm" on:click={() => goToPage(currentPage + 1)} disabled={!hasMore || loading}>»</button>
                </div>
            </div>
        {/if}

        {#if isErrorData(data)}
            <div class="alert alert-error bg-red-50 text-red-600 border border-red-200 mt-4">
                <svg xmlns="http://www.w3.org/2000/svg" class="stroke-current shrink-0 h-6 w-6" fill="none" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
                <span>{data.body?.error || 'Failed to load blocks. Please check your connection to the Yaci node.'}</span>
            </div>
        {:else if !data.blocks || data.blocks.length === 0}
            <div class="bg-white rounded-xl shadow-sm border border-gray-200 p-12 text-center mt-4">
                <h3 class="text-lg font-medium text-gray-900">No blocks found</h3>
                <p class="mt-1 text-sm text-gray-500">Waiting for new blocks to be produced...</p>
            </div>
        {:else}
            <div class="bg-white rounded-xl shadow-sm border border-gray-200 relative">

                {#if loading}
                    <div class="absolute inset-0 bg-white/60 backdrop-blur-sm flex justify-center items-center z-10 rounded-xl">
                        <span class="loading loading-spinner loading-lg text-blue-600"></span>
                    </div>
                {/if}

                <!-- Desktop table -->
                <div class="hidden lg:block overflow-x-auto">
                    <table class="w-full text-left border-collapse">
                        <thead>
                            <tr class="bg-gray-50/80 border-b border-gray-100 text-xs font-bold text-gray-500 uppercase tracking-wider">
                                <th class="py-4 px-6">Block</th>
                                <th class="py-4 px-6">Slot</th>
                                <th class="py-4 px-6">Pool</th>
                                <th class="py-4 px-6">Size (KB)</th>
                                <th class="py-4 px-6"># of Txs</th>
                            </tr>
                        </thead>
                        <tbody class="divide-y divide-gray-100">
                            {#each data.blocks as block (block.number)}
                                <tr class="hover:bg-gray-50/50 transition-colors group">
                                    <td class="py-4 px-6 align-middle">
                                        <div class="flex items-center gap-2">
                                            <span class="text-gray-400">{@html iconCube}</span>
                                            <a href="/blocks/{block.number}" class="font-mono text-sm text-blue-600 hover:text-blue-800 hover:underline font-medium">
                                                {block.number}
                                            </a>
                                            <button
                                                class="text-gray-400 hover:text-gray-700 opacity-0 group-hover:opacity-100 transition-opacity"
                                                on:click={() => copyToClipboard(String(block.number))}
                                                title="Copy block number">
                                                {@html iconCopy}
                                            </button>
                                        </div>
                                    </td>
                                    <td class="py-4 px-6 align-middle">
                                        <span class="text-sm text-gray-600">{block.slot}</span>
                                    </td>
                                    <td class="py-4 px-6 align-middle">
                                        <div class="flex items-center gap-2">
                                            <span class="font-mono text-sm text-gray-700" title={block.slot_leader}>
                                                {truncate(block.slot_leader, 20)}
                                            </span>
                                            <button
                                                class="text-gray-400 hover:text-gray-700 opacity-0 group-hover:opacity-100 transition-opacity"
                                                on:click={() => copyToClipboard(block.slot_leader)}
                                                title="Copy pool ID">
                                                {@html iconCopy}
                                            </button>
                                        </div>
                                    </td>
                                    <td class="py-4 px-6 align-middle">
                                        <span class="text-sm text-gray-600">{(block.size / 1024).toFixed(2)}</span>
                                    </td>
                                    <td class="py-4 px-6 align-middle">
                                        <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-50 text-blue-700">
                                            {block.tx_count}
                                        </span>
                                    </td>
                                </tr>
                            {/each}
                        </tbody>
                    </table>
                </div>

                <!-- Mobile card view -->
                <div class="lg:hidden divide-y divide-gray-100">
                    {#each data.blocks as block (block.number)}
                        <div class="p-4 hover:bg-gray-50/50 transition-colors">
                            <div class="flex justify-between items-start mb-3">
                                <div>
                                    <div class="text-xs font-bold text-gray-400 uppercase tracking-wider mb-1">Block</div>
                                    <a href="/blocks/{block.number}" class="font-mono text-sm text-blue-600 hover:underline font-medium">
                                        {block.number}
                                    </a>
                                </div>
                                <button class="p-2 bg-gray-100 text-gray-600 rounded-md active:bg-gray-200 shrink-0" on:click={() => copyToClipboard(String(block.number))}>
                                    {@html iconCopy}
                                </button>
                            </div>

                            <div class="grid grid-cols-2 gap-3 mb-3">
                                <div class="bg-gray-50 p-3 rounded-lg border border-gray-100">
                                    <div class="text-xs text-gray-500 mb-1">Slot</div>
                                    <div class="text-sm font-medium text-gray-900">{block.slot}</div>
                                </div>
                                <div class="bg-gray-50 p-3 rounded-lg border border-gray-100">
                                    <div class="text-xs text-gray-500 mb-1"># of Txs</div>
                                    <div class="text-sm font-medium text-gray-900">{block.tx_count}</div>
                                </div>
                            </div>

                            <div class="flex justify-between items-center text-xs text-gray-500 border-t border-gray-100 pt-3">
                                <div class="truncate mr-4">
                                    Pool: <span class="font-mono font-medium text-gray-700" title={block.slot_leader}>{truncate(block.slot_leader, 20)}</span>
                                </div>
                                <div class="shrink-0">
                                    Size: <span class="font-medium text-gray-700">{(block.size / 1024).toFixed(2)} KB</span>
                                </div>
                            </div>
                        </div>
                    {/each}
                </div>
            </div>

            <div class="flex justify-end mt-4 mb-4">
                <div class="join">
                    <button class="join-item btn btn-sm" on:click={() => goToPage(currentPage - 1)} disabled={currentPage <= 1 || loading}>«</button>
                    <button class="join-item btn btn-sm">Page {currentPage}</button>
                    <button class="join-item btn btn-sm" on:click={() => goToPage(currentPage + 1)} disabled={!hasMore || loading}>»</button>
                </div>
            </div>
        {/if}
    </div>
</section>

<style>
    @keyframes fadeUp {
        from { opacity: 0; transform: translateY(10px); }
        to { opacity: 1; transform: translateY(0); }
    }
    .animate-fade-up {
        animation: fadeUp 0.3s ease-out forwards;
    }
</style>
