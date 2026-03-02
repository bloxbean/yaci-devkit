<script lang="ts">
  export let text = '';
  export let max = 38;
  export let title = '';

  function truncate(full: string, len: number) {
    if (!full) return '';
    if (full.length <= len) return full;
    const sep = '…';
    const show = len - 1;
    const front = Math.ceil(show / 2);
    const back = Math.floor(show / 2);
    return full.slice(0, front) + sep + full.slice(full.length - back);
  }

  import { showToast } from '$lib/stores/toast';

  async function copy() {
    try {
      await navigator.clipboard.writeText(text);
      showToast('Copied to clipboard', 'success');
    } catch {
      showToast('Copy failed', 'error');
    }
  }
</script>

<div class="inline-flex items-center gap-1">
  <span class="truncate" title={title || text}>{truncate(text, max)}</span>
  <button class="btn btn-ghost btn-xs" aria-label="Copy" on:click={copy} title="Copy">
    <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" viewBox="0 0 24 24" fill="currentColor"><path d="M16 1H4c-1.1 0-2 .9-2 2v12h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"/></svg>
  </button>
</div>
