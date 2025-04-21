<script lang="ts">
    import {onMount} from 'svelte';
    import {page} from "$app/stores";
    import {goto} from '$app/navigation';
    import {formatAda} from '$lib/util';
    import type {Proposal} from "./types";
    import { EyeIcon, CopyIcon, InfoIcon } from 'svelte-feather-icons';
    import { env } from '$env/dynamic/public';

    let proposals: Proposal[] = [];
    let currentPage = 1;
    let itemsPerPage = 10;
    let totalPages = 1;
    let loading = true;
    let error: string | null = null;
    let initialLoad = true;
    let selectedProposal: Proposal | null = null;
    let showModal = false;
    let modalLoading = false;
    let modalError: string | null = null;
    let showToast = false;
    let toastMessage = '';

    // Initialize from URL parameters
    $: {
        const params = new URLSearchParams($page.url.search);
        const pageParam = parseInt(params.get('page') || '1');
        const countParam = parseInt(params.get('count') || '10');
        
        // Only update and reload if the parameters have actually changed
        if (pageParam !== currentPage || countParam !== itemsPerPage) {
            currentPage = pageParam;
            itemsPerPage = countParam;
            if (!initialLoad) {
                // Use a small timeout to prevent multiple rapid reloads
                setTimeout(() => loadProposals(currentPage), 0);
            }
        }
    }

    onMount(() => {
        // Load initial data
        loadProposals(currentPage);
        initialLoad = false;
    });

    async function loadProposals(page: number) {
        loading = true;
        error = null;
        try {
            console.log('Fetching proposals from API...');
            const baseUrl = env.PUBLIC_INDEXER_BASE_URL;
            console.log('Base URL:', baseUrl);
            const apiUrl = `${baseUrl}/governance-state/proposals?page=${page}&count=${itemsPerPage}&order=desc`;
            console.log('Full API URL:', apiUrl);
            const response = await fetch(apiUrl);
            console.log('Response status:', response.status);
            
            if (!response.ok) {
                throw new Error(`Failed to fetch proposals (Status: ${response.status})`);
            }
            
            const data = await response.json();
            console.log('API Response:', data);
            
            // Check if data is in the expected format
            if (!data || !Array.isArray(data)) {
                console.error('Unexpected data format:', data);
                throw new Error('Invalid data format received from server');
            }
            
            proposals = data;
            // If we get a full page of results, assume there are more pages
            totalPages = data.length === itemsPerPage ? page + 1 : page;
            currentPage = page;
            
            console.log('Processed proposals:', proposals);
            console.log('Total pages:', totalPages);
        } catch (e) {
            console.error('Error loading proposals:', e);
            error = e instanceof Error ? e.message : 'An error occurred while loading proposals';
            proposals = [];
        } finally {
            loading = false;
        }
    }

    function goToPage(page: number) {
        if (page < 1 || page > totalPages) return;
        const params = new URLSearchParams();
        params.set('page', page.toString());
        params.set('count', itemsPerPage.toString());
        goto(`?${params.toString()}`);
    }

    function getStatusClass(status: string | undefined): string {
        if (!status) return 'badge badge-ghost';
        switch (status.toUpperCase()) {
            case 'LIVE':
                return 'badge badge-warning';
            case 'ENACTED':
                return 'badge badge-success';
            case 'RATIFIED':
                return 'badge badge-info';
            case 'EXPIRED':
                return 'badge badge-error';
            default:
                return 'badge badge-ghost';
        }
    }

    function formatDate(timestamp: number): string {
        return new Date(timestamp * 1000).toLocaleString();
    }

    function formatDeposit(amount: number): string {
        return formatAda(amount);
    }

    function formatGovActionType(type: string): string {
        return type.split('_').map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()).join(' ');
    }

    function truncateHash(hash: string): string {
        if (!hash) return '';
        return hash.substring(0, 8) + '...' + hash.substring(hash.length - 8);
    }

    function truncateAddress(address: string): string {
        if (!address) return '';
        return address.substring(0, 8) + '...' + address.substring(address.length - 8);
    }

    async function showProposalDetails(txHash: string, index: number) {
        modalLoading = true;
        modalError = null;
        showModal = true;
        try {
            const baseUrl = env.PUBLIC_INDEXER_BASE_URL;
            console.log('Base URL for details:', baseUrl);
            const apiUrl = `${baseUrl}/governance-state/proposals/${txHash}/${index}`;
            console.log('Full API URL for details:', apiUrl);
            const response = await fetch(apiUrl);
            if (!response.ok) {
                throw new Error(`Failed to fetch proposal details (Status: ${response.status})`);
            }
            const data = await response.json();
            selectedProposal = data;
        } catch (e) {
            console.error('Error loading proposal details:', e);
            modalError = e instanceof Error ? e.message : 'An error occurred while loading proposal details';
            selectedProposal = null;
        } finally {
            modalLoading = false;
        }
    }

    function formatGovActionDetails(govAction: any): string {
        return JSON.stringify(govAction, null, 2);
    }

    function copyToClipboard(text: string) {
        navigator.clipboard.writeText(text);
        toastMessage = 'Copied to clipboard';
        showToast = true;
        setTimeout(() => {
            showToast = false;
        }, 2000);
    }

    interface GovAction {
        type: string;
        index: number;
        expiration: number;
        // Add other properties as needed
    }
