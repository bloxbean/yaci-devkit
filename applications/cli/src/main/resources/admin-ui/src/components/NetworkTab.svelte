<script>
  import { onMount } from 'svelte';
  import { networkStatus } from '../lib/stores.js';
  import { api } from '../lib/api.js';
  import { addNotification } from '../lib/stores.js';

  let networkInfo = {
    tip: null,
    genesisHash: null,
    protocolParams: null,
    loading: true
  };

  onMount(async () => {
    await loadNetworkInfo();
  });

  async function loadNetworkInfo() {
    try {
      networkInfo.loading = true;
      
      if ($networkStatus.initialized) {
        const [tip, genesisHash, protocolParams] = await Promise.all([
          api.getDevnetTip().catch(() => null),
          api.getGenesisHash().catch(() => null),
          api.getProtocolParameters().catch(() => null)
        ]);
        
        networkInfo = {
          tip,
          genesisHash,
          protocolParams,
          loading: false
        };
      } else {
        networkInfo.loading = false;
      }
    } catch (error) {
      console.error('Failed to load network info:', error);
      networkInfo.loading = false;
    }
  }

  async function refreshNetworkInfo() {
    addNotification('Refreshing network information...', 'info');
    await loadNetworkInfo();
    addNotification('Network information refreshed', 'success');
  }

  function formatHash(hash) {
    if (!hash) return 'N/A';
    return hash.length > 16 ? `${hash.substring(0, 8)}...${hash.substring(hash.length - 8)}` : hash;
  }

  function formatTimestamp(timestamp) {
    if (!timestamp) return 'N/A';
    return new Date(timestamp * 1000).toLocaleString();
  }
</script>

