<script>
	import { lovelaceToAda } from '$lib/util';
	import InputOutput from '../../../components/inputoutput/InputOutput.svelte';
	import TxnContext from '../../../components/TxnContext.svelte';
	import Inputs from '../../../components/inputoutput/Inputs.svelte';
	import Contract from '../../../components/Contract.svelte';
	import JsonContent from '../../../components/JsonContent.svelte';
	import AddressLink from '../../../components/AddressLink.svelte';
	import EmptyState from '../../../components/EmptyState.svelte';
	import { parseUnit } from '$lib/utils/asset';
	import { truncate } from '$lib/util';

	export let data;
	let { tx, contracts, metadata, withdrawals, mints } = data;

	const INPUT_TAB = 0;
	const CONTRACT_TAB = 1;
	const COLLATERAL_TAB = 2;
	const METADATA_TAB = 3;
	const REFERENCE_INPUT_TAB = 4;
	const WITHDRAWALS_TAB = 5;
	const MINTS_TAB = 6;
	const JSON_TAB = 7;

	let activeTabIndex = INPUT_TAB;
	let copied = false;

	const copyToClipboard = (text) => {
		navigator.clipboard.writeText(text);
		copied = true;
		setTimeout(() => (copied = false), 2000);
	};

	const iconCopy = `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg>`;
	const iconCheck = `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="text-green-500"><polyline points="20 6 9 17 4 12"></polyline></svg>`;
</script>

