/*
 * The outcome of a section a page streams in after it renders (a slow Yaci Store call): its value, or why it's
 * missing. 'heavy': the store didn't answer in time, and the viewer won't ask again for a while (busy address).
 * 'busy': the viewer's slots for slow calls were all taken. 'error': anything else.
 */
export type Slow<T> = { ok: true; value: T } | { ok: false; reason: 'heavy' | 'busy' | 'error'; message: string };
