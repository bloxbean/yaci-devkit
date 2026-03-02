import { writable } from 'svelte/store';

export type Theme = 'yaci' | 'yaci-dark';

const STORAGE_KEY = 'viewer:theme';

function detectInitial(): Theme {
  if (typeof window === 'undefined') return 'yaci';
  try {
    const saved = window.localStorage.getItem(STORAGE_KEY) as Theme | null;
    if (saved === 'yaci' || saved === 'yaci-dark') return saved;
    const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    return prefersDark ? 'yaci-dark' : 'yaci';
  } catch {
    return 'yaci';
  }
}

export const theme = writable<Theme>(detectInitial());

// Apply to <html data-theme>
if (typeof window !== 'undefined') {
  const apply = (t: Theme) => {
    document.documentElement.setAttribute('data-theme', t);
    try { window.localStorage.setItem(STORAGE_KEY, t); } catch {}
  };
  // initialize
  apply(detectInitial());
  // subscribe updates
  theme.subscribe(apply);
}

export function toggleTheme() {
  theme.update((t) => (t === 'yaci' ? 'yaci-dark' : 'yaci'));
}

