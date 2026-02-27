<script lang="ts">
    import { onMount } from 'svelte';
    import { wallet, selectedAccount, formatAda, truncateAddress, truncateHash, formatTokenQuantity, type TransferAmount, type TransferResult } from './lib/stores/wallet';

    let isAvailable = false;
    let showImportModal = false;
    let showSendModal = false;
    let importMnemonic = '';
    let importName = '';
    let importError = '';
    let copySuccess = '';

    // Send modal state
    let sendReceiverMode: 'select' | 'custom' = 'select';
    let sendReceiverSelect = '';
    let sendReceiverCustom = '';
    let sendAsset = 'lovelace';
    let sendAmount = '';
    let sendError = '';
    let sendSuccess: TransferResult | null = null;

    onMount(async () => {
        isAvailable = await wallet.checkAvailability();
        if (isAvailable) {
            await wallet.loadAccounts();
        }
    });

    async function handleAccountChange(event: Event) {
        const select = event.target as HTMLSelectElement;
        await wallet.selectAccount(select.value);
    }

    async function handleRefresh() {
        if ($selectedAccount) {
            await wallet.loadBalance($selectedAccount.address);
            await wallet.loadUtxos($selectedAccount.address);
        }
    }

    async function handleImport() {
        importError = '';
        if (!importMnemonic.trim()) {
            importError = 'Please enter a mnemonic phrase';
            return;
        }
        if (!importName.trim()) {
            importError = 'Please enter an account name';
            return;
        }

        const result = await wallet.importAccount(importMnemonic.trim(), importName.trim());
        if (result) {
            showImportModal = false;
            importMnemonic = '';
            importName = '';
        } else {
            importError = $wallet.error || 'Failed to import account';
        }
    }

    async function handleDelete(accountId: string) {
        if (confirm('Are you sure you want to delete this account?')) {
            await wallet.deleteAccount(accountId);
        }
    }

    async function handleSetActive() {
        if ($wallet.selectedAccountId) {
            const success = await wallet.setActiveAccount($wallet.selectedAccountId);
            if (success) {
                copySuccess = `Account ${$wallet.selectedAccountId} set as active for dApps`;
                setTimeout(() => { copySuccess = ''; }, 3000);
            }
        }
    }

    function copyToClipboard(text: string, label: string) {
        navigator.clipboard.writeText(text);
        copySuccess = `${label} copied!`;
        setTimeout(() => { copySuccess = ''; }, 2000);
    }

    function getUtxoLovelace(utxo: any): string {
        if (utxo.amount && Array.isArray(utxo.amount)) {
            const lovelace = utxo.amount.find((a: any) => a.unit === 'lovelace');
            return lovelace ? String(lovelace.quantity) : '0';
        }
        return '0';
    }

    function getUtxoTokens(utxo: any): Array<{ unit: string; quantity: string }> {
        if (utxo.amount && Array.isArray(utxo.amount)) {
            return utxo.amount
                .filter((a: any) => a.unit !== 'lovelace')
                .map((a: any) => ({ unit: a.unit, quantity: String(a.quantity) }));
        }
        return [];
    }

    function openSendModal() {
        sendReceiverMode = 'select';
        sendReceiverSelect = '';
        sendReceiverCustom = '';
        sendAsset = 'lovelace';
        sendAmount = '';
        sendError = '';
        sendSuccess = null;
        showSendModal = true;
    }

    // Get the actual receiver address based on mode
    function getReceiverAddress(): string {
        if (sendReceiverMode === 'custom') {
            return sendReceiverCustom.trim();
        }
        return sendReceiverSelect;
    }

    // Get available receiver accounts (excluding currently selected account)
    function getReceiverAccounts() {
        return $wallet.accounts.filter(a => a.id !== $wallet.selectedAccountId);
    }

    async function handleSend() {
        sendError = '';
        sendSuccess = null;

        // Get receiver address
        const receiver = getReceiverAddress();

        // Validate receiver
        if (!receiver) {
            sendError = sendReceiverMode === 'custom'
                ? 'Please enter a receiver address'
                : 'Please select a receiver account';
            return;
        }
        if (!receiver.startsWith('addr')) {
            sendError = 'Invalid address format. Address should start with "addr"';
            return;
        }

        // Validate amount
        if (!sendAmount.trim()) {
            sendError = 'Please enter an amount';
            return;
        }

        let quantityStr: string;
        if (sendAsset === 'lovelace') {
            // Convert ADA to lovelace
            const adaAmount = parseFloat(sendAmount);
            if (isNaN(adaAmount) || adaAmount <= 0) {
                sendError = 'Please enter a valid amount';
                return;
            }
            quantityStr = String(Math.floor(adaAmount * 1_000_000));
        } else {
            // Native token - use raw quantity
            const qty = parseInt(sendAmount);
            if (isNaN(qty) || qty <= 0) {
                sendError = 'Please enter a valid token quantity';
                return;
            }
            quantityStr = String(qty);
        }

        // Build amounts array
        const amounts: TransferAmount[] = [];

        if (sendAsset === 'lovelace') {
            amounts.push({ unit: 'lovelace', quantity: quantityStr });
        } else {
            amounts.push({ unit: sendAsset, quantity: quantityStr });
        }

        const result = await wallet.transfer(receiver, amounts);

        if (result.success) {
            sendSuccess = result;
        } else {
            sendError = result.message || 'Transfer failed';
        }
    }

    function closeSendModal() {
        showSendModal = false;
        sendSuccess = null;
    }

    // Get display name for a token
    function getTokenDisplayName(unit: string): string {
        const token = $wallet.tokens.find(t => t.unit === unit);
        if (token?.assetNameUtf8) {
            return token.assetNameUtf8;
        }
        return truncateHash(unit, 8);
    }
