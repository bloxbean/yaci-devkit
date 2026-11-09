import { browser } from '$app/environment';
import { findValidator, parseBlueprint, type Blueprint } from './blueprint';

/*
 * Blueprints the user attached, kept in this browser (localStorage). Datums and redeemers of matching scripts are
 * shown with type, variant and field names everywhere in the viewer.
 */
const KEY = 'viewer:blueprints';

class Blueprints {
	list = $state.raw<Blueprint[]>([]);

	constructor() {
		if (!browser) return;
		try {
			const saved = JSON.parse(localStorage.getItem(KEY) ?? '[]');
			if (Array.isArray(saved)) this.list = saved;
		} catch {
			/* nothing saved, or storage is blocked */
		}
	}

	#save() {
		try {
			localStorage.setItem(KEY, JSON.stringify(this.list));
		} catch {
			/* storage full or blocked: kept for this page only */
		}
	}

	/** Parse and keep a blueprint (replacing one with the same title). Throws with a readable message if invalid. */
	add(text: string): Blueprint {
		let json: unknown;
		try {
			json = JSON.parse(text);
		} catch {
			throw new Error('That file is not JSON.');
		}
		const bp = parseBlueprint(json);
		this.list = [...this.list.filter((b) => b.title !== bp.title), bp];
		this.#save();
		return bp;
	}

	remove(title: string) {
		this.list = this.list.filter((b) => b.title !== title);
		this.#save();
	}

	find(hash: string, purpose?: string | null) {
		return findValidator(this.list, hash, purpose);
	}
}

export const blueprints = new Blueprints();
