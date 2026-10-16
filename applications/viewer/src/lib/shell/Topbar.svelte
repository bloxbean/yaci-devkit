<script lang="ts">
	import Menu from '@lucide/svelte/icons/menu';
	import Search from '@lucide/svelte/icons/search';
	import { shell } from './ui.svelte';
	import Logo from './Logo.svelte';
	import LiveBadge from './LiveBadge.svelte';
	import ThemeToggle from './ThemeToggle.svelte';

	const isMac = typeof navigator !== 'undefined' && /Mac|iPhone|iPad/.test(navigator.platform);
</script>

<header class="sticky top-0 z-30 border-b border-line/70 bg-bg/75 backdrop-blur-xl">
	<div class="mx-auto flex h-16 max-w-[1480px] items-center gap-3 px-4 sm:px-6 lg:px-8">
		<button class="y-icon-btn lg:hidden" onclick={() => (shell.drawerOpen = true)} aria-label="Open menu"><Menu class="h-4 w-4" /></button>
		<div class="lg:hidden"><Logo compact /></div>

		<button
			class="group hidden h-10 min-w-0 flex-1 items-center gap-3 rounded-xl border border-line bg-surface px-3.5 text-left text-sm text-faint transition hover:border-brand/40 sm:flex md:max-w-xl"
			onclick={() => shell.openPalette()}
			aria-label="Search"
		>
			<Search class="h-4 w-4 shrink-0 transition group-hover:text-brand" />
			<span class="truncate">Search blocks, transactions, addresses, assets…</span>
			<span class="ml-auto hidden shrink-0 items-center gap-1 md:flex">
				<kbd class="y-kbd">{isMac ? '⌘' : 'Ctrl'}</kbd><kbd class="y-kbd">K</kbd>
			</span>
		</button>

		<div class="ml-auto flex items-center gap-2">
			<button class="y-icon-btn sm:hidden" onclick={() => shell.openPalette()} aria-label="Search"><Search class="h-4 w-4" /></button>
			<LiveBadge />
			<ThemeToggle />
		</div>
	</div>
</header>
