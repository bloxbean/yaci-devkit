<script>
    import {lovelaceToAda} from "../../util/ada_util.js";
    import {Badge} from 'flowbite-svelte';
    import AmountBadges from "../AmountBadges.svelte";
    import Inputs from "./Inputs.svelte";
    import Outputs from "./Outputs.svelte";

    export let tx = {};
</script>

<section class="text-gray-600 body-font overflow-hidden">
    <div class="container px-5 py-24 mx-auto">
        <div class="-my-8 divide-y-2 divide-gray-100">
            <div class="py-8 flex flex-wrap md:flex-nowrap">
                <div class="md:w-64 md:mb-0 mb-6 flex-shrink-0 flex flex-col">
                    <span class="font-semibold title-font text-gray-700">Inputs</span>
                </div>
                <Inputs inputs={tx.inputs}></Inputs>
            </div>
            <div class="py-8 flex flex-wrap md:flex-nowrap">
                <div class="md:w-64 md:mb-0 mb-6 flex-shrink-0 flex flex-col">
                    <span class="font-semibold title-font text-gray-700">Outputs</span>
                </div>
                <Outputs outputs="{tx.outputs}"></Outputs>
            </div>
        </div>
    </div>
</section>
