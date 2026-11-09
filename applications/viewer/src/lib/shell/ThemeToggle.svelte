<script lang="ts">
	import Moon from '@lucide/svelte/icons/moon';
	import Sun from '@lucide/svelte/icons/sun';
	import { theme, toggleTheme } from '$lib/stores/theme';
</script>

<button class="y-icon-btn" onclick={toggleTheme} aria-label="Switch to {$theme === 'yaci-dark' ? 'light' : 'dark'} theme" title="Toggle theme">
	{#if $theme === 'yaci-dark'}<Sun class="h-4 w-4" />{:else}<Moon class="h-4 w-4" />{/if}
</button>
