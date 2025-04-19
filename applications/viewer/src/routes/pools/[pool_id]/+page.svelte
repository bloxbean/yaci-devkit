<script lang="ts">
    import { onMount } from 'svelte';
    import { formatAda } from '$lib/util';

    export let data;

    interface Pool {
        epoch: number;
        pool_id: string;
        pool_hash: string;
        vrf_key_hash: string;
        pledge: string;
        cost: string;
        margin: string;
        reward_account: string;
        pool_owners: string[];
        relays: {
            port: number;
            ipv4: string | null;
            ipv6: string | null;
            dnsName: string;
        }[];
        metadata_url: string;
        metadata_hash: string;
        tx_hash: string;
        cert_index: number;
        status: string;
        retire_epoch: number | null;
    }

    // Add toast state
    let showToast = false;
    let toastTimeout: number;
    let toastMessage = '';

    // Add clipboard function with toast
    async function copyToClipboard(text: string, message: string) {
        try {
            await navigator.clipboard.writeText(text);
            showToast = true;
            toastMessage = message;
            if (toastTimeout) {
                clearTimeout(toastTimeout);
            }
            toastTimeout = setTimeout(() => {
                showToast = false;
            }, 2000);
        } catch (err) {
            console.error('Failed to copy text: ', err);
        }
    }

    // Cleanup timeout on component destroy
    onMount(() => {
        return () => {
            if (toastTimeout) {
                clearTimeout(toastTimeout);
            }
        };
    });

    // Remove rewards table state and functions
    let loading = false;
    let error: string | null = null;
    let rewards: any[] = [];
    let currentPage = 1;
    let totalPages = 1;
    let totalItems = 0;
    let latestEpoch: number | null = null;
    const itemsPerPage = 10;

    // Remove loadLatestEpoch and loadRewards functions

    // Remove onMount and goToPage functions
</script>

