/*
 * Minimal reader for the flat tar archives julc releases ship. Only regular files at the top level are returned;
 * directories, links, pax headers and anything with a nested or unusual name are skipped, so an archive can't
 * write outside the target folder.
 */
const SAFE_NAME = /^[A-Za-z0-9][A-Za-z0-9._-]*$/;

function field(block, start, length) {
	const end = block.indexOf(0, start);
	return block.toString('utf8', start, end === -1 || end > start + length ? start + length : end);
}

/** @param {Buffer} tar  @returns {Map<string, Buffer>} */
export function untar(tar) {
	const files = new Map();
	let offset = 0;
	while (offset + 512 <= tar.length) {
		const header = tar.subarray(offset, offset + 512);
		if (header.every((b) => b === 0)) break;
		const size = parseInt(field(header, 124, 12).trim() || '0', 8);
		if (!Number.isFinite(size) || size < 0) throw new Error('corrupt tar header');
		const type = String.fromCharCode(header[156] || 48);
		const prefix = field(header, 345, 155);
		const name = (prefix ? `${prefix}/` : '') + field(header, 0, 100);
		const body = tar.subarray(offset + 512, offset + 512 + size);
		if (body.length !== size) throw new Error('truncated tar archive');
		const base = name.replace(/^\.\//, '');
		if (type === '0' && SAFE_NAME.test(base)) files.set(base, Buffer.from(body));
		offset += 512 + Math.ceil(size / 512) * 512;
	}
	return files;
}
