<script>
    let textareaprops = {
        id: 'message',
        name: 'message',
        label: 'Your message',
        rows: 30,
        placeholder: 'Leave a comment...',
    };

    export let text = {};

    function toPrettyJson(obj) {
        if (!obj)
            return "{}";
        return JSON.stringify(obj, null, 2)
    }

</script>

<div class="p-4 w-full">
    <div class="flex flex-col mt-4">
        <div class="md:flex-grow">
            <textarea {...textareaprops} value={toPrettyJson(text)} class="w-full h-max text-base" disabled></textarea>
        </div>
    </div>
</div>


