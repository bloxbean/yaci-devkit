<script lang="ts">
	import { navigating } from '$app/state';

	/* A thin gradient bar at the top while a page loads. It waits a moment so fast navigations don't flicker. */
	let visible = $state(false);
	let done = $state(false);
	let timer: ReturnType<typeof setTimeout> | undefined;

	$effect(() => {
		const busy = navigating.to !== null;
		clearTimeout(timer);
		if (busy) {
			done = false;
			timer = setTimeout(() => (visible = true), 120);
		} else if (visible) {
			done = true;
			timer = setTimeout(() => {
				visible = false;
				done = false;
			}, 320);
		}
	});
</script>

{#if visible}
	<div class="pointer-events-none fixed inset-x-0 top-0 z-[90] h-[3px] overflow-hidden" aria-hidden="true">
		<div class="bar h-full bg-grad shadow-glow {done ? 'done' : ''}"></div>
	</div>
{/if}

<style>
	.bar {
		width: 100%;
		transform-origin: left;
		animation: grow 8s cubic-bezier(0.1, 0.7, 0.2, 1) forwards;
	}
	.bar.done {
		animation: finish 0.3s ease-out forwards;
	}
	@keyframes grow {
		from {
			transform: scaleX(0.05);
		}
		to {
			transform: scaleX(0.92);
		}
	}
	@keyframes finish {
		to {
			transform: scaleX(1);
			opacity: 0;
		}
	}
</style>
