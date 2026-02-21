import { writable } from 'svelte/store';

export type ToastType = 'success' | 'info' | 'warning' | 'error';

export interface ToastItem {
  id: number;
  message: string;
  type: ToastType;
}

export const toasts = writable<ToastItem[]>([]);

let idCounter = 1;

export function showToast(message: string, type: ToastType = 'success', timeout = 2000) {
  const id = idCounter++;
  const item: ToastItem = { id, message, type };
  toasts.update((list) => [...list, item]);
  const timer = setTimeout(() => {
    toasts.update((list) => list.filter((t) => t.id !== id));
    clearTimeout(timer);
  }, timeout);
}