</script>

<div class="container mx-auto px-4 py-8 max-w-6xl">
    <!-- Header -->
    <div class="flex items-center justify-between mb-8">
        <div>
            <h1 class="text-3xl font-bold text-gray-900">Development Wallet</h1>
            <p class="text-gray-600 mt-1">Manage accounts and test transactions on your local devnet</p>
        </div>
        {#if copySuccess}
            <div class="alert alert-success py-2 px-4">
                <span>{copySuccess}</span>
            </div>
        {/if}
    </div>

    {#if !isAvailable}
        <!-- Wallet Not Available -->
        <div class="card bg-base-100 shadow-xl">
            <div class="card-body items-center text-center">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-16 w-16 text-warning mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                </svg>
                <h2 class="card-title text-2xl">Wallet API Not Available</h2>
                <p class="text-gray-600 max-w-md">
                    Make sure Yaci DevKit CLI is running on port 10000 with the wallet API enabled.
                </p>
                <div class="mt-4 bg-gray-100 p-4 rounded-lg w-full max-w-lg">
                    <code class="text-sm">
                        java -Dyaci.store.enabled=true -jar yaci-cli.jar create-node -o --start
                    </code>
                </div>
                <button class="btn btn-primary mt-4" on:click={() => location.reload()}>
                    Retry Connection
                </button>
            </div>
        </div>
    {:else if $wallet.loading && $wallet.accounts.length === 0}
        <!-- Loading State -->
        <div class="flex justify-center items-center py-20">
            <span class="loading loading-spinner loading-lg"></span>
        </div>
    {:else}
        <!-- Account Selector -->
        <div class="card bg-base-100 shadow-xl mb-6">
            <div class="card-body">
                <div class="flex flex-wrap items-center gap-4 justify-between">
                    <div class="flex items-center gap-4 flex-1">
                        <label class="font-semibold text-gray-700">Account:</label>
                        <select
                            class="select select-bordered flex-1 max-w-md"
                            value={$wallet.selectedAccountId}
                            on:change={handleAccountChange}
                        >
                            {#each $wallet.accounts as account}
                                <option value={account.id}>
                                    {account.name} - {truncateAddress(account.address)}
                                </option>
                            {/each}
                        </select>
                    </div>
                    <div class="flex gap-2">
                        <button
                            class="btn btn-outline btn-sm btn-accent"
                            on:click={handleSetActive}
                            title="Set this account as the active CIP-30 wallet account for dApps"
                        >
                            Set Active for dApps
                        </button>
                        <button class="btn btn-outline btn-sm" on:click={handleRefresh}>
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                            </svg>
                            Refresh
                        </button>
                        <button class="btn btn-success btn-sm" on:click={openSendModal}>
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
                            </svg>
                            Send
                        </button>
                        <button class="btn btn-primary btn-sm" on:click={() => showImportModal = true}>
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                            </svg>
                            Import Account
                        </button>
                    </div>
                </div>
            </div>
        </div>

        {#if $selectedAccount}
            <!-- Account Details -->
            <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
                <!-- Balance Card -->
                <div class="card bg-gradient-to-br from-indigo-600 to-purple-700 text-white shadow-xl">
                    <div class="card-body">
                        <h2 class="card-title text-white/80 text-sm font-normal">Balance</h2>
                        <div class="text-3xl font-bold">
                            {formatAda($wallet.balance)} ADA
                        </div>
                        <div class="text-white/70 text-sm">
                            {$wallet.balance.toLocaleString()} lovelace
                        </div>
                    </div>
                </div>

                <!-- Address Card -->
                <div class="card bg-base-100 shadow-xl lg:col-span-2">
                    <div class="card-body">
                        <h2 class="card-title text-sm font-normal text-gray-500">Address</h2>
                        <div class="flex items-center gap-2">
                            <code class="text-sm bg-gray-100 p-2 rounded flex-1 break-all">
                                {$selectedAccount.address}
                            </code>
                            <button
                                class="btn btn-ghost btn-sm"
                                on:click={() => copyToClipboard($selectedAccount?.address || '', 'Address')}
                            >
                                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
                                </svg>
                            </button>
                        </div>
                        {#if $selectedAccount.stakeAddress}
                            <div class="mt-2">
                                <span class="text-xs text-gray-500">Stake Address:</span>
                                <code class="text-xs ml-2">{truncateAddress($selectedAccount.stakeAddress, 16, 10)}</code>
                            </div>
                        {/if}
                    </div>
                </div>
            </div>

            <!-- Native Tokens Table -->
            {#if $wallet.tokens.length > 0}
                <div class="card bg-base-100 shadow-xl mb-6">
                    <div class="card-body">
                        <div class="flex items-center justify-between mb-4">
                            <h2 class="card-title">
                                Native Tokens
                                <span class="badge badge-secondary">{$wallet.tokens.length}</span>
                            </h2>
                        </div>
                        <div class="overflow-x-auto">
                            <table class="table table-zebra w-full">
                                <thead>
                                    <tr>
                                        <th>Token Name</th>
                                        <th>Policy ID</th>
                                        <th class="text-right">Quantity</th>
                                        <th>Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {#each $wallet.tokens as token}
                                        <tr>
                                            <td>
                                                <span class="font-semibold">
                                                    {token.assetNameUtf8 || truncateHash(token.assetName || '', 8) || '(unnamed)'}
                                                </span>
                                                {#if token.assetName && token.assetNameUtf8}
                                                    <br>
                                                    <span class="text-xs text-gray-500 font-mono">{truncateHash(token.assetName, 8)}</span>
                                                {/if}
                                            </td>
                                            <td>
                                                <button
                                                    class="font-mono text-sm link link-primary"
                                                    on:click={() => copyToClipboard(token.policyId || '', 'Policy ID')}
                                                    title={token.policyId}
                                                >
                                                    {truncateHash(token.policyId || '', 8)}
                                                </button>
                                            </td>
                                            <td class="text-right font-mono font-semibold">
                                                {formatTokenQuantity(token.quantity)}
                                            </td>
                                            <td>
                                                <button
                                                    class="btn btn-ghost btn-xs"
                                                    on:click={() => copyToClipboard(token.unit, 'Token unit')}
                                                    title="Copy full token unit"
                                                >
                                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
                                                    </svg>
                                                </button>
                                            </td>
                                        </tr>
                                    {/each}
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            {/if}

            <!-- UTXOs Table -->
            <div class="card bg-base-100 shadow-xl">
                <div class="card-body">
                    <div class="flex items-center justify-between mb-4">
                        <h2 class="card-title">
                            UTXOs
                            <span class="badge badge-primary">{$wallet.utxos.length}</span>
                        </h2>
                    </div>

                    {#if $wallet.utxos.length === 0}
                        <div class="text-center py-8 text-gray-500">
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 mx-auto mb-4 opacity-50" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
                            </svg>
                            <p>No UTXOs found for this account</p>
                        </div>
                    {:else}
                        <div class="overflow-x-auto">
                            <table class="table table-zebra w-full">
                                <thead>
                                    <tr>
                                        <th>Tx Hash</th>
                                        <th>Index</th>
                                        <th class="text-right">ADA</th>
                                        <th>Tokens</th>
                                        <th>Block</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {#each $wallet.utxos as utxo}
                                        <tr>
                                            <td>
                                                <a
                                                    href="http://localhost:5173/transactions/{utxo.tx_hash}"
                                                    target="_blank"
                                                    class="link link-primary font-mono text-sm"
                                                >
                                                    {truncateAddress(utxo.tx_hash, 10, 8)}
                                                </a>
                                            </td>
                                            <td>{utxo.output_index}</td>
                                            <td class="text-right font-mono">
                                                {formatAda(getUtxoLovelace(utxo))}
                                            </td>
                                            <td>
                                                {#if getUtxoTokens(utxo).length > 0}
                                                    <span class="badge badge-secondary badge-sm">
                                                        {getUtxoTokens(utxo).length} token{getUtxoTokens(utxo).length > 1 ? 's' : ''}
                                                    </span>
                                                {:else}
                                                    <span class="text-gray-400">-</span>
                                                {/if}
                                            </td>
                                            <td>
                                                {#if utxo.block_number !== undefined && utxo.block_number >= 0}
                                                    <a href="http://localhost:5173/blocks/{utxo.block_number}" target="_blank" class="link">
                                                        {utxo.block_number}
                                                    </a>
                                                {:else}
                                                    <span class="badge badge-ghost">Genesis</span>
                                                {/if}
                                            </td>
                                        </tr>
                                    {/each}
                                </tbody>
                            </table>
                        </div>
                    {/if}
                </div>
            </div>

            <!-- Account Info Card -->
            <div class="card bg-base-100 shadow-xl mt-6">
                <div class="card-body">
                    <h2 class="card-title">Account Info</h2>
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div>
                            <span class="text-sm text-gray-500">Account ID</span>
                            <p class="font-mono">{$selectedAccount.id}</p>
                        </div>
                        <div>
                            <span class="text-sm text-gray-500">Name</span>
                            <p>{$selectedAccount.name}</p>
                        </div>
                        <div>
                            <span class="text-sm text-gray-500">Type</span>
                            <p>
                                {#if $selectedAccount.isDefault}
                                    <span class="badge badge-primary">Default Account</span>
                                {:else}
                                    <span class="badge badge-secondary">Custom Import</span>
                                    <button
                                        class="btn btn-ghost btn-xs text-error ml-2"
                                        on:click={() => handleDelete($selectedAccount?.id || '')}
                                    >
                                        Delete
                                    </button>
                                {/if}
                            </p>
                        </div>
                    </div>
                </div>
            </div>
        {/if}

        <!-- Integration Info -->
        <div class="card bg-base-100 shadow-xl mt-6">
            <div class="card-body">
                <h2 class="card-title">dApp Integration</h2>
                <p class="text-gray-600 mb-4">
                    Use the Yaci DevKit CIP-30 wallet SDK to integrate with your dApps during development:
                </p>
                <div class="mockup-code bg-gray-900">
                    <pre data-prefix="1"><code>&lt;script src="http://localhost:10000/wallet-sdk.js"&gt;&lt;/script&gt;</code></pre>
                    <pre data-prefix="2"><code></code></pre>
                    <pre data-prefix="3"><code>// Connect to wallet (dev or prod)</code></pre>
                    <pre data-prefix="4"><code>const wallet = await window.cardano.yacidevkit.enable();</code></pre>
                    <pre data-prefix="5"><code>const balance = await wallet.getBalance();</code></pre>
                    <pre data-prefix="6"><code>const address = await wallet.getChangeAddress();</code></pre>
                </div>
                <div class="mt-4">
                    <a href="http://localhost:3000" target="_blank" class="btn btn-outline btn-sm">
                        View Demo App
                    </a>
                </div>
            </div>
        </div>
    {/if}
</div>

<!-- Import Modal -->
{#if showImportModal}
    <div class="modal modal-open">
        <div class="modal-box">
            <h3 class="font-bold text-lg">Import Account</h3>
            <p class="py-2 text-gray-600">Enter a mnemonic phrase to import a custom wallet account.</p>

            {#if importError}
                <div class="alert alert-error mt-2">
                    <span>{importError}</span>
                </div>
            {/if}

            <div class="form-control mt-4">
                <label class="label">
                    <span class="label-text">Account Name</span>
                </label>
                <input
                    type="text"
                    placeholder="My Account"
                    class="input input-bordered"
                    bind:value={importName}
                />
            </div>

            <div class="form-control mt-4">
                <label class="label">
                    <span class="label-text">Mnemonic Phrase</span>
                </label>
                <textarea
                    class="textarea textarea-bordered h-24"
                    placeholder="Enter your 24-word mnemonic phrase..."
                    bind:value={importMnemonic}
                ></textarea>
            </div>

            <div class="modal-action">
                <button class="btn btn-ghost" on:click={() => showImportModal = false}>Cancel</button>
                <button class="btn btn-primary" on:click={handleImport} disabled={$wallet.loading}>
                    {#if $wallet.loading}
                        <span class="loading loading-spinner loading-sm"></span>
                    {:else}
                        Import
                    {/if}
                </button>
            </div>
        </div>
        <!-- svelte-ignore a11y-click-events-have-key-events a11y-no-static-element-interactions -->
        <div class="modal-backdrop" on:click={() => showImportModal = false}></div>
    </div>
{/if}

<!-- Send Modal -->
{#if showSendModal}
    <div class="modal modal-open">
        <div class="modal-box">
            <h3 class="font-bold text-lg">Send Assets</h3>
            <p class="py-2 text-gray-600">Send ADA or native tokens to another address.</p>

            {#if sendError}
                <div class="alert alert-error mt-2">
                    <svg xmlns="http://www.w3.org/2000/svg" class="stroke-current shrink-0 h-5 w-5" fill="none" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    <span>{sendError}</span>
                </div>
            {/if}

            {#if sendSuccess}
                <div class="alert alert-success mt-2">
                    <svg xmlns="http://www.w3.org/2000/svg" class="stroke-current shrink-0 h-5 w-5" fill="none" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    <div>
                        <span class="font-semibold">Transaction submitted!</span>
                        <br>
                        <a href="http://localhost:5173/transactions/{sendSuccess.txHash}" target="_blank" class="link text-sm font-mono">
                            {truncateHash(sendSuccess.txHash || '', 16)}
                        </a>
                    </div>
                </div>
            {:else}
                <div class="form-control mt-4">
                    <label class="label">
                        <span class="label-text">Receiver</span>
                    </label>
                    <div class="flex gap-2 mb-2">
                        <label class="label cursor-pointer gap-2">
                            <input
                                type="radio"
                                name="receiverMode"
                                class="radio radio-sm radio-primary"
                                checked={sendReceiverMode === 'select'}
                                on:change={() => sendReceiverMode = 'select'}
                            />
                            <span class="label-text">Known Account</span>
                        </label>
                        <label class="label cursor-pointer gap-2">
                            <input
                                type="radio"
                                name="receiverMode"
                                class="radio radio-sm radio-primary"
                                checked={sendReceiverMode === 'custom'}
                                on:change={() => sendReceiverMode = 'custom'}
                            />
                            <span class="label-text">Custom Address</span>
                        </label>
                    </div>

                    {#if sendReceiverMode === 'select'}
                        <select class="select select-bordered w-full" bind:value={sendReceiverSelect}>
                            <option value="">Select an account...</option>
                            {#each getReceiverAccounts() as account}
                                <option value={account.address}>
                                    {account.name} - {truncateAddress(account.address)}
                                </option>
                            {/each}
                        </select>
                        {#if sendReceiverSelect}
                            <label class="label">
                                <span class="label-text-alt font-mono text-xs break-all">{sendReceiverSelect}</span>
                            </label>
                        {/if}
                    {:else}
                        <input
                            type="text"
                            placeholder="addr_test1..."
                            class="input input-bordered font-mono text-sm"
                            bind:value={sendReceiverCustom}
                        />
                    {/if}
                </div>

                <div class="form-control mt-4">
                    <label class="label">
                        <span class="label-text">Asset</span>
                    </label>
                    <select class="select select-bordered" bind:value={sendAsset}>
                        <option value="lovelace">ADA (lovelace)</option>
                        {#each $wallet.tokens as token}
                            <option value={token.unit}>
                                {token.assetNameUtf8 || truncateHash(token.assetName || '', 8) || '(unnamed)'} - {formatTokenQuantity(token.quantity)} available
                            </option>
                        {/each}
                    </select>
                </div>

                <div class="form-control mt-4">
                    <label class="label">
                        <span class="label-text">
                            {sendAsset === 'lovelace' ? 'Amount (ADA)' : 'Quantity'}
                        </span>
                        {#if sendAsset === 'lovelace'}
                            <span class="label-text-alt">
                                Available: {formatAda($wallet.balance)} ADA
                            </span>
                        {/if}
                    </label>
                    <input
                        type="text"
                        placeholder={sendAsset === 'lovelace' ? '10.0' : '100'}
                        class="input input-bordered"
                        bind:value={sendAmount}
                    />
                </div>
            {/if}

            <div class="modal-action">
                <button class="btn btn-ghost" on:click={closeSendModal}>
                    {sendSuccess ? 'Close' : 'Cancel'}
                </button>
                {#if !sendSuccess}
                    <button
                        class="btn btn-success"
                        on:click={handleSend}
                        disabled={$wallet.transferInProgress}
                    >
                        {#if $wallet.transferInProgress}
                            <span class="loading loading-spinner loading-sm"></span>
                            Sending...
                        {:else}
                            Send
                        {/if}
                    </button>
                {/if}
            </div>
        </div>
        <!-- svelte-ignore a11y-click-events-have-key-events a11y-no-static-element-interactions -->
        <div class="modal-backdrop" on:click={closeSendModal}></div>
    </div>
{/if}

<style>
    .container {
        max-width: 1200px;
    }
</style>
