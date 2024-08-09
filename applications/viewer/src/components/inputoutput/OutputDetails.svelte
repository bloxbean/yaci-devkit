<script>
    import JsonContent from "../JsonContent.svelte";
    import {createEventDispatcher} from "svelte";
    import {XIcon} from "svelte-feather-icons";

    export let output = {};
    export let show = false;

    const dispatch = createEventDispatcher();

    function close() {
        dispatch('closeDetails');
    }
</script>

{#if show}
    <div class="fixed inset-0 bg-gray-600 bg-opacity-50 flex justify-center items-center">
        <div class="bg-white p-4 rounded shadow-lg mx-auto flex flex-col items-center">
            <div class="text-right w-full">
                <button on:click={close}>
                    <XIcon class="cursor-pointer"/>
                </button>
            </div>
            {#if output.inline_datum}
                <div class="mt-4 text-sm text-gray-700 break-words w-full">
                    <div class="text-lg font-medium text-gray-700">Datum (Cbor) :</div>
                    <div>
                        {output.inline_datum}
                    </div>
                </div>
                <div class="mt-4 text-sm text-gray-700 break-words w-full">
                    <div class="text-lg font-medium text-gray-700">Datum (Json) :</div>
                    <JsonContent text={output.inline_datum_json} rows="20"></JsonContent>
                </div>
            {/if}

            {#if output.script_ref}
                <div class="mt-4 text-sm text-gray-700 break-words w-full">
                    <div class="text-lg font-medium text-gray-700">Reference Script Hash :</div>
                    <div>
                        {output.reference_script_hash}
                    </div>
                </div>
                <div class="mt-4 text-sm text-gray-700 break-words w-full">
                    <div class="text-lg font-medium text-gray-700">Reference Script :</div>
                    <JsonContent text={output.script_ref} rows="10"></JsonContent>
                </div>
            {/if}
        </div>
    </div>
{/if}
