<script lang="ts">
    import { formatAda } from '$lib/util';
    import TruncateCopy from '../../components/TruncateCopy.svelte';
    import EmptyState from '../../components/EmptyState.svelte';

    export let data;

    function getStatusClass(status: string | undefined): string {
        if (!status) return 'badge badge-ghost';
        switch (status.toUpperCase()) {
            case 'LIVE': return 'badge badge-warning';
            case 'ENACTED': return 'badge badge-success';
            case 'RATIFIED': return 'badge badge-info';
            case 'EXPIRED': return 'badge badge-error';
            default: return 'badge badge-ghost';
        }
    }

    function formatGovActionType(type: string): string {
        if (!type) return '';
        return type.split('_').map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()).join(' ');
    }

    function truncateHash(hash: string): string {
        if (!hash) return '';
        return hash.substring(0, 8) + '...' + hash.substring(hash.length - 8);
    }
</script>

<div class="container mx-auto px-4 py-8">
    <h1 class="text-2xl font-bold mb-6">Governance Overview</h1>

    <!-- Summary Cards -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-8">
        <!-- Committee Card -->
        <a href="/governance/committee" class="card bg-base-100 shadow-md hover:shadow-lg transition-shadow">
            <div class="card-body p-4">
                <h2 class="card-title text-sm text-gray-500">Committee</h2>
                {#if data.committee}
                    <p class="text-2xl font-bold">{data.activeCommitteeCount}</p>
                    <p class="text-xs text-gray-400">active members ({data.committee.threshold_numerator}/{data.committee.threshold_denominator} threshold)</p>
                {:else}
                    <p class="text-sm text-gray-400">No data</p>
                {/if}
            </div>
        </a>

        <!-- Constitution Card -->
        <a href="/governance/constitution" class="card bg-base-100 shadow-md hover:shadow-lg transition-shadow">
            <div class="card-body p-4">
                <h2 class="card-title text-sm text-gray-500">Constitution</h2>
                {#if data.constitution}
                    <p class="text-sm font-medium">Epoch {data.constitution.active_epoch}</p>
                    <p class="text-xs text-gray-400 truncate">{data.constitution.anchor_url || 'No anchor'}</p>
                {:else}
                    <p class="text-sm text-gray-400">No data</p>
                {/if}
            </div>
        </a>
    </div>

    <!-- Recent Proposals -->
    <div class="mb-8">
        <div class="flex justify-between items-center mb-4">
            <h2 class="text-lg font-semibold">Recent Proposals</h2>
            <a href="/governance/proposals" class="btn btn-sm btn-ghost">View all &rarr;</a>
        </div>

        {#if data.proposals && data.proposals.length > 0}
            <div class="overflow-x-auto bg-white rounded-lg shadow">
                <table class="table w-full">
                    <thead>
                        <tr class="bg-gray-50">
                            <th class="font-semibold text-gray-700">Tx Hash</th>
                            <th class="font-semibold text-gray-700">Type</th>
                            <th class="font-semibold text-gray-700">Status</th>
                            <th class="font-semibold text-gray-700">Deposit (ADA)</th>
                            <th class="font-semibold text-gray-700">Epoch</th>
                        </tr>
                    </thead>
                    <tbody>
                        {#each data.proposals as proposal}
                            <tr class="hover:bg-gray-50">
                                <td>
                                    <a href="/transactions/{proposal.tx_hash}" class="link link-primary">
                                        {truncateHash(proposal.tx_hash)}
                                    </a>
                                </td>
                                <td>{formatGovActionType(proposal.gov_action?.type || '')}</td>
                                <td><span class={getStatusClass(proposal.status)}>{proposal.status}</span></td>
                                <td>{formatAda(proposal.deposit)}</td>
                                <td>{proposal.epoch}</td>
                            </tr>
                        {/each}
                    </tbody>
                </table>
            </div>
        {:else}
            <EmptyState title="No proposals yet" message="Governance proposals will appear here once submitted." />
        {/if}
    </div>

    <!-- Quick Links -->
    <div>
        <h2 class="text-lg font-semibold mb-4">Governance Pages</h2>
        <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-3">
            <a href="/governance/proposals" class="btn btn-outline btn-sm">Proposals</a>
            <a href="/governance/dreps" class="btn btn-outline btn-sm">DReps</a>
            <a href="/governance/committee" class="btn btn-outline btn-sm">Committee</a>
            <a href="/governance/constitution" class="btn btn-outline btn-sm">Constitution</a>
            <a href="/governance/govactions" class="btn btn-outline btn-sm">Gov Actions</a>
            <a href="/governance/votes" class="btn btn-outline btn-sm">Votes</a>
            <a href="/governance/drep-registrations" class="btn btn-outline btn-sm">DRep Registrations</a>
            <a href="/governance/drep-updates" class="btn btn-outline btn-sm">DRep Updates</a>
            <a href="/governance/drep-deregistrations" class="btn btn-outline btn-sm">DRep DeRegistrations</a>
        </div>
    </div>
</div>
