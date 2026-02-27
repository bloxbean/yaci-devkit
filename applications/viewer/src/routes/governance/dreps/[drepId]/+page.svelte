<script lang="ts">
    import { goto } from '$app/navigation';
    import { formatAda } from '$lib/util';
    import TruncateCopy from '../../../../components/TruncateCopy.svelte';
    import EmptyState from '../../../../components/EmptyState.svelte';

    export let data;

    function getStatusClass(status: string): string {
        if (!status) return 'badge badge-ghost';
        switch (status.toUpperCase()) {
            case 'ACTIVE': return 'badge badge-success';
            case 'INACTIVE': return 'badge badge-error';
            default: return 'badge badge-ghost';
        }
    }

    function truncateHash(hash: string): string {
        if (!hash) return '';
        return hash.substring(0, 8) + '...' + hash.substring(hash.length - 8);
    }

    $: currentPage = parseInt(data.page);
    $: itemsPerPage = parseInt(data.count);
    $: hasMore = data.delegations.length === itemsPerPage;

    const previous = () => {
        let prevPage = currentPage - 1;
        if (prevPage < 1) prevPage = 1;
        goto(`/governance/dreps/${data.drepId}?page=${prevPage}&count=${data.count}`);
    };

    const next = () => {
        let nextPage = currentPage + 1;
        if (data.delegations.length === 0) nextPage = currentPage;
        goto(`/governance/dreps/${data.drepId}?page=${nextPage}&count=${data.count}`);
    };
</script>

<div class="container mx-auto px-4 py-8">
    <div class="mb-2">
        <a href="/governance/dreps" class="btn btn-ghost btn-sm">&larr; Back to DReps</a>
    </div>

    <h1 class="text-2xl font-bold mb-6">DRep Detail</h1>

    <!-- DRep ID -->
    <div class="mb-4">
        <TruncateCopy text={data.drepId} max={80} title="DRep ID" />
    </div>

    {#if data.drep}
        <!-- Info Card -->
        <div class="card bg-base-100 shadow-md mb-8">
            <div class="card-body">
                <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div class="flex flex-col sm:flex-row sm:items-center gap-2">
                        <span class="font-semibold min-w-[140px]">Status:</span>
                        <span class={getStatusClass(data.drep.status)}>{data.drep.status}</span>
                    </div>

                    <div class="flex flex-col sm:flex-row sm:items-center gap-2">
                        <span class="font-semibold min-w-[140px]">Type:</span>
                        <span>{data.drep.drep_type || ''}</span>
                    </div>

                    <div class="flex flex-col sm:flex-row sm:items-center gap-2">
                        <span class="font-semibold min-w-[140px]">DRep Hash:</span>
                        {#if data.drep.drep_hash}
                            <TruncateCopy text={data.drep.drep_hash} max={38} />
                        {:else}
                            <span class="text-gray-400">None</span>
                        {/if}
                    </div>

                    <div class="flex flex-col sm:flex-row sm:items-center gap-2">
                        <span class="font-semibold min-w-[140px]">Deposit:</span>
                        <span>{formatAda(data.drep.deposit)} ADA</span>
                    </div>

                    <div class="flex flex-col sm:flex-row sm:items-center gap-2">
                        <span class="font-semibold min-w-[140px]">Voting Power:</span>
                        <span>{data.drep.voting_power !== null ? formatAda(data.drep.voting_power) + ' ADA' : ''}</span>
                    </div>

                    <div class="flex flex-col sm:flex-row sm:items-center gap-2">
                        <span class="font-semibold min-w-[140px]">Registration Slot:</span>
                        <span>{data.drep.registration_slot}</span>
                    </div>
                </div>
            </div>
        </div>

        <!-- Delegations -->
        <h2 class="text-lg font-semibold mb-4">Delegations</h2>

        {#if data.delegations.length > 0}
            <div class="flex justify-end mb-2">
                <div class="join">
                    <button class="join-item btn btn-sm" disabled={currentPage === 1} on:click={previous}>«</button>
                    <button class="join-item btn btn-sm">Page {currentPage}</button>
                    <button class="join-item btn btn-sm" disabled={!hasMore} on:click={next}>»</button>
                </div>
            </div>

            <div class="overflow-x-auto bg-white rounded-lg shadow mb-4">
                <table class="table w-full">
                    <thead>
                        <tr class="bg-gray-50">
                            <th class="font-semibold text-gray-700">Address</th>
                            <th class="font-semibold text-gray-700">Tx Hash</th>
                            <th class="font-semibold text-gray-700">Block</th>
                            <th class="font-semibold text-gray-700">Slot</th>
                        </tr>
                    </thead>
                    <tbody>
                        {#each data.delegations as delegation}
                            <tr class="hover:bg-gray-50">
                                <td>
                                    {#if delegation.address}
                                        <a href="/addresses/{delegation.address}" class="link link-primary">
                                            <TruncateCopy text={delegation.address} max={30} />
                                        </a>
                                    {:else}
                                        <span class="text-gray-400">-</span>
                                    {/if}
                                </td>
                                <td>
                                    {#if delegation.tx_hash}
                                        <a href="/transactions/{delegation.tx_hash}" class="link link-primary">
                                            {truncateHash(delegation.tx_hash)}
                                        </a>
                                    {:else}
                                        <span class="text-gray-400">-</span>
                                    {/if}
                                </td>
                                <td>
                                    {#if delegation.block_number}
                                        <a href="/blocks/{delegation.block_number}" class="link link-primary">
                                            {delegation.block_number}
                                        </a>
                                    {:else}
                                        <span class="text-gray-400">-</span>
                                    {/if}
                                </td>
                                <td>{delegation.slot ?? '-'}</td>
                            </tr>
                        {/each}
                    </tbody>
                </table>
            </div>

            <div class="flex justify-end">
                <div class="join">
                    <button class="join-item btn btn-sm" disabled={currentPage === 1} on:click={previous}>«</button>
                    <button class="join-item btn btn-sm">Page {currentPage}</button>
                    <button class="join-item btn btn-sm" disabled={!hasMore} on:click={next}>»</button>
                </div>
            </div>
        {:else}
            <EmptyState title="No delegations" message="No delegations found for this DRep." />
        {/if}
    {:else}
        <div class="alert alert-warning">
            <span>DRep not found or failed to load.</span>
        </div>
    {/if}
</div>
