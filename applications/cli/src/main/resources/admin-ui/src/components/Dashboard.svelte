<script>
  import { onMount } from 'svelte';
  import { networkStatus, services, isNetworkRunning, runningServicesCount } from '../lib/stores.js';
  import { api } from '../lib/api.js';
  import { addNotification } from '../lib/stores.js';

  let stats = {
    epoch: null,
    slot: null,
    blockHeight: null,
    kesPeriod: null,
    loading: true
  };

  onMount(async () => {
    await loadStats();
    startStatsRefresh();
  });

  async function loadStats() {
    try {
      stats.loading = true;
      
      if ($networkStatus.initialized) {
        const [epoch, kesPeriod, tip] = await Promise.all([
          api.getLatestEpoch().catch(() => null),
          api.getKesPeriod().catch(() => null),
          api.getDevnetTip().catch(() => null)
        ]);
        
        stats = {
          epoch: epoch?.epoch || null,
          slot: tip?.slot || null,
          blockHeight: tip?.blockHeight || tip?.block || null,
          kesPeriod: kesPeriod?.kesPeriod || null,
          loading: false
        };
      } else {
        stats.loading = false;
      }
    } catch (error) {
      console.error('Failed to load stats:', error);
      stats.loading = false;
    }
  }

  function startStatsRefresh() {
    setInterval(loadStats, 10000);
  }

  async function resetDevnet() {
    try {
      addNotification('Resetting DevNet...', 'info');
      await api.resetDevnet();
      addNotification('DevNet reset successfully', 'success');
      await loadStats();
    } catch (error) {
      addNotification(`Failed to reset DevNet: ${error.message}`, 'error');
    }
  }

  async function createDevnet() {
    try {
      addNotification('Creating DevNet...', 'info');
      await api.createDevnet();
      addNotification('DevNet created successfully', 'success');
      await loadStats();
    } catch (error) {
      addNotification(`Failed to create DevNet: ${error.message}`, 'error');
    }
  }
</script>

