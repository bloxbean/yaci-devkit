import { sveltekit } from '@sveltejs/kit/vite';

// Not annotated: vitest bundles its own Vite, whose config type differs from the app's.
export default {
	plugins: [sveltekit()],
	optimizeDeps: {
		// Icons are imported one file each; pre-bundling them on demand reloads the dev page mid-hydration.
		exclude: ['@lucide/svelte']
	},
	test: {
		include: ['src/**/*.{test,spec}.{js,ts}']
	}
};
