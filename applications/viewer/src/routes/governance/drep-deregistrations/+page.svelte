<script lang="ts">
	import DRepCertList from '$lib/governance/DRepCertList.svelte';

	let { data } = $props();
</script>

<svelte:head><title>DRep retirements · Yaci Viewer</title></svelte:head>

<DRepCertList kind={data.kind} rows={data.rows} page={data.page} count={data.count} />
