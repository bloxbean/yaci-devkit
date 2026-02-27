import { writable } from 'svelte/store';

// 'auto' = not yet probed, 'balance' = /balance works, 'amounts' = fallback to /amounts
export const balanceApiMode = writable<'auto' | 'balance' | 'amounts'>('auto');
