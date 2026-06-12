<script>
    import {createEventDispatcher} from "svelte";
    import {XIcon} from "svelte-feather-icons";

    export let drep = {};
    export let show = false;

    const dispatch = createEventDispatcher();

    function close() {
        dispatch('closeDetails');
    }
</script>

{#if show}
    <div class="fixed inset-0 bg-gray-600 bg-opacity-50 flex justify-center items-center">
        <div class="bg-base-100 p-4 rounded shadow-lg mx-auto flex flex-col items-center">
            <div class="text-right w-full">
                <button on:click={close}>
                    <XIcon class="cursor-pointer"/>
                </button>
            </div>
            <div class="mt-4 text-sm text-base-content/80 break-words w-full">
                <div class="text-lg font-medium text-base-content/80">DRep Id :</div>
                <div>
                    {drep.drep_id}
                </div>
            </div>
            <div class="mt-4 text-sm text-base-content/80 break-words w-full">
                <div class="text-lg font-medium text-base-content/80">DRep Hash :</div>
                <div>
                    {drep.drep_hash}
                </div>
            </div>
            {#if drep.deposit}
                <div class="mt-4 text-sm text-base-content/80 break-words w-full">
                    <div class="text-lg font-medium text-base-content/80">Deposit :</div>
                    <div>
                        {drep.deposit}
                    </div>
                </div>
            {/if}
            {#if drep.anchor_hash}
                <div class="mt-4 text-sm text-base-content/80 break-words w-full">
                    <div class="text-lg font-medium text-base-content/80">Anchor Hash :</div>
                    <div>
                        {drep.anchor_hash}
                    </div>
                </div>
            {/if}
            {#if drep.anchor_url }
                <div class="mt-4 text-sm text-base-content/80 break-words w-full">
                    <div class="text-lg font-medium text-base-content/80">Anchor URL :</div>
                    <div>
                        <a href={drep.anchor_url} target="_blank" class="text-blue-500">{drep.anchor_url}</a>
                    </div>
                </div>
            {/if}
        </div>
    </div>
{/if}
