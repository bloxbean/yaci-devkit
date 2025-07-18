<script>
  import { onMount } from 'svelte';
  import { writable } from 'svelte/store';
  import Dashboard from './components/Dashboard.svelte';
  import NetworkTab from './components/NetworkTab.svelte';
  import ServicesTab from './components/ServicesTab.svelte';
  import UtilitiesTab from './components/UtilitiesTab.svelte';
  import Notifications from './components/Notifications.svelte';
  import { activeTab, networkStatus, services, notifications } from './lib/stores.js';
  import { api } from './lib/api.js';
  
  const loading = writable(true);
  const error = writable(null);

  onMount(async () => {
    console.log('App component onMount');
    
    try {
      console.log('Loading initial data...');
      await loadInitialData();
      console.log('Loading services...');
      await loadServices();
      console.log('All data loaded successfully');
    } catch (e) {
      console.error('Error loading data:', e);
      error.set(e.message);
    }
    
    console.log('Setting loading to false');
    loading.set(false);
  });

  async function loadInitialData() {
    try {
      const [status, tip] = await Promise.all([
        api.getDevnetStatus(),
        api.getDevnetTip()
      ]);
      
      console.log('DevNet status:', status);
      console.log('DevNet tip:', tip);
      
      networkStatus.update(s => ({
        ...s,
        initialized: status === 'initialized' || status?.initialized || false,
        tip: tip,
        loading: false
      }));
    } catch (err) {
      console.error('Failed to load initial data:', err);
      networkStatus.update(s => ({
        ...s,
        error: err.message,
        loading: false
      }));
      throw err;
    }
  }

  async function loadServices() {
    services.update(s => ({ ...s, loading: true }));
    // TODO: Implement service status checking
    services.update(s => ({ ...s, loading: false }));
  }

  function handleTabChange(tabName) {
    activeTab.set(tabName);
  }
</script>

<main class="app">
  {#if $loading}
    <div class="loading">
      <div class="spinner"></div>
      <p>Loading Yaci DevKit...</p>
    </div>
  {:else if $error}
    <div class="error">
      <h2>Error</h2>
      <p>{$error}</p>
    </div>
  {:else}
    <div class="container">
      <header>
        <h1>Yaci DevKit Admin</h1>
        <div class="network-status" class:active={$networkStatus.initialized}>
          <span class="status-dot"></span>
          <span>{$networkStatus.initialized ? 'Network Running' : 'Network Stopped'}</span>
        </div>
      </header>

      <nav class="tabs">
        <button 
          class="tab" 
          class:active={$activeTab === 'dashboard'}
          on:click={() => handleTabChange('dashboard')}
        >
          Dashboard
        </button>
        <button 
          class="tab" 
          class:active={$activeTab === 'network'}
          on:click={() => handleTabChange('network')}
        >
          Network
        </button>
        <button 
          class="tab" 
          class:active={$activeTab === 'services'}
          on:click={() => handleTabChange('services')}
        >
          Services
        </button>
        <button 
          class="tab" 
          class:active={$activeTab === 'utilities'}
          on:click={() => handleTabChange('utilities')}
        >
          Utilities
        </button>
      </nav>

      <div class="tab-content">
        {#if $activeTab === 'dashboard'}
          <Dashboard />
        {:else if $activeTab === 'network'}
          <NetworkTab />
        {:else if $activeTab === 'services'}
          <ServicesTab />
        {:else if $activeTab === 'utilities'}
          <UtilitiesTab />
        {/if}
      </div>

      <Notifications />
    </div>
  {/if}
</main>

<style>
  :global(body) {
    margin: 0;
    padding: 0;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen', 'Ubuntu', 'Cantarell', sans-serif;
    background-color: #f5f5f5;
    color: #333;
  }

  :global(*) {
    box-sizing: border-box;
  }

  .app {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
  }

  .loading {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    height: 100vh;
    gap: 1rem;
  }

  .spinner {
    width: 40px;
    height: 40px;
    border: 3px solid #f3f3f3;
    border-top: 3px solid #3498db;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }

  @keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
  }

  .error {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    height: 100vh;
    color: #e74c3c;
  }

  .container {
    flex: 1;
    display: flex;
    flex-direction: column;
  }

  header {
    background: white;
    padding: 1.5rem 2rem;
    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  h1 {
    margin: 0;
    color: #2c3e50;
    font-size: 1.75rem;
  }

  .network-status {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.5rem 1rem;
    background: #f8f9fa;
    border-radius: 20px;
    font-size: 0.875rem;
  }

  .network-status.active .status-dot {
    background: #27ae60;
  }

  .status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #e74c3c;
  }

  .tabs {
    display: flex;
    background: white;
    border-bottom: 1px solid #e0e0e0;
    padding: 0 2rem;
  }

  .tab {
    padding: 1rem 1.5rem;
    background: none;
    border: none;
    border-bottom: 2px solid transparent;
    cursor: pointer;
    font-size: 0.9rem;
    color: #666;
    transition: all 0.2s;
  }

  .tab:hover {
    color: #333;
  }

  .tab.active {
    color: #3498db;
    border-bottom-color: #3498db;
  }

  .tab-content {
    flex: 1;
    padding: 2rem;
    overflow-y: auto;
  }
</style>