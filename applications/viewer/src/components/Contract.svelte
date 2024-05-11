<script>
    export let contract;

    let textareaprops = {
        id: 'message',
        name: 'message',
        label: 'Your message',
        rows: 10,
        placeholder: 'Leave a comment...',
    };

    function formatRedeemer(redeemer) {
        if (redeemer && typeof redeemer === 'object') {
            const { tag, index, data, ex_units } = redeemer;
            const formattedData = data ? `"${data}"` : '';
            const formattedExUnits = ex_units ? JSON.stringify(ex_units) : '';
            return `Tag: ${tag}, Index: ${index}, Data: ${formattedData}, Ex Units: ${formattedExUnits}`;
        }
        return JSON.stringify(redeemer);
    }

</script>

<div class="p-4 w-full">
    <div class="flex flex-col mt-4 md:flex-row md:items-start">
        <div class="md:w-32 md:mb-0 mb-6 flex-shrink-0">
            <span class="font-semibold title-font text-gray-700">Contract</span>
        </div>
        <div class="md:ml-2 break-words">
            <div class="whitespace-normal md:whitespace-pre-line">{contract.script_hash}&nbsp;&nbsp;({contract.type})</div>
        </div>
    </div>
    <div class="flex flex-col mt-4 md:flex-row md:items-center">
        <div class="md:w-32 md:mb-0 mb-6 flex-shrink-0">
            <span class="font-semibold title-font text-gray-700">Redeemer</span>
        </div>
        <span class="md:ml-2 break-all">{formatRedeemer(contract.redeemer)}</span>
    </div>
    {#if contract.datum_hash}
        <div class="flex flex-col mt-4 md:flex-row md:items-start">
            <div class="md:w-32 md:mb-0 mb-6 flex-shrink-0">
                <span class="font-semibold title-font text-gray-700">Datum Hash</span>
            </div>
            <div class="md:ml-2 break-words">
                <div class="whitespace-normal md:whitespace-pre-line">{contract.datum_hash}</div>
            </div>
        </div>
    {/if}
    {#if contract.datum}
        <div class="flex flex-col mt-4 md:flex-row md:items-start">
            <div class="md:w-32 md:mb-0 mb-6 flex-shrink-0">
                <span class="font-semibold title-font text-gray-700">Datum</span>
            </div>
            <div class="md:ml-2 break-words">
                <div class="whitespace-normal md:whitespace-pre-line">{contract.datum}</div>
            </div>
        </div>
    {/if}
    <div class="flex flex-col mt-4 md:flex-row md:items-center">
        <div class="md:w-32 md:mb-0 mb-6 flex-shrink-0">
            <span class="font-semibold title-font text-gray-700">Contract Body</span>
        </div>
        <div class="md:flex-grow">
            <textarea {...textareaprops} value={contract.script_content} class="w-full h-max"></textarea>
        </div>
    </div>
</div>

