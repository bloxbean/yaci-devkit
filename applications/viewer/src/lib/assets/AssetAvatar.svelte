<script lang="ts">
	import { policyHue } from './types';

	/* A coloured tile with the asset's first letter; the colour is stable per policy. */
	let { policy, name, size = 56 }: { policy: string; name: string; size?: number } = $props();
</script>

<span
	class="grid shrink-0 place-items-center rounded-2xl font-display font-semibold text-white shadow-card"
	style="width:{size}px;height:{size}px;font-size:{size * 0.42}px;background:linear-gradient(135deg, hsl({policyHue(policy)} 70% 52%), hsl({(policyHue(policy) + 40) % 360} 70% 42%))"
	aria-hidden="true">{(name || '?').slice(0, 1).toUpperCase()}</span
>
