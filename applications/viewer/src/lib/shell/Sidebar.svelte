<script lang="ts">
	import { page } from '$app/state';
	import { NAV, activeHref } from './nav';
	import { shell } from './ui.svelte';
	import Logo from './Logo.svelte';
	import PanelLeftClose from '@lucide/svelte/icons/panel-left-close';
	import PanelLeftOpen from '@lucide/svelte/icons/panel-left-open';
	import BookOpen from '@lucide/svelte/icons/book-open';
	import Github from './GithubIcon.svelte';
	import X from '@lucide/svelte/icons/x';

	const current = $derived(activeHref(page.url.pathname));

	// Close the mobile drawer after navigating.
	$effect(() => {
		page.url.pathname;
		shell.drawerOpen = false;
	});
</script>

{#snippet links(compact: boolean)}
	<nav class="flex flex-1 flex-col gap-4 overflow-y-auto px-3 py-3 [scrollbar-width:thin]" aria-label="Main">
		{#each NAV as group (group.label)}
			<div>
				{#if !compact}<div class="y-label mb-1.5 px-2.5">{group.label}</div>{:else}<div class="mx-auto mb-2 h-px w-6 bg-line"></div>{/if}
				<ul class="flex flex-col gap-0.5">
					{#each group.items as item (item.href)}
						{@const active = current === item.href}
						<li>
							<a
								href={item.href}
								class="group relative flex items-center gap-3 rounded-xl px-2.5 py-[7px] text-[13.5px] font-medium transition-colors {active
									? 'bg-brand/10 text-ink'
									: 'text-muted hover:bg-surface-2 hover:text-ink'} {compact ? 'justify-center' : ''}"
								aria-current={active ? 'page' : undefined}
								title={compact ? item.label : undefined}
							>
								{#if active}<span class="absolute -left-3 top-1.5 bottom-1.5 w-[3px] rounded-r-full bg-grad"></span>{/if}
								<item.icon class="h-[18px] w-[18px] shrink-0 {active ? 'text-brand' : 'text-faint group-hover:text-ink'}" />
								{#if !compact}<span class="truncate">{item.label}</span>{/if}
							</a>
						</li>
					{/each}
				</ul>
			</div>
		{/each}
	</nav>
{/snippet}

{#snippet footer(compact: boolean)}
	<div class="flex flex-col gap-0.5 border-t border-line p-3 text-[13px] {compact ? 'items-center' : ''}">
		<a href="https://devkit.yaci.xyz" target="_blank" rel="noopener noreferrer" class="flex items-center gap-3 rounded-xl px-2.5 py-2 text-muted hover:bg-surface-2 hover:text-ink" title="Docs">
			<BookOpen class="h-4 w-4 shrink-0" />{#if !compact}Docs{/if}
		</a>
		<a href="https://github.com/bloxbean/yaci-devkit" target="_blank" rel="noopener noreferrer" class="flex items-center gap-3 rounded-xl px-2.5 py-2 text-muted hover:bg-surface-2 hover:text-ink" title="GitHub">
			<Github class="h-4 w-4 shrink-0" />{#if !compact}GitHub{/if}
		</a>
	</div>
{/snippet}

<!-- Desktop -->
<aside
	class="fixed inset-y-0 left-0 z-40 hidden flex-col border-r border-line bg-surface/80 backdrop-blur-xl transition-[width] duration-200 lg:flex"
	style="width:{shell.collapsed ? 76 : 248}px"
>
	<div class="flex h-16 items-center justify-between px-4 {shell.collapsed ? 'justify-center' : ''}">
		<Logo compact={shell.collapsed} />
		{#if !shell.collapsed}
			<button class="y-icon-btn h-8 w-8 border-transparent bg-transparent" onclick={() => shell.toggleCollapsed()} aria-label="Collapse sidebar">
				<PanelLeftClose class="h-4 w-4" />
			</button>
		{/if}
	</div>
	{#if shell.collapsed}
		<button class="y-icon-btn mx-auto h-8 w-8 border-transparent bg-transparent" onclick={() => shell.toggleCollapsed()} aria-label="Expand sidebar">
			<PanelLeftOpen class="h-4 w-4" />
		</button>
	{/if}
	{@render links(shell.collapsed)}
	{@render footer(shell.collapsed)}
</aside>

<!-- Mobile drawer -->
{#if shell.drawerOpen}
	<div class="fixed inset-0 z-50 lg:hidden">
		<button class="absolute inset-0 bg-black/50 backdrop-blur-sm" onclick={() => (shell.drawerOpen = false)} aria-label="Close menu"></button>
		<aside class="absolute inset-y-0 left-0 flex w-[280px] max-w-[85vw] animate-[slide-in_0.25s_ease-out] flex-col border-r border-line bg-surface">
			<div class="flex h-16 items-center justify-between px-4">
				<Logo />
				<button class="y-icon-btn h-8 w-8" onclick={() => (shell.drawerOpen = false)} aria-label="Close menu"><X class="h-4 w-4" /></button>
			</div>
			{@render links(false)}
			{@render footer(false)}
		</aside>
	</div>
{/if}
