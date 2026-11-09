<script lang="ts">
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import ValidatorLab from '$lib/julc/authoring/ValidatorLab.svelte';
	import FlaskConical from '@lucide/svelte/icons/flask-conical';

	let { data } = $props();
</script>

<svelte:head><title>Validator lab · Yaci Viewer</title></svelte:head>

<PageHeader kind="Script Lab" title="Validator lab">
	{#snippet icon()}<FlaskConical class="h-5 w-5" />{/snippet}
	Write a validator in Java (or bring a compiled one), point it at real UTxOs from this chain and see whether it accepts, as you type. Nothing is deployed, signed or submitted.
</PageHeader>

<ValidatorLab params={data.params} genesis={data.genesis} tipSlot={data.tipSlot} initialScript={data.script} scriptMissing={data.scriptMissing} initialUtxo={data.utxo} />
