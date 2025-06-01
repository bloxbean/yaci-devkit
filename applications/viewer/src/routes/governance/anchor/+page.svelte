<script lang="ts">
    import type { PageData } from './$types';
    import { ExternalLinkIcon } from 'svelte-feather-icons';

    export let data: PageData;

    $: ({ anchorMetadata, error } = data);

    function formatDate(dateString: string): string {
        return new Date(dateString).toLocaleDateString(undefined, {
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        });
    }

    function formatAuthors(authors: any[]): string {
        if (!Array.isArray(authors)) return '';
        return authors.map(author => {
            if (typeof author === 'string') return author;
            return `${author.name}${author.email ? ` <${author.email}>` : ''}`;
        }).join(', ');
    }

    // Sanitize URL to prevent XSS and ensure it's a valid URL
    function sanitizeUrl(url: string): string | null {
        try {
            // Only allow http/https URLs
            const parsedUrl = new URL(url);
            if (!['http:', 'https:'].includes(parsedUrl.protocol)) {
                return null;
            }
            return parsedUrl.toString();
        } catch (e) {
            return null;
        }
    }

    // Sanitize text content to prevent XSS
    function sanitizeText(text: string): string {
        return text
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    // Sanitize object values recursively
    function sanitizeObject(obj: any): any {
        if (typeof obj === 'string') {
            return sanitizeText(obj);
        }
        if (Array.isArray(obj)) {
            return obj.map(item => sanitizeObject(item));
        }
        if (obj && typeof obj === 'object') {
            const sanitized: any = {};
            for (const [key, value] of Object.entries(obj)) {
                sanitized[key] = sanitizeObject(value);
            }
            return sanitized;
        }
        return obj;
    }

    // Format JSON with syntax highlighting and clickable URLs
    function formatJsonForDisplay(obj: any): string {
        // If the input is a string, try to parse it as JSON first
        let jsonObj = obj;
        if (typeof obj === 'string') {
            try {
                jsonObj = JSON.parse(obj);
            } catch (e) {
                // If parsing fails, return the original string
                return obj;
            }
        }

        // Convert to formatted JSON string
        const jsonString = JSON.stringify(jsonObj, null, 2);
        
        // Replace URLs with clickable links (after sanitization)
        return jsonString.replace(/"((?:https?:\/\/)[^"]+)"/g, (match, url) => {
            const sanitizedUrl = sanitizeUrl(url);
            if (sanitizedUrl) {
                return `"<a href="${sanitizedUrl}" target="_blank" rel="noopener noreferrer" class="text-primary hover:underline">${url}</a>"`;
            }
            return match;
        });
    }

    // Check if a string is a URL
    function isUrl(str: string): boolean {
        try {
            new URL(str);
            return true;
        } catch {
            return false;
        }
    }
</script>

