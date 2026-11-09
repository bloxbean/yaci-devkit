import { describe, expect, it } from 'vitest';
import { injectIntoHead, linkHeaderToTags } from './preloads';

const header =
	'<./_app/immutable/assets/0.css>; rel="preload"; as="style"; nopush, <./_app/immutable/entry/start.js>; rel="modulepreload"; nopush, <./fonts/a.woff2>; rel="preload"; as="font"; type="font/woff2"; crossorigin; nopush';

async function text(stream: ReadableStream<Uint8Array>) {
	return new Response(stream).text();
}
function streamOf(...chunks: string[]) {
	const enc = new TextEncoder();
	return new ReadableStream<Uint8Array>({
		start(c) {
			for (const ch of chunks) c.enqueue(enc.encode(ch));
			c.close();
		}
	});
}

describe('preloads', () => {
	it('turns SvelteKit’s Link header into <link> tags', () => {
		expect(linkHeaderToTags(header)).toBe(
			'<link href="./_app/immutable/assets/0.css" rel="preload" as="style">' +
				'<link href="./_app/immutable/entry/start.js" rel="modulepreload">' +
				'<link href="./fonts/a.woff2" rel="preload" as="font" type="font/woff2" crossorigin>'
		);
		expect(linkHeaderToTags('')).toBe('');
	});

	it('inserts the tags before </head>, also when the tag is split across chunks', async () => {
		expect(await text(injectIntoHead(streamOf('<html><head><title>x</title></head><body>hi</body></html>'), '<link>'))).toBe('<html><head><title>x</title><link></head><body>hi</body></html>');
		expect(await text(injectIntoHead(streamOf('<html><head>', '<title>x</title></he', 'ad><body>', 'streamed</body>'), '<link>'))).toBe('<html><head><title>x</title><link></head><body>streamed</body>');
		expect(await text(injectIntoHead(streamOf('no head here'), '<link>'))).toBe('no head here');
	});
});
