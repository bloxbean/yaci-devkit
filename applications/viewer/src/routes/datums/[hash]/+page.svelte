<script>
	import TruncateCopy from '../../../components/TruncateCopy.svelte';
	import JsonContent from '../../../components/JsonContent.svelte';

	export let data;
	let { hash, datumJson, datumCbor } = data;

	const JSON_TAB = 0;
	const CBOR_TAB = 1;

	let activeTabIndex = JSON_TAB;
	let cborCopied = false;

	const copyCbor = () => {
		const text = datumCbor?.cbor || '';
		navigator.clipboard.writeText(text);
		cborCopied = true;
		setTimeout(() => (cborCopied = false), 2000);
	};
</script>

<section class="py-12 px-4 md:px-6 min-h-screen">
	<div class="container mx-auto max-w-6xl">
		<!-- Header -->
		<div class="bg-base-100 rounded-xl shadow-sm border border-base-300 overflow-hidden mb-8">
			<div class="bg-base-200/50 p-6 border-b border-base-200">
				<h2 class="text-xs font-bold text-base-content/60 uppercase tracking-wider mb-2">
					Datum Hash
				</h2>
				<div class="flex items-center gap-3">
					<TruncateCopy text={hash} max={60} />
				</div>
			</div>
		</div>

		<!-- Tabs -->
		<div class="mb-6 bg-base-200 rounded-xl p-2">
			<div class="flex flex-wrap gap-2 border-b border-base-300">
				<button
					class="px-5 py-3 text-sm font-medium transition-colors duration-200 border-b-2 {activeTabIndex ===
					JSON_TAB
						? 'border-blue-600 text-blue-600'
						: 'border-transparent text-base-content/60 hover:text-base-content/80 hover:border-gray-300'}"
					on:click={() => (activeTabIndex = JSON_TAB)}
				>
					JSON
				</button>

				<button
					class="px-5 py-3 text-sm font-medium transition-colors duration-200 border-b-2 {activeTabIndex ===
					CBOR_TAB
						? 'border-blue-600 text-blue-600'
						: 'border-transparent text-base-content/60 hover:text-base-content/80 hover:border-gray-300'}"
					on:click={() => (activeTabIndex = CBOR_TAB)}
				>
					CBOR
				</button>
			</div>
		</div>

		<!-- Tab Content -->
		<div class="bg-base-100 rounded-xl shadow-sm border border-base-300 p-6 min-h-[300px]">
			{#if activeTabIndex === JSON_TAB}
				<div class="w-full animate-fade-in">
					{#if datumJson}
						<JsonContent text={datumJson} customStyle="bg-black text-gray-100" />
					{:else}
						<div class="text-center py-10 text-base-content/50">No JSON representation available.</div>
					{/if}
				</div>
			{/if}

			{#if activeTabIndex === CBOR_TAB}
				<div class="w-full animate-fade-in">
					{#if datumCbor && datumCbor.cbor}
						<div class="flex justify-end mb-2">
							<button
								class="btn btn-sm btn-outline"
								on:click={copyCbor}
							>
								{cborCopied ? 'Copied!' : 'Copy CBOR'}
							</button>
						</div>
						<div class="bg-gray-900 text-gray-100 p-4 rounded-lg overflow-x-auto">
							<pre class="text-sm font-mono whitespace-pre-wrap break-all">{datumCbor.cbor}</pre>
						</div>
					{:else}
						<div class="text-center py-10 text-base-content/50">No CBOR representation available.</div>
					{/if}
				</div>
			{/if}
		</div>
	</div>
</section>

<style>
	.animate-fade-in {
		animation: fadeIn 0.3s ease-in-out;
	}
	@keyframes fadeIn {
		from {
			opacity: 0;
			transform: translateY(5px);
		}
		to {
			opacity: 1;
			transform: translateY(0);
		}
	}
</style>