<div class="container mx-auto px-4 py-8">
    <div class="flex justify-between items-center mb-6">
        <h1 class="text-3xl font-bold">Governance Anchor Metadata</h1>
        <a href="#raw-json" class="btn btn-ghost">View Raw JSON</a>
    </div>

    {#if error}
        <div class="alert alert-error">
            <span>{error}</span>
        </div>
    {:else if anchorMetadata}
        <div class="space-y-6">
            <!-- Basic Information -->
            <div class="card bg-base-100 shadow-md">
                <div class="card-body">
                    <h2 class="card-title text-2xl mb-4">{sanitizeText(anchorMetadata.body?.title || 'Untitled')}</h2>
                    
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                        {#if anchorMetadata.authors}
                            <div>
                                <h3 class="font-semibold mb-2">Authors</h3>
                                <p>{formatAuthors(anchorMetadata.authors)}</p>
                            </div>
                        {/if}

                        <div class="space-y-2">
                            {#if anchorMetadata.created}
                                <p><span class="font-semibold">Created:</span> {formatDate(anchorMetadata.created)}</p>
                            {/if}
                            {#if anchorMetadata['@language']}
                                <p><span class="font-semibold">Language:</span> {sanitizeText(anchorMetadata['@language'])}</p>
                            {/if}
                        </div>
                    </div>

                    {#if anchorMetadata.abstract}
                        <div class="mt-4">
                            <h3 class="font-semibold mb-2">Abstract</h3>
                            <p class="text-gray-700">{sanitizeText(anchorMetadata.abstract)}</p>
                        </div>
                    {/if}
                </div>
            </div>

            <!-- Main Content -->
            <div class="grid grid-cols-1 gap-6">
                {#if anchorMetadata.motivation}
                    <div class="card bg-base-100 shadow-md">
                        <div class="card-body">
                            <h3 class="card-title">Motivation</h3>
                            <div class="prose max-w-none">
                                <p>{sanitizeText(anchorMetadata.motivation)}</p>
                            </div>
                        </div>
                    </div>
                {/if}

                {#if anchorMetadata.rationale}
                    <div class="card bg-base-100 shadow-md">
                        <div class="card-body">
                            <h3 class="card-title">Rationale</h3>
                            <div class="prose max-w-none">
                                <p>{sanitizeText(anchorMetadata.rationale)}</p>
                            </div>
                        </div>
                    </div>
                {/if}

                {#if anchorMetadata.body}
                    <div class="card bg-base-100 shadow-md">
                        <div class="card-body">
                            <h3 class="card-title">Body</h3>
                            <div class="prose max-w-none space-y-4">
                                <!-- Handle schema-defined properties -->
                                {#if anchorMetadata.body.title}
                                    <div>
                                        <h4 class="text-xl font-semibold">{sanitizeText(anchorMetadata.body.title)}</h4>
                                    </div>
                                {/if}

                                {#if anchorMetadata.body.abstract}
                                    <div>
                                        <h5 class="font-semibold mb-2">Abstract</h5>
                                        <p>{sanitizeText(anchorMetadata.body.abstract)}</p>
                                    </div>
                                {/if}

                                {#if anchorMetadata.body.motivation}
                                    <div>
                                        <h5 class="font-semibold mb-2">Motivation</h5>
                                        <p>{sanitizeText(anchorMetadata.body.motivation)}</p>
                                    </div>
                                {/if}

                                {#if anchorMetadata.body.rationale}
                                    <div>
                                        <h5 class="font-semibold mb-2">Rationale</h5>
                                        <p class="whitespace-pre-line">{sanitizeText(anchorMetadata.body.rationale)}</p>
                                    </div>
                                {/if}

                                {#if anchorMetadata.body.comment}
                                    <div>
                                        <h5 class="font-semibold mb-2">Comment</h5>
                                        <p>{sanitizeText(anchorMetadata.body.comment)}</p>
                                    </div>
                                {/if}

                                {#if anchorMetadata.body.dRepName}
                                    <div>
                                        <h5 class="font-semibold mb-2">DRep Name</h5>
                                        <p>{sanitizeText(anchorMetadata.body.dRepName['@value'] || anchorMetadata.body.dRepName)}</p>
                                    </div>
                                {/if}

                                {#if anchorMetadata.body.email}
                                    <div>
                                        <h5 class="font-semibold mb-2">Email</h5>
                                        <p>{sanitizeText(anchorMetadata.body.email['@value'] || anchorMetadata.body.email)}</p>
                                    </div>
                                {/if}

                                {#if anchorMetadata.body.bio}
                                    <div>
                                        <h5 class="font-semibold mb-2">Bio</h5>
                                        <p>{sanitizeText(anchorMetadata.body.bio['@value'] || anchorMetadata.body.bio)}</p>
                                    </div>
                                {/if}

                                <!-- Handle references -->
                                {#if anchorMetadata.body.references && anchorMetadata.body.references.length > 0}
                                    <div>
                                        <h5 class="font-semibold mb-2">References</h5>
                                        <ul class="list-disc list-inside space-y-2">
                                            {#each anchorMetadata.body.references as ref}
                                                {#if ref.uri}
                                                    {@const sanitizedUrl = sanitizeUrl(ref.uri)}
                                                    {#if sanitizedUrl}
                                                        <li class="flex items-center gap-2">
                                                            <span>{sanitizeText(ref.label || ref.title || ref.uri)}</span>
                                                            <a 
                                                                href={sanitizedUrl} 
                                                                target="_blank" 
                                                                rel="noopener noreferrer" 
                                                                class="btn btn-ghost btn-xs"
                                                            >
                                                                <ExternalLinkIcon size="12" />
                                                            </a>
                                                        </li>
                                                    {/if}
                                                {/if}
                                            {/each}
                                        </ul>
                                    </div>
                                {/if}

                                <!-- Handle external updates -->
                                {#if anchorMetadata.body.externalUpdates && anchorMetadata.body.externalUpdates.length > 0}
                                    <div>
                                        <h5 class="font-semibold mb-2">External Updates</h5>
                                        <ul class="list-disc list-inside space-y-2">
                                            {#each anchorMetadata.body.externalUpdates as update}
                                                {#if update.uri}
                                                    {@const sanitizedUrl = sanitizeUrl(update.uri)}
                                                    {#if sanitizedUrl}
                                                        <li class="flex items-center gap-2">
                                                            <span>{sanitizeText(update.title || update.uri)}</span>
                                                            <a 
                                                                href={sanitizedUrl} 
                                                                target="_blank" 
                                                                rel="noopener noreferrer" 
                                                                class="btn btn-ghost btn-xs"
                                                            >
                                                                <ExternalLinkIcon size="12" />
                                                            </a>
                                                        </li>
                                                    {/if}
                                                {/if}
                                            {/each}
                                        </ul>
                                    </div>
                                {/if}

                                <!-- Handle any additional dynamic properties -->
                                {#each Object.entries(anchorMetadata.body) as [key, value]}
                                    {#if !['title', 'abstract', 'motivation', 'rationale', 'comment', 'dRepName', 'email', 'bio', 'references', 'externalUpdates'].includes(key)}
                                        <div>
                                            <h5 class="font-semibold mb-2">{sanitizeText(key.charAt(0).toUpperCase() + key.slice(1))}</h5>
                                            {#if typeof value === 'object' && value !== null}
                                                {#if Array.isArray(value)}
                                                    <ul class="list-disc list-inside space-y-2">
                                                        {#each value as item}
                                                            <li>
                                                                {#if typeof item === 'object' && item !== null}
                                                                    {JSON.stringify(sanitizeObject(item))}
                                                                {:else}
                                                                    {sanitizeText(String(item))}
                                                                {/if}
                                                            </li>
                                                        {/each}
                                                    </ul>
                                                {:else}
                                                    <pre class="bg-base-200 p-2 rounded">{JSON.stringify(sanitizeObject(value), null, 2)}</pre>
                                                {/if}
                                            {:else}
                                                <p>{sanitizeText(String(value))}</p>
                                            {/if}
                                        </div>
                                    {/if}
                                {/each}
                            </div>
                        </div>
                    </div>
                {/if}
            </div>

            <!-- Additional Information -->
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                {#if anchorMetadata.references && anchorMetadata.references.length > 0}
                    <div class="card bg-base-100 shadow-md">
                        <div class="card-body">
                            <h3 class="card-title">References</h3>
                            <ul class="list-disc list-inside space-y-2">
                                {#each anchorMetadata.references as ref}
                                    {#if ref.url}
                                        {@const sanitizedUrl = sanitizeUrl(ref.url)}
                                        {#if sanitizedUrl}
                                            <li class="flex items-center gap-2">
                                                <span>{sanitizeText(ref.title || ref.url)}</span>
                                                <a 
                                                    href={sanitizedUrl} 
                                                    target="_blank" 
                                                    rel="noopener noreferrer" 
                                                    class="btn btn-ghost btn-xs"
                                                >
                                                    <ExternalLinkIcon size="12" />
                                                </a>
                                            </li>
                                        {/if}
                                    {/if}
                                {/each}
                            </ul>
                        </div>
                    </div>
                {/if}

                {#if anchorMetadata.externalUpdates && anchorMetadata.externalUpdates.length > 0}
                    <div class="card bg-base-100 shadow-md">
                        <div class="card-body">
                            <h3 class="card-title">External Updates</h3>
                            <ul class="list-disc list-inside space-y-2">
                                {#each anchorMetadata.externalUpdates as update}
                                    {#if update.url}
                                        {@const sanitizedUrl = sanitizeUrl(update.url)}
                                        {#if sanitizedUrl}
                                            <li class="flex items-center gap-2">
                                                <span>{sanitizeText(update.description || update.url)}</span>
                                                <a 
                                                    href={sanitizedUrl} 
                                                    target="_blank" 
                                                    rel="noopener noreferrer" 
                                                    class="btn btn-ghost btn-xs"
                                                >
                                                    <ExternalLinkIcon size="12" />
                                                </a>
                                            </li>
                                        {/if}
                                    {/if}
                                {/each}
                            </ul>
                        </div>
                    </div>
                {/if}

                {#if anchorMetadata.discussions && anchorMetadata.discussions.length > 0}
                    <div class="card bg-base-100 shadow-md md:col-span-2">
                        <div class="card-body">
                            <h3 class="card-title">Discussions</h3>
                            <ul class="list-disc list-inside space-y-2">
                                {#each anchorMetadata.discussions as discussion}
                                    {#if discussion.url}
                                        {@const sanitizedUrl = sanitizeUrl(discussion.url)}
                                        {#if sanitizedUrl}
                                            <li class="flex items-center gap-2">
                                                <span>{sanitizeText(discussion.title || discussion.url)}</span>
                                                <a 
                                                    href={sanitizedUrl} 
                                                    target="_blank" 
                                                    rel="noopener noreferrer" 
                                                    class="btn btn-ghost btn-xs"
                                                >
                                                    <ExternalLinkIcon size="12" />
                                                </a>
                                            </li>
                                        {/if}
                                    {/if}
                                {/each}
                            </ul>
                        </div>
                    </div>
                {/if}
            </div>

            <!-- Raw JSON View -->
            <div id="raw-json" class="card bg-base-100 shadow-md">
                <div class="card-body">
                    <h3 class="card-title">Raw JSON-LD Data</h3>
                    <div class="mt-2 p-4 bg-base-200 rounded-lg overflow-x-auto text-sm">
                        <pre class="whitespace-pre-wrap font-mono">
                            {@html formatJsonForDisplay(anchorMetadata.rawJson)}
                        </pre>
                    </div>
                </div>
            </div>
        </div>
    {:else}
        <div class="flex justify-center items-center h-64">
            <span class="loading loading-spinner loading-lg"></span>
        </div>
    {/if}
</div>

<style>
    .container {
        max-width: 1400px;
    }
    
    pre {
        white-space: pre-wrap;
        word-wrap: break-word;
    }

    :global(.prose) {
        max-width: none;
    }

    /* JSON syntax highlighting */
    :global(pre) {
        color: #333;
    }

    :global(pre .string) {
        color: #0b7285;
    }

    :global(pre .number) {
        color: #2b8a3e;
    }

    :global(pre .boolean) {
        color: #2b8a3e;
    }

    :global(pre .null) {
        color: #868e96;
    }

    :global(pre .key) {
        color: #862e9c;
    }
</style> 