<section class="py-12 px-4 md:px-6 min-h-screen">
	<div class="container mx-auto max-w-6xl">
		<div class="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden mb-8">
			<div class="bg-gray-50/50 p-6 border-b border-gray-100">
				<h2 class="text-xs font-bold text-gray-500 uppercase tracking-wider mb-2">
					Transaction Hash
				</h2>
				<div class="flex items-center gap-3">
					<span class="font-mono text-lg text-gray-800 break-all">{tx.hash}</span>
					<button
						class="p-2 hover:bg-gray-200 rounded-md transition-colors duration-200"
						on:click={() => copyToClipboard(tx.hash)}
						title="Copy Hash"
					>
						{@html copied ? iconCheck : iconCopy}
					</button>
				</div>
			</div>

			<div
				class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 divide-y md:divide-y-0 md:divide-x divide-gray-100"
			>
				<div class="p-6">
					<div class="text-xs font-bold text-gray-400 uppercase tracking-wider mb-1">
						Block / Slot
					</div>
					<div class="text-gray-900 font-medium text-lg">
						<a
							href="/blocks/{tx.block_height}"
							class="text-blue-600 hover:text-blue-800 hover:underline"
						>
							{tx.block_height}
						</a>
						<span class="text-gray-400 mx-1">/</span>
						<span>{tx.slot}</span>
					</div>
				</div>

				<div class="p-6">
					<div class="text-xs font-bold text-gray-400 uppercase tracking-wider mb-1">Fee</div>
					<div class="text-gray-900 font-medium text-lg">
						{lovelaceToAda(tx.fees, 4)} <span class="text-sm text-gray-500">ADA</span>
					</div>
				</div>

				<div class="p-6">
					<div class="text-xs font-bold text-gray-400 uppercase tracking-wider mb-1">
						Total Output
					</div>
					<div class="text-gray-900 font-medium text-lg">
						{lovelaceToAda(tx.total_output, 2)} <span class="text-sm text-gray-500">ADA</span>
					</div>
				</div>

				<div class="p-6">
					<div class="text-xs font-bold text-gray-400 uppercase tracking-wider mb-1">TTL</div>
					<div class="text-gray-900 font-medium text-lg">{tx.ttl}</div>
				</div>
			</div>
		</div>

		<div class="mb-6 bg-gray-50 rounded-xl p-2">
			<div class="flex flex-wrap gap-2 border-b border-gray-200">
				<button
					class="px-5 py-3 text-sm font-medium transition-colors duration-200 border-b-2 {activeTabIndex ===
					INPUT_TAB
						? 'border-blue-600 text-blue-600'
						: 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'}"
					on:click={() => (activeTabIndex = INPUT_TAB)}
				>
					UTXOs (I/O)
				</button>

				<button
					class="px-5 py-3 text-sm font-medium transition-colors duration-200 border-b-2 {activeTabIndex ===
					CONTRACT_TAB
						? 'border-blue-600 text-blue-600'
						: 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'}"
					on:click={() => (activeTabIndex = CONTRACT_TAB)}
				>
					Contracts
					{#if contracts.length > 0}
						<span class="ml-1 bg-gray-100 text-gray-600 py-0.5 px-2 rounded-full text-xs"
							>{contracts.length}</span
						>
					{/if}
				</button>

				<button
					class="px-5 py-3 text-sm font-medium transition-colors duration-200 border-b-2 {activeTabIndex ===
					COLLATERAL_TAB
						? 'border-blue-600 text-blue-600'
						: 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'}"
					on:click={() => (activeTabIndex = COLLATERAL_TAB)}
				>
					Collaterals
				</button>

				<button
					class="px-5 py-3 text-sm font-medium transition-colors duration-200 border-b-2 {activeTabIndex ===
					METADATA_TAB
						? 'border-blue-600 text-blue-600'
						: 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'}"
					on:click={() => (activeTabIndex = METADATA_TAB)}
				>
					Metadata
				</button>

				<button
					class="px-5 py-3 text-sm font-medium transition-colors duration-200 border-b-2 {activeTabIndex ===
					REFERENCE_INPUT_TAB
						? 'border-blue-600 text-blue-600'
						: 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'}"
					on:click={() => (activeTabIndex = REFERENCE_INPUT_TAB)}
				>
					Reference Inputs
				</button>

				{#if withdrawals && withdrawals.length > 0}
					<button
						class="px-5 py-3 text-sm font-medium transition-colors duration-200 border-b-2 {activeTabIndex ===
						WITHDRAWALS_TAB
							? 'border-blue-600 text-blue-600'
							: 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'}"
						on:click={() => (activeTabIndex = WITHDRAWALS_TAB)}
					>
						Withdrawals
						<span class="ml-1 bg-gray-100 text-gray-600 py-0.5 px-2 rounded-full text-xs"
							>{withdrawals.length}</span
						>
					</button>
				{/if}

				{#if mints && mints.length > 0}
					<button
						class="px-5 py-3 text-sm font-medium transition-colors duration-200 border-b-2 {activeTabIndex ===
						MINTS_TAB
							? 'border-blue-600 text-blue-600'
							: 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'}"
						on:click={() => (activeTabIndex = MINTS_TAB)}
					>
						Mints
						<span class="ml-1 bg-gray-100 text-gray-600 py-0.5 px-2 rounded-full text-xs"
							>{mints.length}</span
						>
					</button>
				{/if}

				<button
					class="ml-auto px-5 py-3 text-sm font-medium transition-colors duration-200 border-b-2 {activeTabIndex ===
					JSON_TAB
						? 'border-blue-600 text-blue-600'
						: 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'}"
					on:click={() => (activeTabIndex = JSON_TAB)}
				>
					Raw JSON
				</button>
			</div>
		</div>

		<div class="bg-white rounded-xl shadow-sm border border-gray-200 p-6 min-h-[300px]">
			{#if activeTabIndex == INPUT_TAB}
				<div class="w-full animate-fade-in">
					<InputOutput {tx}></InputOutput>
				</div>
			{/if}

			{#if activeTabIndex == CONTRACT_TAB}
				<div class="w-full animate-fade-in">
					{#if contracts.length === 0}
						<div class="text-center py-10 text-gray-400">
							No contracts found in this transaction.
						</div>
					{:else}
						{#each contracts as contract, index}
							<div class="mb-8 last:mb-0">
								{#if index > 0}<div class="border-b border-gray-100 mb-6"></div>{/if}
								<Contract {contract} {index}></Contract>
							</div>
						{/each}
					{/if}
				</div>
			{/if}

			{#if activeTabIndex == COLLATERAL_TAB}
				<div class="w-full animate-fade-in">
					<Inputs inputs={tx.collateral_inputs}></Inputs>
				</div>
			{/if}

			{#if activeTabIndex == METADATA_TAB}
				<div class="w-full animate-fade-in">
					{#if !metadata}
						<div class="text-center py-10 text-gray-400">No metadata available.</div>
					{:else}
						<JsonContent text={metadata} customStyle="bg-black text-gray-100"></JsonContent>
					{/if}
				</div>
			{/if}

			{#if activeTabIndex == REFERENCE_INPUT_TAB}
				<div class="w-full animate-fade-in">
					<Inputs inputs={tx.reference_inputs}></Inputs>
				</div>
			{/if}

			{#if activeTabIndex == WITHDRAWALS_TAB}
				<div class="w-full animate-fade-in">
					{#if !withdrawals || withdrawals.length === 0}
						<EmptyState title="No Withdrawals" message="No withdrawals found in this transaction." />
					{:else}
						<div class="overflow-x-auto">
							<table class="min-w-full divide-y divide-gray-200">
								<thead class="bg-gray-50">
									<tr>
										<th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Address</th>
										<th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Amount</th>
									</tr>
								</thead>
								<tbody class="bg-white divide-y divide-gray-200">
									{#each withdrawals as w}
										<tr class="hover:bg-gray-50">
											<td class="px-4 py-4 text-sm">
												<AddressLink address={w.address} maxLength={40} />
											</td>
											<td class="px-4 py-4 text-sm font-medium text-gray-900">
												{lovelaceToAda(w.amount, 6)} <span class="text-sm text-gray-500">ADA</span>
											</td>
										</tr>
									{/each}
								</tbody>
							</table>
						</div>
					{/if}
				</div>
			{/if}

			{#if activeTabIndex == MINTS_TAB}
				<div class="w-full animate-fade-in">
					{#if !mints || mints.length === 0}
						<EmptyState title="No Mints" message="No mint or burn events found in this transaction." />
					{:else}
						<div class="overflow-x-auto">
							<table class="min-w-full divide-y divide-gray-200">
								<thead class="bg-gray-50">
									<tr>
										<th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Asset</th>
										<th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Policy ID</th>
										<th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Type</th>
										<th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Quantity</th>
									</tr>
								</thead>
								<tbody class="bg-white divide-y divide-gray-200">
									{#each mints as mint}
										{@const unit = mint.unit || (mint.policy + (mint.asset_name || ''))}
										{@const parsed = parseUnit(unit)}
										<tr class="hover:bg-gray-50">
											<td class="px-4 py-4 text-sm">
												<a href="/assets/unit/{unit}" class="text-blue-600 hover:underline">
													{parsed.assetNameUtf8 || parsed.assetNameHex || '(empty name)'}
												</a>
											</td>
											<td class="px-4 py-4 text-sm">
												<a href="/assets/policy/{parsed.policyId}" class="text-blue-600 hover:underline">
													{truncate(parsed.policyId, 20, '...')}
												</a>
											</td>
											<td class="px-4 py-4 text-sm">
												{#if mint.mint_type === 'MINT'}
													<span class="badge badge-success badge-sm">MINT</span>
												{:else}
													<span class="badge badge-error badge-sm">BURN</span>
												{/if}
											</td>
											<td class="px-4 py-4 text-sm font-medium text-gray-900">
												{mint.quantity}
											</td>
										</tr>
									{/each}
								</tbody>
							</table>
						</div>
					{/if}
				</div>
			{/if}

			{#if activeTabIndex == JSON_TAB}
				<div class="w-full animate-fade-in">
					<TxnContext {tx}></TxnContext>
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
