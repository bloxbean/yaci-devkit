<script lang="ts">
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import GuardrailLab from '$lib/julc/governance/GuardrailLab.svelte';
	import ShieldCheck from '@lucide/svelte/icons/shield-check';

	let { data } = $props();
</script>

<svelte:head><title>Guardrail check · Governance · Yaci Viewer</title></svelte:head>

<PageHeader kind="Governance" title="Guardrail check">
	{#snippet icon()}<ShieldCheck class="h-5 w-5" />{/snippet}
	Would the constitution’s guardrail script accept this proposal? Draft a parameter change or treasury withdrawal and check it in the browser before it is submitted.
</PageHeader>

<GuardrailLab initial={data.draft} params={data.params} recent={data.recent} defaultScript={data.script} constitutionScript={data.constitution?.script ?? null} />
