<script lang="ts">
  export let title = 'Something went wrong';
  export let message = 'Please try again.';
  export let actionLabel = 'Retry';
  export let onAction: (() => void) | null = null;
</script>

<div class="bg-base-100 border border-base-300 rounded-lg p-6 text-center">
  <div class="mb-2 text-error font-semibold">{title}</div>
  <div class="text-sm text-base-content/70 mb-4">{message}</div>
  {#if onAction}
    <button class="btn btn-sm" on:click={onAction}>{actionLabel}</button>
  {/if}
  <slot />
</div>

