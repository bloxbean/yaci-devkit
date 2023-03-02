<script>
    import {Textarea} from "flowbite-svelte";
    import AmountBadges from "./AmountBadges.svelte";

    let textareaprops = {
        id: 'message',
        name: 'message',
        label: 'Your message',
        rows: 30,
        placeholder: 'Leave a comment...',
    };

    export let tx = {};

    function toPrettyJson(obj) {
        return JSON.stringify(obj, null, 2)
    }

</script>


<section class="text-gray-600 body-font overflow-hidden">
    <div class="container px-5 py-24 mx-auto">
        <div class="-my-8 divide-y-2 divide-gray-100">
            <div class="py-8 flex flex-wrap md:flex-nowrap">

                <div class="md:flex-grow">
                    <!-- <textarea cols="80">{toPrettyJson(tx)} </textarea> -->
                    <Textarea {...textareaprops} value={toPrettyJson(tx)} class="h-max"></Textarea>
                </div>
            </div>

        </div>
    </div>
</section>
