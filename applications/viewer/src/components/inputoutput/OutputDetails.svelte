<script lang="ts">
    import JsonContent from "../JsonContent.svelte";
    import {createEventDispatcher} from "svelte";
    import {XIcon} from "svelte-feather-icons";

    interface Output {
        inline_datum?: string;
        inline_datum_json?: string;
        script_ref?: string;
        reference_script_hash?: string;
    }

    export let output: Output = {};
    export let show = false;

    const dispatch = createEventDispatcher();

    function close() {
        dispatch('closeDetails');
    }
</script>

{#if show}
    <div class="fixed inset-0 bg-gray-600 bg-opacity-50 flex justify-center items-center z-[9999]">
        <div class="bg-white p-4 rounded shadow-lg mx-auto flex flex-col items-center max-w-3xl w-full max-h-[90vh] overflow-y-auto">
            <div class="sticky top-0 bg-white w-full text-right z-[9999] py-2">
                <button on:click={close} class="hover:bg-gray-100 rounded-full p-1">
                    <XIcon class="cursor-pointer"/>
                </button>
            </div>
            <div class="w-full">
                {#if output.inline_datum}
                    <div class="mt-4 text-sm text-gray-700 break-words w-full">
                        <div class="text-lg font-medium text-gray-700">Datum (Cbor) :</div>
                        <div class="font-mono break-all whitespace-pre-wrap">
                            {output.inline_datum}
                        </div>
                    </div>
                    <div class="mt-4 text-sm text-gray-700 break-words w-full">
                        <div class="text-lg font-medium text-gray-700">Datum (Json) :</div>
                        <JsonContent text={output.inline_datum_json} rows={20}></JsonContent>
                    </div>
                {/if}

                {#if output.script_ref}
                    <div class="mt-4 text-sm text-gray-700 break-words w-full">
                        <div class="text-lg font-medium text-gray-700">Reference Script Hash :</div>
                        <div class="font-mono break-all">
                            {output.reference_script_hash}
                        </div>
                    </div>
                    <div class="mt-4 text-sm text-gray-700 break-words w-full">
                        <div class="text-lg font-medium text-gray-700">Reference Script :</div>
                        <JsonContent text={output.script_ref} rows={10}></JsonContent>
                    </div>
                {/if}
            </div>
        </div>
    </div>
{/if}