<section class="container mx-auto px-4 py-8">
    <!-- Toast Notification -->
    {#if showToast}
        <div class="fixed bottom-4 right-4 bg-green-500 text-white px-4 py-2 rounded-lg shadow-lg transition-opacity duration-300 z-50">
            {toastMessage}
        </div>
    {/if}

    {#if data.error}
        <div class="text-center text-red-500 mt-4">
            {data.error}
        </div>
    {:else}
        <!-- Header Section -->
        <div class="bg-white shadow-lg rounded-lg p-6 mb-6">
            <div class="flex flex-col gap-4">
                <div class="flex flex-col gap-2">
                    <h1 class="text-2xl font-bold text-gray-800">Pool Details</h1>
                    <div class="flex flex-wrap items-center gap-4">
                        <div class="flex items-center gap-2">
                            <span class="text-gray-500">Status:</span>
                            <span class="px-3 py-1 rounded-full text-sm font-medium {data.pool.status === 'active' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}">
                                {data.pool.status}
                            </span>
                        </div>
                        <div class="flex items-center gap-2">
                            <span class="text-gray-500">Epoch:</span>
                            <span class="font-medium text-gray-700">{data.pool.epoch}</span>
                        </div>
                    </div>
                </div>
                <div class="flex flex-col gap-2">
                    <span class="text-gray-500">Pool ID:</span>
                    <div class="flex items-center gap-2 bg-gray-50 p-3 rounded">
                        <span class="font-mono text-gray-700 truncate" title={data.pool.pool_id}>{data.pool.pool_id}</span>
                        <button 
                            class="text-gray-400 hover:text-gray-600 transition-colors duration-150" 
                            on:click={() => copyToClipboard(data.pool.pool_id, 'Pool ID copied to clipboard!')}
                            title="Copy pool ID"
                        >
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
                            </svg>
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <!-- Basic Information Card -->
            <div class="bg-white shadow-lg rounded-lg p-6">
                <h2 class="text-lg font-semibold text-gray-800 mb-4 flex items-center gap-2">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    Basic Information
                </h2>
                <div class="space-y-4">
                    <div class="flex items-center gap-2">
                        <span class="text-gray-500 w-32">Pool Hash:</span>
                        <span class="font-mono text-gray-700 truncate flex-1" title={data.pool.pool_hash}>{data.pool.pool_hash}</span>
                        <button 
                            class="text-gray-400 hover:text-gray-600 transition-colors duration-150 flex-shrink-0" 
                            on:click={() => copyToClipboard(data.pool.pool_hash, 'Pool hash copied to clipboard!')}
                            title="Copy pool hash"
                        >
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
                            </svg>
                        </button>
                    </div>
                    <div class="flex items-center gap-2">
                        <span class="text-gray-500 w-32">VRF Key Hash:</span>
                        <span class="font-mono text-gray-700 truncate flex-1" title={data.pool.vrf_key_hash}>{data.pool.vrf_key_hash}</span>
                        <button 
                            class="text-gray-400 hover:text-gray-600 transition-colors duration-150 flex-shrink-0" 
                            on:click={() => copyToClipboard(data.pool.vrf_key_hash, 'VRF key hash copied to clipboard!')}
                            title="Copy VRF key hash"
                        >
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
                            </svg>
                        </button>
                    </div>
                </div>
            </div>

            <!-- Financial Information Card -->
            <div class="bg-white shadow-lg rounded-lg p-6">
                <h2 class="text-lg font-semibold text-gray-800 mb-4 flex items-center gap-2">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    Financial Information
                </h2>
                <div class="space-y-4">
                    <div class="flex items-center gap-2">
                        <span class="text-gray-500 w-32">Pledge:</span>
                        <span class="font-medium text-gray-700 flex-1" title="{data.pool.pledge} lovelace">{formatAda(data.pool.pledge)} ADA</span>
                    </div>
                    <div class="flex items-center gap-2">
                        <span class="text-gray-500 w-32">Cost:</span>
                        <span class="font-medium text-gray-700 flex-1" title="{data.pool.cost} lovelace">{formatAda(data.pool.cost)} ADA</span>
                    </div>
                    <div class="flex items-center gap-2">
                        <span class="text-gray-500 w-32">Margin:</span>
                        <span class="font-medium text-gray-700 flex-1">{(parseFloat(data.pool.margin) * 100).toFixed(2)}%</span>
                    </div>
                    <div class="flex items-center gap-2">
                        <span class="text-gray-500 w-32">Reward Account:</span>
                        <span class="font-mono text-gray-700 truncate flex-1" title={data.pool.reward_account}>{data.pool.reward_account}</span>
                        <button 
                            class="text-gray-400 hover:text-gray-600 transition-colors duration-150 flex-shrink-0" 
                            on:click={() => copyToClipboard(data.pool.reward_account, 'Reward account copied to clipboard!')}
                            title="Copy reward account"
                        >
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
                            </svg>
                        </button>
                    </div>
                </div>
            </div>

            <!-- Pool Owners Card -->
            <div class="bg-white shadow-lg rounded-lg p-6">
                <h2 class="text-lg font-semibold text-gray-800 mb-4 flex items-center gap-2">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                    </svg>
                    Pool Owners
                </h2>
                <div class="space-y-3">
                    {#each data.pool.pool_owners as owner}
                        <div class="flex items-center gap-2 bg-gray-50 p-3 rounded">
                            <span class="font-mono text-gray-700 truncate flex-1" title={owner}>{owner}</span>
                            <button 
                                class="text-gray-400 hover:text-gray-600 transition-colors duration-150 flex-shrink-0" 
                                on:click={() => copyToClipboard(owner, 'Pool owner address copied to clipboard!')}
                                title="Copy pool owner address"
                            >
                                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
                                </svg>
                            </button>
                        </div>
                    {/each}
                </div>
            </div>

            <!-- Relays Card -->
            <div class="bg-white shadow-lg rounded-lg p-6">
                <h2 class="text-lg font-semibold text-gray-800 mb-4 flex items-center gap-2">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 12h14M5 12a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v4a2 2 0 01-2 2M5 12a2 2 0 00-2 2v4a2 2 0 002 2h14a2 2 0 002-2v-4a2 2 0 00-2-2m-2-4h.01M17 16h.01" />
                    </svg>
                    Relays
                </h2>
                <div class="space-y-3">
                    {#each data.pool.relays as relay}
                        <div class="bg-gray-50 p-4 rounded-lg">
                            <div class="flex items-center gap-2 mb-2">
                                <span class="text-gray-500">DNS Name:</span>
                                <span class="font-medium text-gray-700 truncate flex-1" title={relay.dnsName}>{relay.dnsName}</span>
                                <button 
                                    class="text-gray-400 hover:text-gray-600 transition-colors duration-150 flex-shrink-0" 
                                    on:click={() => copyToClipboard(relay.dnsName, 'DNS name copied to clipboard!')}
                                    title="Copy DNS name"
                                >
                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
                                    </svg>
                                </button>
                            </div>
                            <div class="flex items-center gap-2">
                                <span class="text-gray-500">Port:</span>
                                <span class="font-medium text-gray-700">{relay.port}</span>
                            </div>
                        </div>
                    {/each}
                </div>
            </div>

            <!-- Metadata Card -->
            <div class="bg-white shadow-lg rounded-lg p-6">
                <h2 class="text-lg font-semibold text-gray-800 mb-4 flex items-center gap-2">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                    Metadata
                </h2>
                <div class="space-y-4">
                    <div class="flex items-center gap-2">
                        <span class="text-gray-500 w-32">URL:</span>
                        <a href={data.pool.metadata_url} target="_blank" class="text-blue-500 hover:underline truncate flex-1" title={data.pool.metadata_url}>
                            {data.pool.metadata_url}
                        </a>
                    </div>
                    <div class="flex items-center gap-2">
                        <span class="text-gray-500 w-32">Hash:</span>
                        <span class="font-mono text-gray-700 truncate flex-1" title={data.pool.metadata_hash}>{data.pool.metadata_hash}</span>
                        <button 
                            class="text-gray-400 hover:text-gray-600 transition-colors duration-150 flex-shrink-0" 
                            on:click={() => copyToClipboard(data.pool.metadata_hash, 'Metadata hash copied to clipboard!')}
                            title="Copy metadata hash"
                        >
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
                            </svg>
                        </button>
                    </div>
                </div>
            </div>

            <!-- Transaction Card -->
            <div class="bg-white shadow-lg rounded-lg p-6">
                <h2 class="text-lg font-semibold text-gray-800 mb-4 flex items-center gap-2">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                    </svg>
                    Transaction
                </h2>
                <div class="flex items-center gap-2">
                    <span class="text-gray-500 w-32">Hash:</span>
                    <a href="/transactions/{data.pool.tx_hash}" class="text-blue-500 hover:underline font-mono truncate flex-1" title={data.pool.tx_hash}>
                        {data.pool.tx_hash}
                    </a>
                    <button 
                        class="text-gray-400 hover:text-gray-600 transition-colors duration-150 flex-shrink-0" 
                        on:click={() => copyToClipboard(data.pool.tx_hash, 'Transaction hash copied to clipboard!')}
                        title="Copy transaction hash"
                    >
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
                        </svg>
                    </button>
                </div>
            </div>

            <!-- Rewards Link Card -->
            <div class="bg-white shadow-lg rounded-lg p-6">
                <h2 class="text-lg font-semibold text-gray-800 mb-4 flex items-center gap-2">
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    Rewards
                </h2>
                <div class="flex flex-col gap-4">
                    <p class="text-gray-600">
                        View rewards for this pool in the latest epoch ({data.latestEpoch}).
                    </p>
                    <a 
                        href="/rewards/pool/{data.pool.pool_id}/epoch/{data.latestEpoch}" 
                        class="inline-flex items-center justify-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                    >
                        View Latest Rewards
                    </a>
                </div>
            </div>
        </div>
    {/if}
</section>

<style>
    /* Add transition for smooth appearance/disappearance */
    .fixed {
        transition: opacity 0.3s ease-in-out;
        z-index: 50;
    }
</style> 