<script>
    import Inputs from "./Inputs.svelte";
    import Outputs from "./Outputs.svelte";

    export let tx = {};
</script>

<div class="text-gray-600 body-font">
    <div class="container px-5 py-10 mx-auto">
        <div class="flex flex-wrap -m-4">
            <div class="p-4 w-full">
                <h2 class="text-xl font-semibold text-gray-700">Inputs</h2>
                <Inputs inputs={tx.inputs}></Inputs>
            </div>
            <div class="p-4 w-full">
                <h2 class="text-xl font-semibold text-gray-700">Outputs</h2>
                <Outputs outputs={tx.outputs}></Outputs>
            </div>
        </div>
    </div>
</div>
