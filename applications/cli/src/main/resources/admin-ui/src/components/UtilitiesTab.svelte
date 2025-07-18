<script>
  import { onMount } from 'svelte';
  import { networkStatus } from '../lib/stores.js';
  import { api } from '../lib/api.js';
  import { addNotification } from '../lib/stores.js';

  let topupForm = {
    address: '',
    amount: 10000,
    loading: false
  };

  let addressQuery = {
    address: '',
    utxos: null,
    loading: false
  };

  let downloadsSection = {
    loading: false
  };

  async function topupAddress() {
    if (!topupForm.address.trim()) {
      addNotification('Please enter a valid address', 'error');
      return;
    }

    if (topupForm.amount <= 0) {
      addNotification('Amount must be greater than 0', 'error');
      return;
    }

    topupForm.loading = true;
    try {
      await api.topupAddress(topupForm.address, topupForm.amount);
      addNotification(`Successfully topped up ${topupForm.address.substring(0, 20)}... with ${topupForm.amount} ADA`, 'success');
      topupForm.address = '';
      topupForm.amount = 10000;
    } catch (error) {
      addNotification(`Failed to topup address: ${error.message}`, 'error');
    } finally {
      topupForm.loading = false;
    }
  }

  async function queryAddress() {
    if (!addressQuery.address.trim()) {
      addNotification('Please enter a valid address', 'error');
      return;
    }

    addressQuery.loading = true;
    try {
      const utxos = await api.getAddressUtxos(addressQuery.address);
      addressQuery.utxos = utxos;
      addNotification(`Found ${utxos.length} UTXOs for address`, 'success');
    } catch (error) {
      addNotification(`Failed to query address: ${error.message}`, 'error');
      addressQuery.utxos = null;
    } finally {
      addressQuery.loading = false;
    }
  }

  function formatAmount(amount) {
    if (typeof amount === 'string') {
      return parseFloat(amount) / 1000000;
    }
    return amount / 1000000;
  }

  function formatUtxo(utxo) {
    return {
      txHash: utxo.txHash,
      outputIndex: utxo.outputIndex,
      amount: formatAmount(utxo.amount),
      address: utxo.address
    };
  }

  async function downloadDevnetFiles() {
    try {
      downloadsSection.loading = true;
      await api.downloadDevnetFiles();
      addNotification('DevNet files download initiated', 'success');
    } catch (error) {
      addNotification(`Failed to download DevNet files: ${error.message}`, 'error');
    } finally {
      downloadsSection.loading = false;
    }
  }

  async function downloadGenesisFiles() {
    try {
      downloadsSection.loading = true;
      await api.downloadGenesisFiles();
      addNotification('Genesis files download initiated', 'success');
    } catch (error) {
      addNotification(`Failed to download Genesis files: ${error.message}`, 'error');
    } finally {
      downloadsSection.loading = false;
    }
  }

  function clearAddressQuery() {
    addressQuery.address = '';
    addressQuery.utxos = null;
  }
</script>

