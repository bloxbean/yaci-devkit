<script lang="ts">
    import { parseUnit } from '$lib/utils/asset';
    import { truncate } from '$lib/util';

    export let unit: string;
    export let maxLength: number = 20;

    $: parsed = parseUnit(unit);
    $: displayName = parsed.assetNameUtf8 || truncate(unit, maxLength, '...');
</script>

<a href="/assets/unit/{unit}" class="text-blue-600 hover:underline" title={unit}>{displayName}</a>