</script>

<div class="container mx-auto px-4 py-8">
    <!-- Toast Notification -->
    {#if showToast}
        <div class="toast toast-top toast-end">
            <div class="alert alert-success">
                <span>{toastMessage}</span>
            </div>
        </div>
    {/if}

    <div class="flex justify-between items-center mb-6">
        <h1 class="text-2xl font-bold text-gray-900">Governance Action Proposals</h1>
    </div>

    {#if error}
        <div class="alert alert-error mb-4">
            <span>{error}</span>
        </div>
    {/if}

    {#if loading}
        <div class="flex justify-center items-center h-64">
            <span class="loading loading-spinner loading-lg"></span>
        </div>
    {:else if proposals.length === 0}
        <div class="text-center py-8">
            <p class="text-gray-600">No proposals available for this page.</p>
        </div>
    {:else}
        <div class="flex justify-end mt-6">
            <div class="join">
                <button 
                    class="join-item btn btn-sm" 
                    disabled={currentPage === 1}
                    on:click={() => goToPage(currentPage - 1)}
                >
                    «
                </button>
                <button class="join-item btn btn-sm">Page {currentPage}</button>
                <button 
                    class="join-item btn btn-sm" 
                    disabled={currentPage >= totalPages}
                    on:click={() => goToPage(currentPage + 1)}
                >
                    »
                </button>
            </div>
        </div>
        <div class="overflow-x-auto bg-white rounded-lg shadow">
            <table class="table w-full">
                <thead>
                    <tr class="bg-gray-50">
                        <th class="font-semibold text-gray-700">Transaction Hash</th>
                        <th class="font-semibold text-gray-700">Type</th>
                        <th class="font-semibold text-gray-700">Status</th>
                        <th class="font-semibold text-gray-700">Deposit (ADA)</th>
                        <th class="font-semibold text-gray-700">Epoch</th>
                        <th class="font-semibold text-gray-700">Block</th>
                        <th class="font-semibold text-gray-700">Time</th>
                        <th class="font-semibold text-gray-700">Details</th>
                    </tr>
                </thead>
                <tbody>
                    {#each proposals as proposal}
                        <tr class="hover:bg-gray-50">
                            <td>
                                <a href="/transactions/{proposal.tx_hash}" class="link link-primary hover:underline" target="_blank">
                                    {truncateHash(proposal.tx_hash)}
                                </a>
                            </td>
                            <td>{formatGovActionType(proposal.gov_action?.type || '')}</td>
                            <td>
                                <span class={getStatusClass(proposal.status)}>{proposal.status}</span>
                            </td>
                            <td>{formatDeposit(proposal.deposit)}</td>
                            <td>{proposal.epoch}</td>
                            <td>
                                <a href="/blocks/{proposal.block_number}" class="link link-primary hover:underline" target="_blank">
                                    {proposal.block_number}
                                </a>
                            </td>
                            <td>{formatDate(proposal.block_time)}</td>
                            <td>
                                <button 
                                    class="btn btn-ghost btn-sm hover:bg-gray-100"
                                    on:click={() => showProposalDetails(proposal.tx_hash, proposal.index)}
                                >
                                    <EyeIcon size="16" />
                                </button>
                            </td>
                        </tr>
                    {/each}
                </tbody>
            </table>
        </div>

        <div class="flex justify-end mt-6">
            <div class="join">
                <button 
                    class="join-item btn btn-sm" 
                    disabled={currentPage === 1}
                    on:click={() => goToPage(currentPage - 1)}
                >
                    «
                </button>
                <button class="join-item btn btn-sm">Page {currentPage}</button>
                <button 
                    class="join-item btn btn-sm" 
                    disabled={currentPage >= totalPages}
                    on:click={() => goToPage(currentPage + 1)}
                >
                    »
                </button>
            </div>
        </div>
    {/if}
</div>

<!-- Modal -->
{#if showModal}
    <div class="modal modal-open">
        <div class="modal-box max-w-4xl">
            <h3 class="font-bold text-lg mb-4">Proposal Details</h3>
            
            {#if modalLoading}
                <div class="flex justify-center items-center h-32">
                    <span class="loading loading-spinner loading-lg"></span>
                </div>
            {:else if modalError}
                <div class="alert alert-error mb-4">
                    <span>{modalError}</span>
                </div>
            {:else if selectedProposal}
                <div class="grid grid-cols-1 gap-4">
                    <!-- Basic Information -->
                    <div class="card bg-base-100 shadow-sm">
                        <div class="card-body p-4">
                            <h4 class="card-title text-lg mb-4">Basic Information</h4>
                            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                                <div class="space-y-2">
                                    <p class="flex items-center gap-2">
                                        <span class="font-semibold min-w-[120px]">Transaction Hash:</span>
                                        {#if selectedProposal?.tx_hash}
                                            <a href="/transactions/{selectedProposal.tx_hash}" class="link link-primary hover:underline" target="_blank">
                                                {truncateHash(selectedProposal.tx_hash)}
                                            </a>
                                            <button class="btn btn-ghost btn-xs" on:click={() => selectedProposal && copyToClipboard(selectedProposal.tx_hash)} title="Copy full hash">
                                                <CopyIcon size="12" />
                                            </button>
                                        {/if}
                                    </p>
                                    <p class="flex items-center gap-2">
                                        <span class="font-semibold min-w-[120px]">Status:</span>
                                        {#if selectedProposal?.status}
                                            <span class={getStatusClass(selectedProposal.status)}>{selectedProposal.status}</span>
                                        {/if}
                                    </p>
                                    <p class="flex items-center gap-2">
                                        <span class="font-semibold min-w-[120px]">Return Address:</span>
                                        {#if selectedProposal?.return_address}
                                            <span class="tooltip" data-tip={selectedProposal.return_address}>
                                                <a href="/rewards/account/{selectedProposal.return_address}" class="link link-primary hover:underline" target="_blank">
                                                    <span class="font-mono">{truncateAddress(selectedProposal.return_address)}</span>
                                                </a>
                                            </span>
                                            <button class="btn btn-ghost btn-xs" on:click={() => selectedProposal && copyToClipboard(selectedProposal.return_address)} title="Copy full address">
                                                <CopyIcon size="12" />
                                            </button>
                                        {/if}
                                    </p>
                                </div>
                                <div class="space-y-2">
                                    <p class="flex items-center gap-2">
                                        <span class="font-semibold min-w-[120px]">Block:</span>
                                        {#if selectedProposal?.block_number}
                                            <a href="/blocks/{selectedProposal.block_number}" class="link link-primary hover:underline" target="_blank">{selectedProposal.block_number}</a>
                                        {/if}
                                    </p>
                                    <p class="flex items-center gap-2">
                                        <span class="font-semibold min-w-[120px]">Slot:</span>
                                        {#if selectedProposal?.slot}
                                            <span>{selectedProposal.slot}</span>
                                        {/if}
                                    </p>
                                    <p class="flex items-center gap-2">
                                        <span class="font-semibold min-w-[120px]">Time:</span>
                                        {#if selectedProposal?.block_time}
                                            <span>{formatDate(selectedProposal.block_time)}</span>
                                        {/if}
                                    </p>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Governance Action -->
                    <div class="card bg-base-100 shadow-sm">
                        <div class="card-body p-4">
                            <h4 class="card-title text-lg mb-4">Governance Action</h4>
                            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                                <div class="space-y-2">
                                    <p class="flex items-center gap-2">
                                        <span class="font-semibold min-w-[120px]">Type:</span>
                                        {#if selectedProposal?.gov_action?.type}
                                            <span>{formatGovActionType(selectedProposal.gov_action.type)}</span>
                                        {/if}
                                    </p>
                                    <p class="flex items-center gap-2">
                                        <span class="font-semibold min-w-[120px]">Index:</span>
                                        {#if selectedProposal?.index !== undefined}
                                            <span>{selectedProposal.index}</span>
                                        {/if}
                                    </p>
                                </div>
                                <div class="space-y-2">
                                    <p class="flex items-center gap-2">
                                        <span class="font-semibold min-w-[120px]">Deposit:</span>
                                        {#if selectedProposal?.deposit}
                                            <span>{formatDeposit(selectedProposal.deposit)}</span>
                                        {/if}
                                    </p>
                                    <p class="flex items-center gap-2">
                                        <span class="font-semibold min-w-[120px]">Epoch:</span>
                                        {#if selectedProposal?.epoch}
                                            <span>{selectedProposal.epoch}</span>
                                        {/if}
                                    </p>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Anchor Information -->
                    {#if selectedProposal?.anchor_url}
                        <div class="card bg-base-100 shadow-sm">
                            <div class="card-body p-4">
                                <h4 class="card-title text-lg mb-2">Anchor Information</h4>
                                <div class="space-y-2">
                                    <div class="flex items-center gap-2 flex-wrap">
                                        <span class="font-semibold min-w-[120px]">URL:</span>
                                        <div class="flex flex-col sm:flex-row gap-2 items-start sm:items-center flex-wrap">
                                            <a href={selectedProposal.anchor_url} class="link link-primary hover:underline break-all" target="_blank" title="Open raw URL">{selectedProposal.anchor_url}</a>
                                            <a href="/governance/anchor?url={encodeURIComponent(selectedProposal.anchor_url)}" class="btn btn-primary btn-sm whitespace-nowrap" title="View Formatted Metadata" target="_blank">
                                                <span class="flex items-center gap-2">
                                                    <InfoIcon size="16" />
                                                    <span>View Formatted Details</span>
                                                </span>
                                            </a>
                                        </div>
                                    </div>
                                    {#if selectedProposal?.anchor_hash}
                                        <p class="flex items-center gap-2">
                                            <span class="font-semibold min-w-[120px]">Hash:</span>
                                            <span class="font-mono">{selectedProposal.anchor_hash}</span>
                                            <button class="btn btn-ghost btn-xs" on:click={() => selectedProposal && copyToClipboard(selectedProposal.anchor_hash)} title="Copy full hash">
                                                <CopyIcon size="12" />
                                            </button>
                                        </p>
                                    {/if}
                                </div>
                            </div>
                        </div>
                    {/if}

                    <!-- Details -->
                    <div class="card bg-base-100 shadow-sm">
                        <div class="card-body p-4">
                            <h4 class="card-title text-lg mb-2">Details</h4>
                            <pre class="mt-2 p-4 bg-base-200 rounded-lg overflow-x-auto">{formatGovActionDetails(selectedProposal.gov_action)}</pre>
                        </div>
                    </div>
                </div>
            {/if}

            <div class="modal-action">
                <button class="btn" on:click={() => showModal = false}>Close</button>
            </div>
        </div>
    </div>
{/if}

<style>
    .container {
        max-width: 1400px;
    }
    
    pre {
        white-space: pre-wrap;
        word-wrap: break-word;
    }

    .table th {
        padding: 0.75rem 1rem;
    }

    .table td {
        padding: 0.75rem 1rem;
    }

    .toast {
        position: fixed;
        z-index: 1000;
    }
</style> 