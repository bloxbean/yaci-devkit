<script lang="ts">
    import TruncateCopy from '../../../components/TruncateCopy.svelte';
    import EmptyState from '../../../components/EmptyState.svelte';

    export let data;

    $: currentEpoch = data.currentEpoch ?? 0;
    $: uniqueMembers = getUniqueMembers(data.committee?.members);
    $: activeCount = uniqueMembers.filter(m => m.expired_epoch >= currentEpoch).length;
    $: totalCount = uniqueMembers.length;

    function getUniqueMembers(members: any[] | undefined): any[] {
        if (!members) return [];
        const seen = new Set<string>();
        return members.filter(m => {
            if (seen.has(m.hash)) return false;
            seen.add(m.hash);
            return true;
        });
    }
</script>

<div class="container mx-auto px-4 py-8">
    <h1 class="text-2xl font-bold mb-6">Constitutional Committee</h1>

    {#if data.error}
        <div class="alert alert-error mb-4">
            <span>{data.error}</span>
        </div>
    {/if}

    {#if data.committee}
        <!-- Info Bar -->
        <div class="flex flex-wrap gap-4 mb-6">
            <div class="badge badge-lg badge-outline gap-2 p-4">
                <span class="font-semibold">Threshold:</span>
                {data.committee.threshold_numerator}/{data.committee.threshold_denominator}
            </div>
            <div class="badge badge-lg badge-outline gap-2 p-4">
                <span class="font-semibold">Active Members:</span>
                {activeCount}
            </div>
        </div>

        <!-- Members Table -->
        {#if data.committee.members && data.committee.members.length > 0}
            <div class="overflow-x-auto bg-base-100 rounded-lg shadow">
                <table class="table w-full">
                    <thead>
                        <tr class="bg-base-200">
                            <th class="font-semibold text-base-content/80">Hash</th>
                            <th class="font-semibold text-base-content/80">Credential Type</th>
                            <th class="font-semibold text-base-content/80">Start Epoch</th>
                            <th class="font-semibold text-base-content/80">Expiry Epoch</th>
                            <th class="font-semibold text-base-content/80">Status</th>
                        </tr>
                    </thead>
                    <tbody>
                        {#each data.committee.members as member}
                            <tr class="hover:bg-base-200">
                                <td>
                                    <TruncateCopy text={member.hash} max={38} />
                                </td>
                                <td>{member.cred_type || ''}</td>
                                <td>{member.start_epoch}</td>
                                <td>{member.expired_epoch}</td>
                                <td>
                                    {#if member.expired_epoch >= currentEpoch}
                                        <span class="badge badge-success">Active</span>
                                    {:else}
                                        <span class="badge badge-error">Expired</span>
                                    {/if}
                                </td>
                            </tr>
                        {/each}
                    </tbody>
                </table>
            </div>
        {:else}
            <EmptyState title="No members" message="Committee members will appear here once available." />
        {/if}
    {:else if !data.error}
        <EmptyState title="No committee data" message="Committee information will appear here once available." />
    {/if}
</div>
