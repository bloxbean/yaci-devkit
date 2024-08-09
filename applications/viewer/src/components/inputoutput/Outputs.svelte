<script>
    import AmountBadges from "../AmountBadges.svelte";
    import {MoreHorizontalIcon} from "svelte-feather-icons";
    import OutputDetails from "./OutputDetails.svelte";

    export let outputs = [];

    let showDetails = false;
    let selectedOutput = {};

    const toggleDatum = (output) => {
        selectedOutput = output;
        showDetails = !showDetails;
    };

    function closeDetails() {
        showDetails = false;
    }
</script>

<div>
    {#each outputs as output}
        <div class="mt-4">
            <div class="text-sm font-medium text-gray-700 md:ml-2 break-words">{output.address}</div>
            <div class="flex justify-end mt-2">
                <div class="space-x-2">
                    <AmountBadges amounts={output.amount}></AmountBadges>
                </div>
            </div>
            {#if output.inline_datum_json || output.script_ref}
                <div class="flex justify-end mt-2 items-center">
                    <div class="ml-2">
                        <button class="text-blue-500 underline"
                                on:click={() => {toggleDatum(output); }}>
                            <MoreHorizontalIcon class="cursor-pointer"/>
                        </button>
                    </div>
                </div>
            {/if}
        </div>
        <hr class="my-4 border-gray-100">
    {/each}
</div>

    <OutputDetails output={selectedOutput} show={showDetails} on:closeDetails={closeDetails}></OutputDetails>