<div class="network-tab">
  <div class="tab-header">
    <h2>Network Information</h2>
    <button 
      class="btn btn-primary"
      on:click={refreshNetworkInfo}
      disabled={networkInfo.loading}
    >
      🔄 Refresh
    </button>
  </div>

  {#if networkInfo.loading}
    <div class="loading-state">
      <div class="spinner"></div>
      <p>Loading network information...</p>
    </div>
  {:else if !$networkStatus.initialized}
    <div class="empty-state">
      <h3>DevNet Not Running</h3>
      <p>Please create or start a DevNet to view network information.</p>
    </div>
  {:else}
    <div class="network-cards">
      <div class="card">
        <div class="card-header">
          <h3>Chain Tip</h3>
        </div>
        <div class="card-content">
          {#if networkInfo.tip}
            <div class="info-grid">
              <div class="info-item">
                <span class="label">Block Height</span>
                <span class="value">{networkInfo.tip.blockHeight || networkInfo.tip.block || 'N/A'}</span>
              </div>
              <div class="info-item">
                <span class="label">Slot</span>
                <span class="value">{networkInfo.tip.slot || 'N/A'}</span>
              </div>
              <div class="info-item">
                <span class="label">Hash</span>
                <span class="value hash" title={networkInfo.tip.hash}>{formatHash(networkInfo.tip.hash)}</span>
              </div>
              <div class="info-item">
                <span class="label">Previous Hash</span>
                <span class="value hash" title={networkInfo.tip.prevHash}>{formatHash(networkInfo.tip.prevHash)}</span>
              </div>
            </div>
          {:else}
            <p class="no-data">No tip information available</p>
          {/if}
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <h3>Genesis Information</h3>
        </div>
        <div class="card-content">
          <div class="info-grid">
            <div class="info-item">
              <span class="label">Genesis Hash</span>
              <span class="value hash" title={networkInfo.genesisHash}>{formatHash(networkInfo.genesisHash)}</span>
            </div>
            <div class="info-item">
              <span class="label">Network ID</span>
              <span class="value">Local DevNet</span>
            </div>
            <div class="info-item">
              <span class="label">Era</span>
              <span class="value">{$networkStatus.era || 'Conway'}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="card full-width">
        <div class="card-header">
          <h3>Protocol Parameters</h3>
        </div>
        <div class="card-content">
          {#if networkInfo.protocolParams}
            <div class="params-grid">
              <div class="param-section">
                <h4>Fees</h4>
                <div class="param-list">
                  <div class="param-item">
                    <span class="param-label">Min Fee A</span>
                    <span class="param-value">{networkInfo.protocolParams.minFeeA || 'N/A'}</span>
                  </div>
                  <div class="param-item">
                    <span class="param-label">Min Fee B</span>
                    <span class="param-value">{networkInfo.protocolParams.minFeeB || 'N/A'}</span>
                  </div>
                  <div class="param-item">
                    <span class="param-label">Key Deposit</span>
                    <span class="param-value">{networkInfo.protocolParams.keyDeposit || 'N/A'}</span>
                  </div>
                  <div class="param-item">
                    <span class="param-label">Pool Deposit</span>
                    <span class="param-value">{networkInfo.protocolParams.poolDeposit || 'N/A'}</span>
                  </div>
                </div>
              </div>
              
              <div class="param-section">
                <h4>Limits</h4>
                <div class="param-list">
                  <div class="param-item">
                    <span class="param-label">Max Block Size</span>
                    <span class="param-value">{networkInfo.protocolParams.maxBlockSize || 'N/A'}</span>
                  </div>
                  <div class="param-item">
                    <span class="param-label">Max Tx Size</span>
                    <span class="param-value">{networkInfo.protocolParams.maxTxSize || 'N/A'}</span>
                  </div>
                  <div class="param-item">
                    <span class="param-label">Max Block Header Size</span>
                    <span class="param-value">{networkInfo.protocolParams.maxBlockHeaderSize || 'N/A'}</span>
                  </div>
                  <div class="param-item">
                    <span class="param-label">UTXO Cost Per Word</span>
                    <span class="param-value">{networkInfo.protocolParams.utxoCostPerWord || 'N/A'}</span>
                  </div>
                </div>
              </div>
              
              <div class="param-section">
                <h4>Governance</h4>
                <div class="param-list">
                  <div class="param-item">
                    <span class="param-label">Min Pool Cost</span>
                    <span class="param-value">{networkInfo.protocolParams.minPoolCost || 'N/A'}</span>
                  </div>
                  <div class="param-item">
                    <span class="param-label">Price Memory</span>
                    <span class="param-value">{networkInfo.protocolParams.priceMemory || 'N/A'}</span>
                  </div>
                  <div class="param-item">
                    <span class="param-label">Price Steps</span>
                    <span class="param-value">{networkInfo.protocolParams.priceSteps || 'N/A'}</span>
                  </div>
                  <div class="param-item">
                    <span class="param-label">Max Tx Ex Units</span>
                    <span class="param-value">{networkInfo.protocolParams.maxTxExUnits || 'N/A'}</span>
                  </div>
                </div>
              </div>
            </div>
          {:else}
            <p class="no-data">No protocol parameters available</p>
          {/if}
        </div>
      </div>
    </div>
  {/if}
</div>

<style>
  .network-tab {
    padding: 0;
  }

  .tab-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 2rem;
  }

  .tab-header h2 {
    margin: 0;
    color: #1f2937;
    font-size: 1.5rem;
    font-weight: 600;
  }

  .btn {
    padding: 0.5rem 1rem;
    border: none;
    border-radius: 0.5rem;
    cursor: pointer;
    font-weight: 500;
    transition: all 0.2s;
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

  .loading-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 4rem;
    text-align: center;
  }

  .spinner {
    width: 2rem;
    height: 2rem;
    border: 3px solid #f3f4f6;
    border-top: 3px solid #667eea;
    border-radius: 50%;
    animation: spin 1s linear infinite;
    margin-bottom: 1rem;
  }

  @keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
  }

  .empty-state {
    text-align: center;
    padding: 4rem;
    color: #6b7280;
  }

  .empty-state h3 {
    margin-bottom: 1rem;
    color: #1f2937;
  }

  .network-cards {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 1.5rem;
  }

  .card {
    background: white;
    border-radius: 0.75rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    overflow: hidden;
  }

  .card.full-width {
    grid-column: 1 / -1;
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
  }

  .card-content {
    padding: 1rem 1.5rem 1.5rem;
  }

  .info-grid {
    display: grid;
    gap: 1rem;
  }

  .info-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0.5rem 0;
  }

  .label {
    font-weight: 500;
    color: #6b7280;
  }

  .value {
    font-weight: 600;
    color: #1f2937;
  }

  .value.hash {
    font-family: monospace;
    font-size: 0.875rem;
    cursor: help;
  }

  .no-data {
    color: #6b7280;
    font-style: italic;
    text-align: center;
    margin: 2rem 0;
  }

  .params-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 2rem;
  }

  .param-section h4 {
    margin: 0 0 1rem 0;
    color: #1f2937;
    font-size: 1rem;
    font-weight: 600;
    border-bottom: 1px solid #e5e7eb;
    padding-bottom: 0.5rem;
  }

  .param-list {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }

  .param-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .param-label {
    font-size: 0.875rem;
    color: #6b7280;
    font-weight: 500;
  }

  .param-value {
    font-size: 0.875rem;
    color: #1f2937;
    font-weight: 600;
    font-family: monospace;
  }
</style>