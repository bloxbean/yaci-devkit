<script lang="ts">
	let { rows = 6, class: klass = '' }: { rows?: number; class?: string } = $props();
</script>

<div class="flex flex-col gap-3 p-5 {klass}" aria-busy="true" aria-label="Loading">
	{#each Array(rows) as _, i}
		<div class="flex items-center gap-4">
			<div class="y-skel h-4 w-24"></div>
			<div class="y-skel h-4 flex-1" style="max-width:{60 + ((i * 37) % 35)}%"></div>
			<div class="y-skel h-4 w-16"></div>
		</div>
	{/each}
</div>
