<script lang="ts">
    import { onMount } from 'svelte';
    export let contract;
    export let index: number = 0;

    let textareaprops = {
        id: 'message',
        name: 'message',
        label: 'Your message',
        rows: 10,
        placeholder: 'Leave a comment...',
    };

    let showToast = false;
    let toastMessage = '';
    let toastTimeout: number;

    interface Redeemer {
        tag: string;
        index: number;
        data?: string;
        ex_units?: any;
    }

    function formatRedeemer(redeemer: Redeemer | string): string {
        if (redeemer && typeof redeemer === 'object') {
            const { tag, index, data, ex_units } = redeemer;
            const formattedData = data ? `"${data}"` : '';
            const formattedExUnits = ex_units ? JSON.stringify(ex_units) : '';
            return `Tag: ${tag}, Index: ${index}, Data: ${formattedData}, Ex Units: ${formattedExUnits}`;
        }
        return JSON.stringify(redeemer);
    }

    async function copyToClipboard(text: string, message: string): Promise<void> {
        try {
            await navigator.clipboard.writeText(text);
            showToast = true;
            toastMessage = message;
            // Clear any existing timeout
            if (toastTimeout) {
                clearTimeout(toastTimeout);
            }
            // Hide toast after 2 seconds
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
</script>

<div class="p-4 w-full">
    <!-- Toast Notification -->
    {#if showToast}
        <div class="fixed top-4 right-4 bg-green-500 text-white px-4 py-2 rounded shadow-lg z-50 transition-opacity duration-300">
            {toastMessage}
        </div>
    {/if}

    <!-- Contract Section -->
    <div class="bg-white rounded-lg shadow-sm p-4 mb-4">
        <div class="flex flex-col">
            <div class="text-sm font-medium text-gray-500 mb-2">Contract {index + 1}</div>
            <div class="flex items-center gap-2">
                <div class="text-sm text-gray-900 break-all">{contract.script_hash}</div>
                <span class="text-sm text-gray-500">({contract.type})</span>
            </div>
        </div>
    </div>

    <!-- Redeemer Section -->
    <div class="bg-white rounded-lg shadow-sm p-4 mb-4">
        <div class="flex flex-col">
            <div class="text-sm font-medium text-gray-500 mb-2">Redeemer</div>
            {#if contract.redeemer && typeof contract.redeemer === 'object'}
                <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div class="flex flex-col">
                        <span class="text-xs text-gray-500">Tag</span>
                        <span class="text-sm text-gray-900">{contract.redeemer.tag}</span>
                    </div>
                    <div class="flex flex-col">
                        <span class="text-xs text-gray-500">Index</span>
                        <span class="text-sm text-gray-900">{contract.redeemer.index}</span>
                    </div>
                    {#if contract.redeemer.data}
                        <div class="flex flex-col">
                            <span class="text-xs text-gray-500">Data</span>
                            <span class="text-sm text-gray-900 break-all">{contract.redeemer.data}</span>
                        </div>
                    {/if}
                    {#if contract.redeemer.ex_units}
                        <div class="grid grid-cols-2 gap-4">
                            <div class="flex flex-col">
                                <span class="text-xs text-gray-500">Memory</span>
                                <span class="text-sm text-gray-900">{contract.redeemer.ex_units.mem.toLocaleString()}</span>
                            </div>
                            <div class="flex flex-col">
                                <span class="text-xs text-gray-500">Steps</span>
                                <span class="text-sm text-gray-900">{contract.redeemer.ex_units.steps.toLocaleString()}</span>
                            </div>
                        </div>
                    {/if}
                </div>
            {:else}
                <div class="text-sm text-gray-900 break-all">{formatRedeemer(contract.redeemer)}</div>
            {/if}
        </div>
    </div>

    <!-- Datum Hash Section -->
    {#if contract.datum_hash}
        <div class="bg-white rounded-lg shadow-sm p-4 mb-4">
            <div class="flex flex-col">
                <div class="text-sm font-medium text-gray-500 mb-2">Datum Hash</div>
                <div class="flex items-start gap-2">
                    <div class="text-sm text-gray-900 break-all flex-grow">{contract.datum_hash}</div>
                    <button 
                        class="text-gray-400 hover:text-gray-600 flex-shrink-0" 
                        on:click={() => copyToClipboard(contract.datum_hash, 'Datum Hash copied to clipboard')}
                        title="Copy datum hash"
                    >
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
                        </svg>
                    </button>
                </div>
            </div>
        </div>
    {/if}

    <!-- Datum Section -->
    {#if contract.datum}
        <div class="bg-white rounded-lg shadow-sm p-4 mb-4">
            <div class="flex flex-col">
                <div class="text-sm font-medium text-gray-500 mb-2">Datum</div>
                <div class="flex items-start gap-2">
                    <div class="text-sm text-gray-900 break-all flex-grow">{contract.datum}</div>
                    <button 
                        class="text-gray-400 hover:text-gray-600 flex-shrink-0" 
                        on:click={() => copyToClipboard(contract.datum, 'Datum copied to clipboard')}
                        title="Copy datum value"
                    >
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
                        </svg>
                    </button>
                </div>
            </div>
        </div>
    {/if}

    <!-- Contract Body Section -->
    <div class="bg-white rounded-lg shadow-sm p-4">
        <div class="flex flex-col">
            <div class="text-sm font-medium text-gray-500 mb-2">Contract Body</div>
            <div class="flex items-start gap-2">
                <textarea {...textareaprops} value={contract.script_content} class="w-full h-max font-mono text-sm"></textarea>
                <button 
                    class="text-gray-400 hover:text-gray-600 flex-shrink-0" 
                    on:click={() => copyToClipboard(contract.script_content, 'Contract Body copied to clipboard')}
                    title="Copy contract body"
                >
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
                    </svg>
                </button>
            </div>
        </div>
    </div>
</div>

<style>
    /* Add transition for smooth appearance/disappearance */
    .fixed {
        transition: opacity 0.3s ease-in-out;
        z-index: 50;
    }

    /* Style for the textarea */
    textarea {
        font-family: monospace;
        line-height: 1.5;
        padding: 0.5rem;
        border: 1px solid #e5e7eb;
        border-radius: 0.375rem;
        background-color: #f9fafb;
    }

    textarea:focus {
        outline: none;
        border-color: #3b82f6;
        box-shadow: 0 0 0 1px #3b82f6;
    }
</style>

