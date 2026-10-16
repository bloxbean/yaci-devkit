<script module lang="ts">
export interface Column {
	key: string;
	label: string;
	align?: 'left' | 'right' | 'center';
	/** Hide below the md breakpoint, so small screens keep the essentials. */
	minor?: boolean;
	class?: string;
}
</script>

<script lang="ts" generics="T">
	import type { Snippet } from 'svelte';
	import { goto } from '$app/navigation';
	import Empty from './Empty.svelte';


	/*
	 * A list table: sticky header, hover rows, optional row links, "new row" flashes, and an empty state.
	 * Cells are rendered by the `cell` snippet, so each page decides how a value looks.
	 */
	let {
		rows,
		columns,
		cell,
		key = undefined,
		rowHref = undefined,
		fresh = undefined,
		emptyTitle = 'Nothing to show',
		emptyText = undefined,
		dense = false
	}: {
		rows: T[];
		columns: Column[];
		cell: Snippet<[T, string]>;
		key?: (row: T) => string | number;
		rowHref?: (row: T) => string | undefined;
		fresh?: (row: T) => boolean;
		emptyTitle?: string;
		emptyText?: string;
		dense?: boolean;
	} = $props();

	function onRowClick(e: MouseEvent, row: T) {
		const href = rowHref?.(row);
		if (!href) return;
		// Let links, buttons and text selection inside the row behave normally.
		const target = e.target as HTMLElement;
		if (target.closest('a,button,input,select') || window.getSelection()?.toString()) return;
		if (e.metaKey || e.ctrlKey) window.open(href, '_blank');
		else goto(href);
	}

	const align = (c: Column) => (c.align === 'right' ? 'text-right' : c.align === 'center' ? 'text-center' : 'text-left');
</script>

{#if rows.length === 0}
	<Empty title={emptyTitle} text={emptyText} />
{:else}
	<div class="overflow-x-auto">
		<table class="w-full border-collapse text-sm">
			<thead>
				<tr class="border-b border-line bg-surface-2/60">
					{#each columns as c (c.key)}
						<th class="y-label whitespace-nowrap px-4 py-2.5 font-semibold {align(c)} {c.minor ? 'hidden md:table-cell' : ''} {c.class ?? ''}">{c.label}</th>
					{/each}
				</tr>
			</thead>
			<tbody>
				{#each rows as row, i (key ? key(row) : i)}
					<tr
						class="border-b border-line/70 transition-colors last:border-0 hover:bg-surface-2/70 {rowHref?.(row) ? 'cursor-pointer' : ''} {fresh?.(row) ? 'animate-flash' : ''}"
						onclick={(e) => onRowClick(e, row)}
					>
						{#each columns as c (c.key)}
							<td class="whitespace-nowrap px-4 {dense ? 'py-2' : 'py-3'} {align(c)} {c.minor ? 'hidden md:table-cell' : ''} {c.class ?? ''}">
								{@render cell(row, c.key)}
							</td>
						{/each}
					</tr>
				{/each}
			</tbody>
		</table>
	</div>
{/if}
