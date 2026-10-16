<script lang="ts">
	import LookupHero from '$lib/ui/LookupHero.svelte';
	import Coins from '@lucide/svelte/icons/coins';

	/* Asset lookup: a unit (policy + name), a policy id, or a CIP-14 fingerprint. */
	function resolve(raw: string) {
		const v = raw.trim();
		if (/^asset1[0-9a-z]{38}$/i.test(v)) return `/assets/fingerprint/${v.toLowerCase()}`;
		if (/^[0-9a-f]+$/i.test(v)) {
			if (v.length === 56) return `/assets/policy/${v.toLowerCase()}`;
			if (v.length > 56 && v.length <= 120) return `/assets/unit/${v.toLowerCase()}`;
		}
		return { error: 'Enter a policy id (56 hex characters), a policy id followed by the asset name in hex, or a fingerprint (asset1…).' };
	}
</script>

<svelte:head><title>Assets · Yaci Viewer</title></svelte:head>

<LookupHero
	kind="Explore"
	title="Look up a token"
	text="Supply, mint and burn history, and where a native asset lives. Works for fungible tokens and NFTs, including CIP-68."
	placeholder="Policy id, asset unit or asset1… fingerprint"
	icon={Coins}
	{resolve}
>
	<div class="grid gap-3 sm:grid-cols-3">
		{#each [{ t: 'Policy id', d: '56 hex characters. Shows every asset minted under it.' }, { t: 'Asset unit', d: 'Policy id followed by the asset name in hex.' }, { t: 'Fingerprint', d: 'The asset1… id from CIP-14, as wallets show it.' }] as tip (tip.t)}
			<div class="y-panel p-4">
				<div class="text-sm font-semibold">{tip.t}</div>
				<p class="mt-1 text-xs text-muted">{tip.d}</p>
			</div>
		{/each}
	</div>
</LookupHero>
