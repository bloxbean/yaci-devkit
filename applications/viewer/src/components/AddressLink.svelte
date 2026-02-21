<script lang="ts">
    import { truncate } from '$lib/util';

    export let address: string;
    export let maxLength: number = 20;
</script>

<a href="/addresses/{address}" class="text-blue-600 hover:underline" title={address}>{truncate(address, maxLength, '...')}</a>
