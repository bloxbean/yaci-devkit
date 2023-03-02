<script>
    import {Textarea} from "flowbite-svelte";

    export let contract;

    let textareaprops = {
        id: 'message',
        name: 'message',
        label: 'Your message',
        rows: 10,
        placeholder: 'Leave a comment...',
    };
</script>
<section class="text-gray-600 body-font overflow-hidden">
    <div class="container px-5 py-5 mx-auto">
        <div class="-my-8 divide-y-5 divide-gray-100">
            <div class="py-2 flex flex-wrap md:flex-nowrap">
                <div class="md:w-32 md:mb-0 mb-6 flex-shrink-0 flex flex-col">
                    <span class="font-semibold title-font text-gray-700">Contract</span>
                </div>
                {contract.script_hash}&nbsp;&nbsp;({contract.type})
            </div>
            <div class="py-2 flex flex-wrap md:flex-nowrap">
                <div class="md:w-32 md:mb-0 mb-6 flex-shrink-0 flex flex-col">
                    <span class="font-semibold title-font text-gray-700">Redeemer</span>
                </div>
                {JSON.stringify(contract.redeemer)}
            </div>
            {#if contract.datum_hash}
                <div class="py-2 flex flex-wrap md:flex-nowrap">
                    <div class="md:w-32 md:mb-0 mb-6 flex-shrink-0 flex flex-col">
                        <span class="font-semibold title-font text-gray-700">Datum Hash</span>
                    </div>
                    {contract.datum_hash}
                </div>
            {/if}
            {#if contract.datum}
                <div class="py-4 flex flex-wrap md:flex-nowrap">
                    <div class="md:w-32 md:mb-0 mb-6 flex-shrink-0 flex flex-col">
                        <span class="font-semibold title-font text-gray-700">Datum</span>
                    </div>
                    <span class=""break-words>
                    {contract.datum}
                    </span>
                </div>
            {/if}
            <div class="py-2 flex flex-wrap md:flex-nowrap">
                <div class="md:w-32 md:mb-0 mb-6 flex-shrink-0 flex flex-col">
                    <span class="font-semibold title-font text-gray-700">Contract Body</span>
                </div>
                <div class="md:flex-grow">
                    <!-- <textarea cols="80">{toPrettyJson(tx)} </textarea> -->
                    <Textarea {...textareaprops} value={contract.script_content} class="h-max"></Textarea>
                </div>
            </div>

        </div>
    </div>
</section>
