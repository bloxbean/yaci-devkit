<script lang="ts">
	import type { EntityKind } from '$lib/search/classify';
	import Box from '@lucide/svelte/icons/box';
	import ArrowLeftRight from '@lucide/svelte/icons/arrow-left-right';
	import Wallet from '@lucide/svelte/icons/wallet';
	import KeyRound from '@lucide/svelte/icons/key-round';
	import Server from '@lucide/svelte/icons/server';
	import Users from '@lucide/svelte/icons/users';
	import Coins from '@lucide/svelte/icons/coins';
	import ShieldCheck from '@lucide/svelte/icons/shield-check';
	import FingerprintPattern from '@lucide/svelte/icons/fingerprint-pattern';
	import FileCode from '@lucide/svelte/icons/file-code';
	import Braces from '@lucide/svelte/icons/braces';
	import Timer from '@lucide/svelte/icons/timer';
	import ScrollText from '@lucide/svelte/icons/scroll-text';

	let { kind, class: klass = 'h-4 w-4' }: { kind: EntityKind; class?: string } = $props();

	const icons = {
		block: Box,
		tx: ArrowLeftRight,
		address: Wallet,
		stake: KeyRound,
		pool: Server,
		drep: Users,
		asset: Coins,
		policy: ShieldCheck,
		fingerprint: FingerprintPattern,
		script: FileCode,
		datum: Braces,
		epoch: Timer,
		govaction: ScrollText
	};
	const Icon = $derived(icons[kind]);
</script>

<Icon class={klass} />
