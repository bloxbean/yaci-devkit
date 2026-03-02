<script>
    import moment from 'moment';
    import { lovelaceToAda } from "../../../util/ada_util.js";
    import { truncate } from "../../../util/util.js";
    import AddressLink from "../../../components/AddressLink.svelte";

    export let data;
    let {block, txs} = data;

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

    let copiedHash = false;
    let copiedPrevHash = false;

    const copyToClipboard = (text, type) => {
        if (!text) return;
        navigator.clipboard.writeText(text);
        if (type === 'hash') {
            copiedHash = true;
            setTimeout(() => copiedHash = false, 2000);
        } else {
            copiedPrevHash = true;
            setTimeout(() => copiedPrevHash = false, 2000);
        }
    };

    const iconCopy = `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg>`;
    const iconCheck = `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="text-green-500"><polyline points="20 6 9 17 4 12"></polyline></svg>`;
</script>

<section class="py-10 px-4 md:px-6 min-h-screen">
    <div class="container mx-auto max-w-6xl">
        
        <div class="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden mb-6">
            <div class="grid grid-cols-1 lg:grid-cols-2 divide-y lg:divide-y-0 lg:divide-x divide-gray-100">
                <div class="p-6">
                    <h2 class="text-xs font-bold text-gray-400 uppercase tracking-wider mb-2">Block Hash</h2>
                    <div class="flex items-center justify-between gap-3">
                        <span class="font-mono text-sm md:text-base text-gray-800 break-all">{block.hash}</span>
                        <button 
                            class="p-2 hover:bg-gray-100 rounded-md transition-colors duration-200 shrink-0" 
                            on:click={() => copyToClipboard(block.hash, 'hash')}
                            title="Copy Hash">
                            {@html copiedHash ? iconCheck : iconCopy}
                        </button>
                    </div>
                </div>

                <div class="p-6 bg-gray-50/50">
                    <h2 class="text-xs font-bold text-gray-400 uppercase tracking-wider mb-2">Previous Hash</h2>
                    <div class="flex items-center justify-between gap-3">
                        <span class="font-mono text-sm md:text-base text-gray-500 break-all">
                            {block.previous_block ? block.previous_block : 'Genesis Block'}
                        </span>
                        {#if block.previous_block}
                            <button 
                                class="p-2 hover:bg-gray-200 rounded-md transition-colors duration-200 shrink-0" 
                                on:click={() => copyToClipboard(block.previous_block, 'prev')}
                                title="Copy Previous Hash">
                                {@html copiedPrevHash ? iconCheck : iconCopy}
                            </button>
                        {/if}
                    </div>
                </div>
            </div>
        </div>

        <div class="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden mb-10">
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 divide-y md:divide-y-0 md:divide-x divide-gray-100 border-b border-gray-100">
                <div class="p-6 hover:bg-gray-50/50 transition-colors">
                    <div class="text-xs font-bold text-gray-400 uppercase tracking-wider mb-1">Number</div>
                    <div class="text-gray-900 font-medium text-xl">{block.number}</div>
                </div>
                <div class="p-6 hover:bg-gray-50/50 transition-colors">
                    <div class="text-xs font-bold text-gray-400 uppercase tracking-wider mb-1">Epoch</div>
                    <div class="text-gray-900 font-medium text-xl">{block.epoch}</div>
                </div>
                <div class="p-6 hover:bg-gray-50/50 transition-colors">
                    <div class="text-xs font-bold text-gray-400 uppercase tracking-wider mb-1">Slot</div>
                    <div class="text-gray-900 font-medium text-xl">{block.slot}</div>
                </div>
                <div class="p-6 hover:bg-gray-50/50 transition-colors">
                    <div class="text-xs font-bold text-gray-400 uppercase tracking-wider mb-1">Era</div>
                    <div class="text-gray-900 font-medium text-xl capitalize">{block.era}</div>
                </div>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 divide-y md:divide-y-0 md:divide-x divide-gray-100">
                <div class="p-6 hover:bg-gray-50/50 transition-colors">
                    <div class="text-xs font-bold text-gray-400 uppercase tracking-wider mb-1"># of Txs</div>
                    <div class="text-gray-900 font-medium text-xl">{block.tx_count}</div>
                </div>
                <div class="p-6 hover:bg-gray-50/50 transition-colors">
                    <div class="text-xs font-bold text-gray-400 uppercase tracking-wider mb-1">Size</div>
                    <div class="text-gray-900 font-medium text-xl">{block.size} <span class="text-sm font-normal text-gray-500">bytes</span></div>
                </div>
                <div class="p-6 hover:bg-gray-50/50 transition-colors">
                    <div class="text-xs font-bold text-gray-400 uppercase tracking-wider mb-1">Protocol Version</div>
                    <div class="text-gray-900 font-medium text-xl">{block.protocol_version}</div>
                </div>
                <div class="p-6 hover:bg-gray-50/50 transition-colors">
                    <div class="text-xs font-bold text-gray-400 uppercase tracking-wider mb-1">Timestamp</div>
                    <div class="text-gray-900 font-medium text-sm mt-1">
                        {getDate(block.time)}
                        <span class="block text-gray-500 text-xs mt-1">{moment(block.time * 1000).fromNow()}</span>
                    </div>
                </div>
            </div>
        </div>

        {#if txs && txs.length > 0}
            <div class="mt-8">
                <h3 class="text-lg font-bold text-gray-800 mb-4 px-1">Transactions in this Block</h3>
                <div class="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
                    <div class="overflow-x-auto">
                        <table class="w-full text-left border-collapse">
                            <thead>
                                <tr class="bg-gray-50/80 border-b border-gray-100 text-xs uppercase tracking-wider text-gray-500">
                                    <th class="py-4 px-6 font-semibold">Tx Hash</th>
                                    <th class="py-4 px-6 font-semibold">Total Output (ADA)</th>
                                    <th class="py-4 px-6 font-semibold">Fee (ADA)</th>
                                    <th class="py-4 px-6 font-semibold">Output Addresses</th>
                                </tr>
                            </thead>
                            <tbody class="divide-y divide-gray-100">
                                {#each txs as tx}
                                    <tr class="hover:bg-gray-50/50 transition-colors group">
                                        <td class="py-4 px-6 align-top">
                                            <a href="/transactions/{tx.tx_hash}" class="text-blue-600 hover:text-blue-800 hover:underline font-mono text-sm">
                                                {truncate(tx.tx_hash, 20, "...")}
                                            </a>
                                        </td>
                                        <td class="py-4 px-6 align-top font-medium text-gray-900">
                                            {lovelaceToAda(tx.total_output)}
                                        </td>
                                        <td class="py-4 px-6 align-top font-medium text-gray-900">
                                            {lovelaceToAda(tx.fee)}
                                        </td>
                                        <td class="py-4 px-6 align-top text-sm text-gray-500 font-mono">
                                            <div class="max-h-24 overflow-y-auto pr-2 scrollbar-thin">
                                                {#each tx.output_addresses as address}
                                                    <div class="mb-1 last:mb-0 hover:text-gray-800 transition-colors" title="{address}">
                                                        {truncate(address, 25, "...")}
                                                    </div>
                                                {/each}
                                            </div>
                                        </td>
                                    </tr>
                                {/each}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        {/if}

    </div>
</section>

{#if txs.length > 0}
    <section class="container mx-auto text-sm mt-10 mb-10">
        <div class="overflow-x-auto">
            <table class="w-full bg-white border border-gray-300">
                <thead>
                <tr>
                    <th class="py-2 px-4 bg-gray-100 font-bold">Tx Hash</th>
                    <th class="py-2 px-4 bg-gray-100 font-bold text-center">Total Output (Ada)</th>
                    <th class="py-2 px-4 bg-gray-100 font-bold text-center">Fee (Ada)</th>
                    <th class="py-2 px-4 bg-gray-100 font-bold">Output Addresses</th>
                </tr>
                </thead>
                <tbody>
                {#each txs as tx, index}
                    <tr class="{index % 2 === 0 ? 'bg-gray-50' : 'bg-white'}">
                        <td class="py-2 px-4">
                            <a href="/transactions/{tx.tx_hash}" class="text-blue-500">{tx.tx_hash}</a>
                        </td>
                        <td class="py-2 px-4 text-center">{lovelaceToAda(tx.total_output)}</td>
                        <td class="py-2 px-4 text-center">{lovelaceToAda(tx.fee)}</td>
                        <td class="py-2 px-4">
                            {#each tx.output_addresses as address}
                                <AddressLink {address} maxLength={25} /><br>
                            {/each}
                        </td>
                    </tr>
                {/each}
                </tbody>
            </table>
        </div>
    </section>
{/if}
