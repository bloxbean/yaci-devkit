<script lang="ts">
  export let title = 'No data yet';
  export let message = 'New items will appear here as they arrive.';
</script>

<div class="bg-base-100 border border-base-300 rounded-lg p-6 text-center">
  <div class="mb-2 font-semibold">{title}</div>
  <div class="text-sm text-base-content/70">{message}</div>
  <slot />
</div>

