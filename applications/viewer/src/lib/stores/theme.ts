import { writable } from 'svelte/store';

export type Theme = 'yaci' | 'yaci-dark';

const STORAGE_KEY = 'viewer:theme';

/* The theme app.html already applied before first paint: saved choice, else the OS preference. */
function current(): Theme {
	if (typeof document === 'undefined') return 'yaci';
	return document.documentElement.getAttribute('data-theme') === 'yaci-dark' ? 'yaci-dark' : 'yaci';
}

export const theme = writable<Theme>(current());

/** Only an explicit toggle is saved, so people who never choose keep following their OS. */
export function toggleTheme() {
	theme.update((t) => {
		const next: Theme = t === 'yaci' ? 'yaci-dark' : 'yaci';
		document.documentElement.setAttribute('data-theme', next);
		try {
			localStorage.setItem(STORAGE_KEY, next);
		} catch {
			/* storage unavailable */
		}
		return next;
	});
}

// Follow OS changes until the user picks a theme.
if (typeof window !== 'undefined' && window.matchMedia) {
	window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
		try {
			if (localStorage.getItem(STORAGE_KEY)) return;
		} catch {
			/* storage unavailable */
		}
		const next: Theme = e.matches ? 'yaci-dark' : 'yaci';
		document.documentElement.setAttribute('data-theme', next);
		theme.set(next);
	});
}
