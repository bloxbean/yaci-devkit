/*
 * The current value of a load result that may be streamed (a promise) or already there: undefined while it streams.
 * Follows the source when the page's data changes, ignoring promises that settle after they were replaced.
 */
export function resolved<T>(source: () => T | Promise<T>): { readonly value: T | undefined } {
	const isPromise = (v: unknown): v is Promise<T> => typeof (v as Promise<T>)?.then === 'function';
	const initial = source();
	let value = $state<T | undefined>(isPromise(initial) ? undefined : initial);
	$effect(() => {
		const s = source();
		if (!isPromise(s)) {
			value = s;
			return;
		}
		value = undefined;
		let current = true;
		s.then((v) => current && (value = v));
		return () => {
			current = false;
		};
	});
	return {
		get value() {
			return value;
		}
	};
}