<div class="utilities-tab">
  <div class="tab-header">
    <h2>Utilities</h2>
    <div class="help-text">
      Tools for managing addresses, UTXOs, and DevNet files
    </div>
  </div>

  <div class="utilities-grid">
    <!-- Address Topup -->
    <div class="utility-card">
      <div class="card-header">
        <h3>💰 Address Topup</h3>
        <p>Add ADA to any address on the local DevNet</p>
      </div>
      <div class="card-content">
        <div class="form-group">
          <label for="topup-address">Address</label>
          <input
            id="topup-address"
            type="text"
            bind:value={topupForm.address}
            placeholder="addr1..."
            disabled={topupForm.loading || !$networkStatus.initialized}
          />
        </div>
        <div class="form-group">
          <label for="topup-amount">Amount (ADA)</label>
          <input
            id="topup-amount"
            type="number"
            bind:value={topupForm.amount}
            min="1"
            step="1"
            disabled={topupForm.loading || !$networkStatus.initialized}
          />
        </div>
        <button
          class="btn btn-primary"
          on:click={topupAddress}
          disabled={topupForm.loading || !$networkStatus.initialized}
        >
          {#if topupForm.loading}
            <span class="spinner"></span>
            Topping up...
          {:else}
            Topup Address
          {/if}
        </button>
        {#if !$networkStatus.initialized}
          <p class="warning-text">DevNet must be running to topup addresses</p>
        {/if}
      </div>
    </div>

    <!-- Address Query -->
    <div class="utility-card">
      <div class="card-header">
        <h3>🔍 Address Query</h3>
        <p>Query UTXOs for any address</p>
      </div>
      <div class="card-content">
        <div class="form-group">
          <label for="query-address">Address</label>
          <input
            id="query-address"
            type="text"
            bind:value={addressQuery.address}
            placeholder="addr1..."
            disabled={addressQuery.loading || !$networkStatus.initialized}
          />
        </div>
        <div class="button-group">
          <button
            class="btn btn-primary"
            on:click={queryAddress}
            disabled={addressQuery.loading || !$networkStatus.initialized}
          >
            {#if addressQuery.loading}
              <span class="spinner"></span>
              Querying...
            {:else}
              Query UTXOs
            {/if}
          </button>
          <button
            class="btn btn-secondary"
            on:click={clearAddressQuery}
            disabled={addressQuery.loading}
          >
            Clear
          </button>
        </div>
        {#if !$networkStatus.initialized}
          <p class="warning-text">DevNet must be running to query addresses</p>
        {/if}
      </div>
    </div>

    <!-- Downloads -->
    <div class="utility-card">
      <div class="card-header">
        <h3>📁 Downloads</h3>
        <p>Download DevNet configuration and genesis files</p>
      </div>
      <div class="card-content">
        <div class="download-buttons">
          <button
            class="btn btn-outline"
            on:click={downloadDevnetFiles}
            disabled={downloadsSection.loading || !$networkStatus.initialized}
          >
            {#if downloadsSection.loading}
              <span class="spinner"></span>
            {:else}
              📦
            {/if}
            DevNet Files
          </button>
          <button
            class="btn btn-outline"
            on:click={downloadGenesisFiles}
            disabled={downloadsSection.loading || !$networkStatus.initialized}
          >
            {#if downloadsSection.loading}
              <span class="spinner"></span>
            {:else}
              📄
            {/if}
            Genesis Files
          </button>
        </div>
        {#if !$networkStatus.initialized}
          <p class="warning-text">DevNet must be running to download files</p>
        {/if}
      </div>
    </div>
  </div>

  <!-- UTXOs Results -->
  {#if addressQuery.utxos !== null}
    <div class="results-section">
      <div class="results-header">
        <h3>UTXOs for {addressQuery.address.substring(0, 20)}...</h3>
        <div class="results-count">{addressQuery.utxos.length} UTXOs found</div>
      </div>
      <div class="results-content">
        {#if addressQuery.utxos.length === 0}
          <div class="empty-results">
            <p>No UTXOs found for this address</p>
          </div>
        {:else}
          <div class="utxos-table">
            <div class="table-header">
              <div class="col-txhash">Transaction Hash</div>
              <div class="col-index">Index</div>
              <div class="col-amount">Amount (ADA)</div>
            </div>
            {#each addressQuery.utxos as utxo}
              <div class="table-row">
                <div class="col-txhash">
                  <span class="hash" title={utxo.txHash}>
                    {utxo.txHash.substring(0, 16)}...
                  </span>
                </div>
                <div class="col-index">{utxo.outputIndex}</div>
                <div class="col-amount">{formatAmount(utxo.amount).toFixed(6)}</div>
              </div>
            {/each}
          </div>
        {/if}
      </div>
    </div>
  {/if}
</div>

<style>
  .utilities-tab {
    padding: 0;
  }

  .tab-header {
    margin-bottom: 2rem;
  }

  .tab-header h2 {
    margin: 0;
    color: #1f2937;
    font-size: 1.5rem;
    font-weight: 600;
  }

  .help-text {
    margin-top: 0.5rem;
    font-size: 0.875rem;
    color: #6b7280;
  }

  .utilities-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
    gap: 1.5rem;
    margin-bottom: 2rem;
  }

  .utility-card {
    background: white;
    border-radius: 0.75rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    overflow: hidden;
    border: 1px solid #e5e7eb;
  }

  .card-header {
    padding: 1.5rem 1.5rem 1rem;
    border-bottom: 1px solid #f3f4f6;
  }

  .card-header h3 {
    margin: 0;
    font-size: 1.125rem;
    font-weight: 600;
    color: #1f2937;
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }

  .card-header p {
    margin: 0.5rem 0 0 0;
    font-size: 0.875rem;
    color: #6b7280;
  }

  .card-content {
    padding: 1.5rem;
  }

  .form-group {
    margin-bottom: 1rem;
  }

  .form-group label {
    display: block;
    margin-bottom: 0.5rem;
    font-weight: 500;
    color: #374151;
    font-size: 0.875rem;
  }

  .form-group input {
    width: 100%;
    padding: 0.75rem;
    border: 1px solid #d1d5db;
    border-radius: 0.375rem;
    font-size: 0.875rem;
    transition: border-color 0.2s;
    box-sizing: border-box;
  }

  .form-group input:focus {
    outline: none;
    border-color: #667eea;
    box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
  }

  .form-group input:disabled {
    background-color: #f9fafb;
    color: #6b7280;
    cursor: not-allowed;
  }

  .button-group {
    display: flex;
    gap: 0.5rem;
    flex-wrap: wrap;
  }

  .btn {
    padding: 0.75rem 1.5rem;
    border: none;
    border-radius: 0.375rem;
    cursor: pointer;
    font-weight: 500;
    font-size: 0.875rem;
    transition: all 0.2s;
    display: flex;
    align-items: center;
    gap: 0.5rem;
    justify-content: center;
  }

  .btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  .btn-primary {
    background-color: #667eea;
    color: white;
  }

  .btn-primary:hover:not(:disabled) {
    background-color: #5a67d8;
  }

  .btn-secondary {
    background-color: #6b7280;
    color: white;
  }

  .btn-secondary:hover:not(:disabled) {
    background-color: #4b5563;
  }

  .btn-outline {
    background-color: white;
    color: #374151;
    border: 1px solid #d1d5db;
  }

  .btn-outline:hover:not(:disabled) {
    background-color: #f9fafb;
    border-color: #9ca3af;
  }

  .spinner {
    width: 1rem;
    height: 1rem;
    border: 2px solid transparent;
    border-top: 2px solid currentColor;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }

  @keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
  }

  .warning-text {
    font-size: 0.875rem;
    color: #d97706;
    margin-top: 0.5rem;
    font-style: italic;
  }

  .download-buttons {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }

  .results-section {
    background: white;
    border-radius: 0.75rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    overflow: hidden;
    border: 1px solid #e5e7eb;
  }

  .results-header {
    padding: 1.5rem;
    border-bottom: 1px solid #f3f4f6;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .results-header h3 {
    margin: 0;
    font-size: 1.125rem;
    font-weight: 600;
    color: #1f2937;
  }

  .results-count {
    font-size: 0.875rem;
    color: #6b7280;
    font-weight: 500;
  }

  .results-content {
    padding: 1.5rem;
  }

  .empty-results {
    text-align: center;
    color: #6b7280;
    padding: 2rem;
  }

  .utxos-table {
    display: grid;
    grid-template-columns: 1fr auto auto;
    gap: 0.5rem;
    font-size: 0.875rem;
  }

  .table-header {
    display: contents;
    font-weight: 600;
    color: #374151;
  }

  .table-header > div {
    padding: 0.75rem;
    background-color: #f9fafb;
    border-bottom: 1px solid #e5e7eb;
  }

  .table-row {
    display: contents;
  }

  .table-row > div {
    padding: 0.75rem;
    border-bottom: 1px solid #f3f4f6;
    display: flex;
    align-items: center;
  }

  .col-txhash {
    min-width: 0;
  }

  .col-index {
    text-align: center;
    width: 80px;
  }

  .col-amount {
    text-align: right;
    width: 120px;
    font-family: monospace;
    font-weight: 600;
  }

  .hash {
    font-family: monospace;
    cursor: help;
  }
</style>