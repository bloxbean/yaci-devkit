/* Axis and tooltip labels for the analytics charts. */

/** "2026-10-17" → "17 Oct". */
export const dayLabel = (iso: string) => new Date(`${iso}T00:00:00Z`).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', timeZone: 'UTC' });

/** 662 → "Epoch 662". */
export const epochLabel = (epoch: number) => `Epoch ${epoch}`;
