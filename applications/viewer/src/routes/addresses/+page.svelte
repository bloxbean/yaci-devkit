<script lang="ts">
	import { chain } from '$lib/live/chain.svelte';
	import LookupHero from '$lib/ui/LookupHero.svelte';
	import Wallet from '@lucide/svelte/icons/wallet';

	// Recent output addresses make handy examples on a devnet.
	const examples = $derived(
		[...new Set(chain.txs.flatMap((t) => t.outputAddresses))].slice(0, 2).map((a) => ({ label: `${a.slice(0, 12)}…${a.slice(-4)}`, value: a }))
	);

	function resolve(v: string) {
		if (/^(stake|stake_test)1/.test(v)) return `/stake/${v}`;
		if (/^(addr|addr_test)1/.test(v) || /^(Ae2|DdzFF)/.test(v)) return `/addresses/${v}`;
		return { error: 'That is not a Cardano address. Addresses start with addr1, addr_test1 or stake1.' };
	}
</script>

<svelte:head><title>Addresses · Yaci Viewer</title></svelte:head>

<LookupHero
	kind="Explore"
	title="Look up an address"
	text="Balance, tokens, unspent outputs and transaction history for any payment or stake address."
	placeholder="addr_test1… or stake_test1…"
	icon={Wallet}
	{resolve}
	{examples}
/>
