<script>
    import AmountBadges from "../AmountBadges.svelte";
    import AddressLink from "../AddressLink.svelte";
    import {MoreHorizontalIcon} from "svelte-feather-icons";
    import OutputDetails from "./OutputDetails.svelte";

    export let inputs = []

    let showDetails = false;
    let selectedInput = {};

    const toggleDatum = (input) => {
        selectedInput = input;
        showDetails = !showDetails;
    };

    function closeDetails() {
        showDetails = false;
    }
</script>

<div>
    {#each inputs as input}
        <div class="mt-4">
            <div class="text-sm font-medium text-base-content/80 md:ml-2 break-words"><AddressLink address={input.address} maxLength={45} /></div>
            <div class="text-xs text-base-content/60 break-words"><a href="/transactions/{input.tx_hash}">{input.tx_hash}#{input.output_index}</a></div>
            <div class="flex justify-end mt-2">
                <div class="space-x-2">
                    <AmountBadges amounts={input.amount}></AmountBadges>
                </div>
            </div>
            {#if input.inline_datum_json || input.script_ref}
                <div class="flex justify-end mt-2 items-center">
                    <div class="ml-2">
                        <button class="text-blue-500 underline"
                                on:click={() => { console.log('Button clicked'); toggleDatum(input); }}>
                            <MoreHorizontalIcon class="cursor-pointer"/>
                        </button>
                    </div>
                </div>
            {/if}
        </div>
        <hr class="my-4 border-base-200">
    {/each}
</div>

<OutputDetails output={selectedInput} show={showDetails} on:closeDetails={closeDetails}></OutputDetails>
