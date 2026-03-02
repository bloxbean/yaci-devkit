<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { health, startHealthPolling, stopHealthPolling, checkHealth } from '$lib/stores/health';
  let state = { status: 'unknown' } as any;
  let unsub: () => void;
  onMount(() => {
    unsub = health.subscribe((v) => (state = v));
    startHealthPolling(15000);
  });
  onDestroy(() => { unsub && unsub(); stopHealthPolling(); });
  const color = () => state.status === 'ok' ? 'bg-success' : state.status === 'down' ? 'bg-error' : 'bg-warning';
  const tip = () => {
    if (state.status === 'ok') return `Indexer healthy (${state.latencyMs ?? '?'} ms)`;
    if (state.status === 'down') return `Indexer unreachable${state.error ? `: ${state.error}` : ''}`;
    return 'Checking indexer…';
  };
</script>

<div class="tooltip" data-tip={tip()}>
  <button class="btn btn-ghost btn-sm flex items-center gap-2" on:click={() => checkHealth()}>
    <span class={`inline-block w-2.5 h-2.5 rounded-full ${color()}`}></span>
    <span class="hidden md:inline text-sm">Indexer</span>
  </button>
  
</div>

