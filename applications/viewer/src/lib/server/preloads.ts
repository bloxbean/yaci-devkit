/*
 * Turn SvelteKit's `Link` preload header into <link> tags for the page's <head> (see hooks.server.ts):
 *   <./_app/a.js>; rel="modulepreload"; nopush, <./_app/b.css>; rel="preload"; as="style"; nopush
 * becomes <link href="./_app/a.js" rel="modulepreload"><link href="./_app/b.css" rel="preload" as="style">.
 */

const escape = (s: string) => s.replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;');

/** The header's entries as tags (one per entry, `nopush` dropped); empty string when there are none. */
export function linkHeaderToTags(header: string): string {
	return header
		.split(/,\s*(?=<)/)
		.map((entry) => {
			const m = entry.trim().match(/^<([^>]*)>\s*(.*)$/);
			if (!m) return '';
			const attrs = [`href="${escape(m[1])}"`];
			for (const part of m[2].split(';')) {
				const p = part.trim();
				if (!p || p === 'nopush') continue;
				const kv = p.match(/^([a-z-]+)\s*=\s*"?([^"]*)"?$/i);
				attrs.push(kv ? `${kv[1].toLowerCase()}="${escape(kv[2])}"` : escape(p.toLowerCase()));
			}
			return `<link ${attrs.join(' ')}>`;
		})
		.filter(Boolean)
		.join('');
}

/** Insert `tags` right before the first </head> of a streamed HTML body, passing everything else through as it comes. */
export function injectIntoHead(body: ReadableStream<Uint8Array>, tags: string): ReadableStream<Uint8Array> {
	let done = false;
	let pending = '';
	const inject = new TransformStream<string, string>({
		transform(chunk, controller) {
			if (done) return controller.enqueue(chunk);
			pending += chunk;
			const at = pending.indexOf('</head>');
			if (at >= 0) {
				controller.enqueue(pending.slice(0, at) + tags + pending.slice(at));
				pending = '';
				done = true;
			} else if (pending.length > 7) {
				// Keep a tail that might be the start of a split "</head>".
				controller.enqueue(pending.slice(0, -7));
				pending = pending.slice(-7);
			}
		},
		flush(controller) {
			if (pending) controller.enqueue(pending);
		}
	});
	return body.pipeThrough(new TextDecoderStream()).pipeThrough(inject).pipeThrough(new TextEncoderStream());
}
