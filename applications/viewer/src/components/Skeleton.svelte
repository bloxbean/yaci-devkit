<script lang="ts">
  export let lines = 3;
  export let rounded = 'rounded-md';
  export let className = '';
</script>

<div class={`animate-pulse ${className}`}>
  <div class={`h-4 bg-base-200 ${rounded} mb-3 w-2/3`}></div>
  {#each Array(lines) as _, i}
    <div class={`h-3 bg-base-200 ${rounded} mb-2 ${i % 2 === 0 ? 'w-5/6' : 'w-4/6'}`}></div>
  {/each}
</div>

<style>
  .animate-pulse { animation: pulse 1.6s cubic-bezier(0.4, 0, 0.6, 1) infinite; }
  @keyframes pulse { 50% { opacity: .6; } }
</style>

