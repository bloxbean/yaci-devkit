export interface Toast {
	id: number;
	text: string;
	tone: 'ok' | 'error' | 'info';
}

let next = 1;

class Toasts {
	items = $state<Toast[]>([]);

	show(text: string, tone: Toast['tone'] = 'ok', ms = 1800) {
		const id = next++;
		this.items = [...this.items, { id, text, tone }].slice(-3);
		setTimeout(() => (this.items = this.items.filter((t) => t.id !== id)), ms);
	}
}

export const toasts = new Toasts();

export async function copyText(text: string, label = 'Copied') {
	try {
		await navigator.clipboard.writeText(text);
		toasts.show(label);
		return true;
	} catch {
		toasts.show('Copy failed', 'error');
		return false;
	}
}
