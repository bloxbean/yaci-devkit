<script lang="ts">
	import { chain } from '$lib/live/chain.svelte';
	import { dateTime, timeAgo } from '$lib/format';

	/* "12s ago" that keeps counting, with the exact time on hover. */
	let { ts, class: klass = '' }: { ts: number | null | undefined; class?: string } = $props();

	const iso = $derived(ts ? new Date(ts > 1e12 ? ts : ts * 1000).toISOString() : undefined);
</script>

<time class="whitespace-nowrap tabular-nums {klass}" title={dateTime(ts)} datetime={iso}
	>{timeAgo(ts, chain.now)}</time
>
