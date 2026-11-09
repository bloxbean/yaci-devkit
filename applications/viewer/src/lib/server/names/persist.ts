import { mkdir, readFile, rename, writeFile } from 'node:fs/promises';
import { join } from 'node:path';

/*
 * The names are saved as JSON files (one per kind), so a restart has them at once and only refreshes what changed.
 * Writes go to a temporary file first and are renamed into place. Where the folder can't be written (a read-only
 * deployment), the names stay in memory only: one warning, then no more tries.
 */

let readOnly = false;

export async function loadJson<T>(dir: string, name: string): Promise<T | null> {
	try {
		return JSON.parse(await readFile(join(dir, `${name}.json`), 'utf8')) as T;
	} catch {
		return null;
	}
}

export async function saveJson(dir: string, name: string, value: unknown): Promise<void> {
	if (readOnly) return;
	const file = join(dir, `${name}.json`);
	try {
		await mkdir(dir, { recursive: true });
		await writeFile(`${file}.tmp`, JSON.stringify(value));
		await rename(`${file}.tmp`, file);
	} catch (e) {
		readOnly = true;
		console.warn(`[names] can't save to ${dir} (${(e as Error).message}); names are kept in memory only. Set NAMES_DIR to a writable folder.`);
	}
}
