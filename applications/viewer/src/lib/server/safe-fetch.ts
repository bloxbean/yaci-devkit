import http from 'node:http';
import https from 'node:https';
import dns from 'node:dns';
import net, { type LookupFunction } from 'node:net';
import { checkPublicUrl, isBlockedIp } from '$lib/net/public-url';

/*
 * GET a URL that came from chain data or user input, without letting the viewer's server reach the local network.
 * - checkPublicUrl: scheme, port and host rules, private IP literals.
 * - safeLookup: every DNS answer is checked when the socket connects, so a hostname can't resolve (or re-resolve,
 *   DNS rebinding) to a private address between the check and the request.
 * - Redirects are followed by hand, each hop checked again.
 * - An overall deadline and a body size cap.
 */

export class SafeFetchError extends Error {}

export interface SafeGetOptions {
	maxBytes: number;
	timeoutMs: number;
	maxRedirects: number;
	accept?: string;
}

export interface SafeGetResult {
	url: URL;
	contentType: string;
	body: Uint8Array;
}

const safeLookup: LookupFunction = (hostname, options, callback) => {
	dns.lookup(hostname, { all: true, verbatim: true }, (err, list) => {
		if (err) return callback(err, '', 4);
		if (!list.length || list.some((a) => isBlockedIp(a.address))) {
			return callback(new SafeFetchError('That host resolves to a private or reserved address.'), '', 4);
		}
		if (options.all) return callback(null, list);
		callback(null, list[0].address, list[0].family);
	});
};

function concat(chunks: Uint8Array[]) {
	const out = new Uint8Array(chunks.reduce((n, c) => n + c.length, 0));
	let o = 0;
	for (const c of chunks) {
		out.set(c, o);
		o += c.length;
	}
	return out;
}

type Hop = { status: number; location?: string; contentType: string; body: Uint8Array };

function request(url: URL, deadline: number, opts: SafeGetOptions): Promise<Hop> {
	return new Promise((resolve, reject) => {
		const host = url.hostname.replace(/^\[|\]$/g, '');
		// IP literals skip DNS, so check them here too.
		if (net.isIP(host) && isBlockedIp(host)) return reject(new SafeFetchError('Private, loopback and reserved addresses are not allowed.'));
		const left = deadline - Date.now();
		if (left <= 0) return reject(new SafeFetchError('The host took too long to answer.'));
		const lib = url.protocol === 'https:' ? https : http;
		const req = lib.request(
			url,
			{
				method: 'GET',
				lookup: safeLookup,
				headers: { accept: opts.accept ?? 'application/json', 'user-agent': 'yaci-viewer' },
				timeout: left
			},
			(res) => {
				const status = res.statusCode ?? 0;
				if (status >= 300 && status < 400) {
					res.resume();
					return resolve({ status, location: String(res.headers.location ?? '') || undefined, contentType: '', body: new Uint8Array() });
				}
				const tooBig = () => new SafeFetchError(`The document is larger than ${Math.round(opts.maxBytes / 1024)} KB.`);
				if (Number(res.headers['content-length'] ?? 0) > opts.maxBytes) {
					res.destroy();
					return reject(tooBig());
				}
				const chunks: Uint8Array[] = [];
				let size = 0;
				res.on('data', (c: Uint8Array) => {
					size += c.length;
					if (size > opts.maxBytes) {
						res.destroy();
						reject(tooBig());
					} else chunks.push(c);
				});
				res.on('end', () => resolve({ status, contentType: String(res.headers['content-type'] ?? ''), body: concat(chunks) }));
				res.on('error', reject);
			}
		);
		// A socket that trickles data still stops at the deadline.
		const hardStop = setTimeout(() => req.destroy(new SafeFetchError('The host took too long to answer.')), left);
		req.on('close', () => clearTimeout(hardStop));
		req.on('timeout', () => req.destroy(new SafeFetchError('The host took too long to answer.')));
		req.on('error', reject);
		req.end();
	});
}

/** Fetch `start` (already checked by the caller or by checkPublicUrl) and follow safe redirects. */
export async function safeGet(start: URL, opts: SafeGetOptions): Promise<SafeGetResult> {
	const first = checkPublicUrl(start.href);
	if (!first.ok) throw new SafeFetchError(first.reason);
	const deadline = Date.now() + opts.timeoutMs;
	let url = first.url;
	for (let hop = 0; ; hop++) {
		let res: Hop;
		try {
			res = await request(url, deadline, opts);
		} catch (e) {
			if (e instanceof SafeFetchError) throw e;
			throw new SafeFetchError(`Could not fetch the document: ${(e as Error).message}`);
		}
		if (res.status >= 300 && res.status < 400) {
			if (!res.location || hop >= opts.maxRedirects) throw new SafeFetchError(opts.maxRedirects ? 'Too many redirects.' : 'The document is behind a redirect.');
			const next = checkPublicUrl(new URL(res.location, url).href);
			if (!next.ok) throw new SafeFetchError(`It redirects somewhere not allowed: ${next.reason}`);
			url = next.url;
			continue;
		}
		if (res.status < 200 || res.status >= 300) throw new SafeFetchError(`The host answered ${res.status}.`);
		return { url, contentType: res.contentType, body: res.body };
	}
}
