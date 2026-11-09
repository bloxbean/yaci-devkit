<script lang="ts">
	import ExternalLink from '@lucide/svelte/icons/external-link';
	import UserRound from '@lucide/svelte/icons/user-round';

	/*
	 * A CIP-108 (governance action) or CIP-119 (DRep) metadata document. Text is shown as text, never as HTML;
	 * line breaks are kept.
	 */
	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	let { doc }: { doc: any } = $props();

	// JSON-LD values can be plain strings or { "@value": "…" }.
	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	const text = (v: any): string => {
		if (v == null) return '';
		if (typeof v === 'string') return v;
		if (typeof v === 'number' || typeof v === 'boolean') return String(v);
		if (Array.isArray(v)) return v.map(text).filter(Boolean).join('\n');
		if (typeof v === 'object' && '@value' in v) return text(v['@value']);
		return '';
	};
	const safeHref = (u: string) => (/^(https?:|ipfs:)/i.test(u) ? u.replace(/^ipfs:\/\//i, 'https://ipfs.io/ipfs/') : null);

	const body = $derived(doc?.body ?? {});
	const title = $derived(text(body.title) || text(body.givenName) || text(body.name));
	const sections = $derived(
		[
			['Abstract', body.abstract],
			['Motivation', body.motivation],
			['Rationale', body.rationale],
			['Objectives', body.objectives],
			['Motivations', body.motivations],
			['Qualifications', body.qualifications]
		]
			.map(([label, v]) => [label as string, text(v)] as const)
			.filter(([, v]) => v)
	);
	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	const references = $derived((Array.isArray(body.references) ? body.references : []) as any[]);
	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	const authors = $derived((Array.isArray(doc?.authors) ? doc.authors : []) as any[]);
</script>

<article class="flex flex-col gap-6">
	{#if title}<h2 class="text-2xl font-semibold leading-tight">{title}</h2>{/if}

	{#if authors.length}
		<div class="flex flex-wrap gap-2">
			{#each authors as a, i (i)}
				<span class="y-pill border-line bg-surface-2 text-muted"><UserRound class="h-3 w-3" />{text(a.name) || 'Anonymous'}{a.witness ? ' · signed' : ''}</span>
			{/each}
		</div>
	{/if}

	{#each sections as [label, value] (label)}
		<section>
			<h3 class="y-label mb-2">{label}</h3>
			<p class="whitespace-pre-line break-words text-[15px] leading-relaxed text-ink">{value}</p>
		</section>
	{/each}

	{#if references.length}
		<section>
			<h3 class="y-label mb-2">References</h3>
			<ul class="flex flex-col gap-1.5">
				{#each references as r, i (i)}
					{@const href = safeHref(text(r.uri))}
					<li class="flex min-w-0 items-center gap-2 text-sm">
						{#if href}
							<a {href} target="_blank" rel="noopener noreferrer nofollow" class="inline-flex min-w-0 items-center gap-1.5 text-brand hover:underline">
								<ExternalLink class="h-3.5 w-3.5 shrink-0" /><span class="truncate">{text(r.label) || text(r.uri)}</span>
							</a>
						{:else}
							<span class="truncate text-muted">{text(r.label) || text(r.uri)}</span>
						{/if}
						{#if r['@type']}<span class="text-[11px] text-faint">{text(r['@type'])}</span>{/if}
					</li>
				{/each}
			</ul>
		</section>
	{/if}

	{#if !title && !sections.length && !references.length}
		<p class="text-sm text-muted">This document doesn't follow CIP-108 or CIP-119, so there is nothing to lay out. The raw JSON is below.</p>
	{/if}
</article>
