<script lang="ts">
    import TruncateCopy from '../../../components/TruncateCopy.svelte';
    import EmptyState from '../../../components/EmptyState.svelte';

    export let data;
</script>

<div class="container mx-auto px-4 py-8">
    <h1 class="text-2xl font-bold mb-6">Constitution</h1>

    {#if data.error}
        <div class="alert alert-error mb-4">
            <span>{data.error}</span>
        </div>
    {/if}

    {#if data.constitution}
        <div class="card bg-base-100 shadow-md">
            <div class="card-body">
                <div class="grid grid-cols-1 gap-4">
                    <div class="flex flex-col sm:flex-row sm:items-center gap-2">
                        <span class="font-semibold min-w-[140px]">Active Epoch:</span>
                        <span>{data.constitution.active_epoch}</span>
                    </div>

                    <div class="flex flex-col sm:flex-row sm:items-start gap-2">
                        <span class="font-semibold min-w-[140px]">Anchor URL:</span>
                        <span class="break-all">
                            {#if data.constitution.anchor_url}
                                <a href={data.constitution.anchor_url} target="_blank" rel="noopener noreferrer" class="link link-primary">
                                    {data.constitution.anchor_url}
                                </a>
                            {:else}
                                <span class="text-base-content/50">None</span>
                            {/if}
                        </span>
                    </div>

                    <div class="flex flex-col sm:flex-row sm:items-center gap-2">
                        <span class="font-semibold min-w-[140px]">Anchor Hash:</span>
                        {#if data.constitution.anchor_hash}
                            <TruncateCopy text={data.constitution.anchor_hash} max={48} />
                        {:else}
                            <span class="text-base-content/50">None</span>
                        {/if}
                    </div>

                    <div class="flex flex-col sm:flex-row sm:items-center gap-2">
                        <span class="font-semibold min-w-[140px]">Script:</span>
                        {#if data.constitution.script}
                            <TruncateCopy text={data.constitution.script} max={48} />
                        {:else}
                            <span class="text-base-content/50">None</span>
                        {/if}
                    </div>
                </div>
            </div>
        </div>
    {:else if !data.error}
        <EmptyState title="No constitution data" message="Constitution information will appear here once available." />
    {/if}
</div>