<div class="dashboard">
  <div class="dashboard-header">
    <h2>DevNet Dashboard</h2>
    <div class="quick-actions">
      <button 
        class="btn btn-primary"
        on:click={createDevnet}
        disabled={$networkStatus.loading}
      >
        🚀 Create DevNet
      </button>
      <button 
        class="btn btn-secondary"
        on:click={resetDevnet}
        disabled={$networkStatus.loading || !$networkStatus.initialized}
      >
        🔄 Reset DevNet
      </button>
    </div>
  </div>

  <div class="cards-grid">
    <div class="card network-status">
      <div class="card-header">
        <h3>Network Status</h3>
        <div class="status-badge {$isNetworkRunning ? 'running' : 'stopped'}">
          {$isNetworkRunning ? 'Running' : 'Stopped'}
        </div>
      </div>
      <div class="card-content">
        {#if $networkStatus.loading || stats.loading}
          <div class="loading">Loading network information...</div>
        {:else if $networkStatus.initialized}
          <div class="stat-grid">
            <div class="stat">
              <span class="label">Epoch</span>
              <span class="value">{stats.epoch || 'N/A'}</span>
            </div>
            <div class="stat">
              <span class="label">Slot</span>
              <span class="value">{stats.slot || 'N/A'}</span>
            </div>
            <div class="stat">
              <span class="label">Block Height</span>
              <span class="value">{stats.blockHeight || 'N/A'}</span>
            </div>
            <div class="stat">
              <span class="label">KES Period</span>
              <span class="value">{stats.kesPeriod || 'N/A'}</span>
            </div>
          </div>
        {:else}
          <div class="empty-state">
            <p>DevNet is not running</p>
            <p class="help-text">Create or start a DevNet to see network statistics</p>
          </div>
        {/if}
      </div>
    </div>

    <div class="card services-overview">
      <div class="card-header">
        <h3>Services Overview</h3>
        <div class="services-count">
          {$runningServicesCount} / {Object.keys($services).length - 1} Running
        </div>
      </div>
      <div class="card-content">
        <div class="services-list">
          {#each Object.entries($services) as [name, service]}
            {#if typeof service === 'object' && service.port}
              <div class="service-item">
                <div class="service-info">
                  <span class="service-name">{name}</span>
                  <span class="service-port">:{service.port}</span>
                </div>
                <div class="service-status {service.running ? 'running' : 'stopped'}">
                  {service.running ? '✓' : '✗'}
                </div>
              </div>
            {/if}
          {/each}
        </div>
      </div>
    </div>

    <div class="card recent-activity">
      <div class="card-header">
        <h3>Quick Actions</h3>
      </div>
      <div class="card-content">
        <div class="action-buttons">
          <button 
            class="action-btn"
            on:click={() => api.downloadDevnetFiles()}
            disabled={!$networkStatus.initialized}
          >
            📁 Download DevNet Files
          </button>
          <button 
            class="action-btn"
            on:click={() => api.downloadGenesisFiles()}
            disabled={!$networkStatus.initialized}
          >
            📄 Download Genesis Files
          </button>
        </div>
      </div>
    </div>

    <div class="card system-info">
      <div class="card-header">
        <h3>System Information</h3>
      </div>
      <div class="card-content">
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">Era</span>
            <span class="info-value">{$networkStatus.era || 'Conway'}</span>
          </div>
          <div class="info-item">
            <span class="info-label">Network ID</span>
            <span class="info-value">Local DevNet</span>
          </div>
          <div class="info-item">
            <span class="info-label">Protocol</span>
            <span class="info-value">Cardano</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>

<style>
  .dashboard {
    padding: 0;
  }

  .dashboard-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 2rem;
  }

  .dashboard-header h2 {
    margin: 0;
    color: #1f2937;
    font-size: 1.5rem;
    font-weight: 600;
  }

  .quick-actions {
    display: flex;
    gap: 1rem;
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

  .btn-secondary {
    background-color: #6b7280;
    color: white;
  }

  .btn-secondary:hover:not(:disabled) {
    background-color: #4b5563;
  }

  .cards-grid {
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

  .card-header {
    padding: 1.5rem 1.5rem 1rem;
    display: flex;
    justify-content: space-between;
    align-items: center;
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

  .status-badge {
    padding: 0.25rem 0.75rem;
    border-radius: 1rem;
    font-size: 0.75rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  .status-badge.running {
    background-color: #d1fae5;
    color: #065f46;
  }

  .status-badge.stopped {
    background-color: #fee2e2;
    color: #991b1b;
  }

  .services-count {
    font-size: 0.875rem;
    color: #6b7280;
    font-weight: 500;
  }

  .loading {
    text-align: center;
    color: #6b7280;
    font-style: italic;
  }

  .empty-state {
    text-align: center;
    color: #6b7280;
  }

  .help-text {
    font-size: 0.875rem;
    margin-top: 0.5rem;
  }

  .stat-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 1rem;
  }

  .stat {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
  }

  .stat .label {
    font-size: 0.875rem;
    color: #6b7280;
    font-weight: 500;
  }

  .stat .value {
    font-size: 1.25rem;
    font-weight: 600;
    color: #1f2937;
  }

  .services-list {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }

  .service-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0.75rem;
    background-color: #f9fafb;
    border-radius: 0.5rem;
  }

  .service-info {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
  }

  .service-name {
    font-weight: 500;
    color: #1f2937;
    text-transform: capitalize;
  }

  .service-port {
    font-size: 0.875rem;
    color: #6b7280;
    font-family: monospace;
  }

  .service-status {
    width: 1.5rem;
    height: 1.5rem;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: bold;
    color: white;
    font-size: 0.875rem;
  }

  .service-status.running {
    background-color: #10b981;
  }

  .service-status.stopped {
    background-color: #ef4444;
  }

  .action-buttons {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }

  .action-btn {
    padding: 0.75rem 1rem;
    border: 1px solid #d1d5db;
    border-radius: 0.5rem;
    background: white;
    cursor: pointer;
    text-align: left;
    transition: all 0.2s;
    font-weight: 500;
  }

  .action-btn:hover:not(:disabled) {
    background-color: #f9fafb;
    border-color: #9ca3af;
  }

  .action-btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  .info-grid {
    display: flex;
    flex-direction: column;
    gap: 1rem;
  }

  .info-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .info-label {
    font-weight: 500;
    color: #6b7280;
  }

  .info-value {
    font-weight: 600;
    color: #1f2937;
  }
</style>