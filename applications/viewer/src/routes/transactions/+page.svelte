<script lang="ts">
    import { formatAda, truncate } from '$lib/util';
    import { goto } from '$app/navigation';
    import { page } from '$app/stores';
    import { onMount } from 'svelte';
    import AddressLink from '../../components/AddressLink.svelte';

    let showToast = false;
    let toastTimeout: number;
    let loading = false;
    let currentPage = 1; 
    let searchQuery = ''; 
    const ITEMS_PER_PAGE = 15;

    interface TxSummary {
        tx_hash: string;
        block_number: number;
        slot: number;
        total_output: number;
        fee: number;
        output_addresses: string[];
    }
    interface SuccessData {
        txs: TxSummary[];
        total: number;
        total_pages: number;
        page: number; 
        count: number;
        status?: undefined;
        body?: undefined;
    }
    interface ErrorData {
        status: number;
        body: { error?: string };
        txs?: undefined;
    }
    
    export let data: SuccessData | ErrorData;
    
    function isErrorData(data: SuccessData | ErrorData): data is ErrorData { 
        return data?.status !== undefined && data.status >= 400;
    }

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
        const url = `/transactions?page=${targetPage}&count=${ITEMS_PER_PAGE}`;
        goto(url);
    }

    function handleSearch() {
        const q = searchQuery.trim();
        if (!q) return;

        if (q.startsWith('addr') || q.startsWith('stake')) {
            goto(`/addresses/${q}`);
        } else {
            goto(`/transactions/${q}`);
        }
    
    }

    onMount(() => {
        if (!isErrorData(data)) {
            currentPage = Number(data.page) || 1; 
        }
        loading = false;
        return () => { if (toastTimeout) clearTimeout(toastTimeout); };
    });
    
    $: hasMore = !isErrorData(data) && data.txs && data.txs.length === ITEMS_PER_PAGE;
    
    $: if (data && $page) {
        if (!isErrorData(data) && data.txs) { 
            currentPage = Number(data.page) || 1;
        }
        loading = false;
    }

    const iconCopy = `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg>`;
    const iconCube = `<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path><polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline><line x1="12" y1="22.08" x2="12" y2="12"></line></svg>`;
    const iconCardano = `<svg class="h-4 w-4 font-medium" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 36 40" fill="currentColor"><path style="fill-rule: evenodd; stroke-width: 1.45536" d="M 15.4375 -0.10546875 L 9.1875 15.783203 L 0.015625 15.783203 L 0.015625 20.822266 L 7.2070312 20.822266 L 5.53125 25.083984 L 0.015625 25.083984 L 0.015625 30.123047 L 3.5488281 30.123047 L 0.015625 39.105469 L 5.0976562 39.105469 L 8.6386719 30.123047 L 27.353516 30.123047 L 30.894531 39.105469 L 35.976562 39.105469 L 32.443359 30.123047 L 35.976562 30.123047 L 35.976562 25.083984 L 30.460938 25.083984 L 28.785156 20.822266 L 35.976562 20.822266 L 35.976562 15.783203 L 26.804688 15.783203 L 20.554688 -0.10546875 L 15.4375 -0.10546875 z M 17.996094 6.3847656 L 21.701172 15.783203 L 14.291016 15.783203 L 17.996094 6.3847656 z M 12.304688 20.822266 L 23.6875 20.822266 L 25.367188 25.083984 L 10.625 25.083984 L 12.304688 20.822266 z "></path></svg>`;
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
                <h1 class="text-3xl font-bold mb-1">Recent Transactions</h1>
            </div>

            <form on:submit|preventDefault={handleSearch} class="w-full md:w-[400px] relative">
                <input 
                    type="text" 
                    bind:value={searchQuery}
                    placeholder="Search Tx Hash or Address..." 
                    class="input input-bordered w-full bg-base-100 text-black pr-10 focus:outline-none focus:ring-2 focus:ring-blue-500/30 focus:border-blue-500 transition-shadow"
                    spellcheck="false"
                />
                <button type="submit" class="absolute right-3 top-1/2 -translate-y-1/2 text-base-content/50 hover:text-blue-600 transition-colors" aria-label="Search">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                    </svg>
                </button>
            </form>
        </div>

        {#if !isErrorData(data) && data.txs && data.txs.length > 0}
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
                <span>{data.body?.error || 'Failed to load transactions. Please check your connection to the Yaci node.'}</span>
            </div>
        {:else if !data.txs || data.txs.length === 0}
            <div class="bg-base-100 rounded-xl shadow-sm border border-base-300 p-12 text-center mt-4">
                <h3 class="text-lg font-medium text-base-content">No transactions found</h3>
                <p class="mt-1 text-sm text-base-content/60">Waiting for new transactions to be produced...</p>
            </div>
        {:else}
            <div class="bg-base-100 rounded-xl shadow-sm border border-base-300 relative">
                
                {#if loading}
                    <div class="absolute inset-0 bg-base-100/60 backdrop-blur-sm flex justify-center items-center z-10 rounded-xl">
                        <span class="loading loading-spinner loading-lg text-blue-600"></span>
                    </div>
                {/if}

                <div class="hidden lg:block overflow-x-auto">
                    <table class="w-full text-left border-collapse">
                        <thead>
                            <tr class="bg-base-200/80 border-b border-base-200 text-xs font-bold text-base-content/60 uppercase tracking-wider">
                                <th class="py-4 px-6">Tx Hash</th>
                                <th class="py-4 px-6">Block / Slot</th>
                                <th class="py-4 px-6">Total Output</th>
                                <th class="py-4 px-6">Fee</th>
                                <th class="py-4 px-6">Addresses</th>
                            </tr>
                        </thead>
                        <tbody class="divide-y divide-base-200">
                            {#each data.txs as tx (tx.tx_hash)}
                                <tr class="hover:bg-base-200/50 transition-colors group">
                                    <td class="py-4 px-6 align-middle">
                                        <div class="flex items-center gap-2">
                                            <a href="/transactions/{tx.tx_hash}" class="font-mono text-sm text-blue-600 hover:text-blue-800 hover:underline font-medium">
                                                {truncate(tx.tx_hash, 24, "...")}
                                            </a>
                                            <button 
                                                class="text-base-content/50 hover:text-base-content/80 opacity-0 group-hover:opacity-100 transition-opacity" 
                                                on:click={() => copyToClipboard(tx.tx_hash)} title="Copy Hash">
                                                {@html iconCopy}
                                            </button>
                                        </div>
                                    </td>
                                    <td class="py-4 px-6 align-middle">
                                        <div class="flex items-center gap-1">
                                            <span class="text-base-content/50">{@html iconCube}</span>
                                            <a href="/blocks/{tx.block_number}" class="text-sm font-medium text-blue-600 hover:text-blue-800 hover:underline">
                                                {tx.block_number}
                                            </a> <div  class="text-sm font-medium text-base-content/60">/ {tx.slot}</div>
                                        </div>
                                    </td>
                                    <td class="py-4 px-6 align-middle">
                                        <div class="flex items-center gap-2">
                                            <div class="bg-emerald-100 p-1.5 rounded-full text-emerald-600">
                                                {@html iconCardano}
                                            </div>
                                            <div class="text-sm font-semibold text-base-content" title="{tx.total_output} lovelace">
                                                {formatAda(tx.total_output)} <span class="text-xs text-base-content/60 font-normal">ADA</span>
                                            </div>
                                        </div>
                                    </td>
                                    <td class="py-4 px-6 align-middle">
                                        <div class="flex items-center gap-2">
                                            <div class="bg-rose-100 p-1.5 rounded-full text-rose-500">
                                                {@html iconCardano}
                                            </div>
                                            <div class="text-sm font-medium text-base-content/80" title="{tx.fee} lovelace">
                                                {formatAda(tx.fee)} <span class="text-xs text-base-content/50 font-normal">ADA</span>
                                            </div>
                                        </div>
                                    </td>
                                    <td class="py-4 px-6 align-top max-w-xs">
                                        <div class="max-h-20 overflow-y-auto pr-2 scrollbar-thin text-sm font-mono text-base-content/60">
                                            {#if tx.output_addresses && tx.output_addresses.length > 0}
                                                {#each tx.output_addresses as address, i (address + i)}
                                                    <div class="mb-1 truncate hover:text-base-content transition-colors" title={address}>
                                                        <AddressLink {address} maxLength={22} />
                                                    </div>
                                                {/each}
                                            {:else}
                                                <span class="text-base-content/50 italic">No outputs</span>
                                            {/if}
                                        </div>
                                    </td>
                                </tr>
                            {/each}
                        </tbody>
                    </table>
                </div>

                <div class="lg:hidden divide-y divide-base-200">
                    {#each data.txs as tx (tx.tx_hash)}
                        <div class="p-4 hover:bg-base-200/50 transition-colors">
                            <div class="flex justify-between items-start mb-4">
                                <div>
                                    <div class="text-xs font-bold text-base-content/50 uppercase tracking-wider mb-1">Tx Hash</div>
                                    <a href="/transactions/{tx.tx_hash}" class="font-mono text-sm text-blue-600 hover:underline break-all block pr-4">
                                        {tx.tx_hash}
                                    </a>
                                </div>
                                <button class="p-2 bg-base-200 text-base-content/70 rounded-md active:bg-base-300 shrink-0 mt-4" on:click={() => copyToClipboard(tx.tx_hash)}>
                                    {@html iconCopy}
                                </button>
                            </div>

                            <div class="grid grid-cols-2 gap-3 mb-4">
                                <div class="bg-base-200 p-3 rounded-lg border border-base-200">
                                    <div class="text-xs text-base-content/60 mb-1 flex items-center gap-1">{@html iconCube} Block</div>
                                    <a href="/blocks/{tx.block_number}" class="text-sm font-medium text-blue-600">{tx.block_number}</a>
                                    <span class="text-xs text-base-content/50 ml-1">(Slot {tx.slot})</span>
                                </div>
                                <div class="bg-base-200 p-3 rounded-lg border border-base-200 flex flex-col justify-center">
                                    <div class="text-xs text-base-content/60 mb-1 flex items-center gap-1">
                                        <div class="text-emerald-500">{@html iconCardano}</div> Output
                                    </div>
                                    <div class="text-sm font-semibold text-base-content">{formatAda(tx.total_output)}</div>
                                </div>
                            </div>

                            <div class="flex justify-between items-center text-xs text-base-content/60 border-t border-base-200 pt-3">
                                <div class="flex items-center gap-1.5">
                                    <div class="text-rose-500">{@html iconCardano}</div> 
                                    Fee: <span class="font-medium text-base-content/80">{formatAda(tx.fee)} ADA</span>
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
    .scrollbar-thin::-webkit-scrollbar { width: 4px; }
    .scrollbar-thin::-webkit-scrollbar-track { background: transparent; }
    .scrollbar-thin::-webkit-scrollbar-thumb { background-color: #e5e7eb; border-radius: 4px; }
    .scrollbar-thin:hover::-webkit-scrollbar-thumb { background-color: #d1d5db; }
</style>