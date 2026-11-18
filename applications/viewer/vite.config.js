import { sveltekit } from '@sveltejs/kit/vite';

// Not annotated: vitest bundles its own Vite, whose config type differs from the app's.
export default {
	plugins: [sveltekit()],
	optimizeDeps: {
		// Icons are imported one file each; pre-bundling them on demand reloads the dev page mid-hydration.
		// The julc engine names its worker and engine files relative to its own modules (new URL(…, import.meta.url)),
		// which pre-bundling moves: the dev server then can't find the worker. Production builds need nothing.
		exclude: ['@lucide/svelte', '@julclang/wasm-client', '@julclang/julc-wasm']
	},
	test: {
		include: ['src/**/*.{test,spec}.{js,ts}', 'scripts/**/*.test.ts']
	}
};
