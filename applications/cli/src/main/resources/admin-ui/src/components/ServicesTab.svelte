<script>
  import { onMount } from 'svelte';
  import { services } from '../lib/stores.js';
  import { api } from '../lib/api.js';
  import { addNotification } from '../lib/stores.js';

  let servicesInfo = [
    {
      id: 'yaciStore',
      name: 'Yaci Store',
      description: 'Cardano blockchain indexer and query service',
      port: 8080,
      icon: '🗃️',
      actions: [
        { name: 'Start', action: 'start', variant: 'primary' },
        { name: 'Stop', action: 'stop', variant: 'secondary' },
        { name: 'Resync', action: 'resync', variant: 'warning' }
      ]
    },
    {
      id: 'ogmios',
      name: 'Ogmios',
      description: 'JSON-RPC bridge for Cardano',
      port: 1337,
      icon: '🌉',
      actions: [
        { name: 'Start', action: 'start', variant: 'primary' },
        { name: 'Stop', action: 'stop', variant: 'secondary' }
      ]
    },
    {
      id: 'kupo',
      name: 'Kupo',
      description: 'Chain indexer for Cardano',
      port: 1442,
      icon: '🔍',
      actions: [
        { name: 'Start', action: 'start', variant: 'primary' },
        { name: 'Stop', action: 'stop', variant: 'secondary' }
      ]
    },
    {
      id: 'submitApi',
      name: 'Submit API',
      description: 'Transaction submission service',
      port: 8090,
      icon: '📤',
      actions: []
    }
  ];

  let actionInProgress = {};

  onMount(() => {
    // Services info is reactive through the store
  });

  async function executeAction(serviceId, action) {
    const actionKey = `${serviceId}_${action}`;
    actionInProgress[actionKey] = true;
    
    try {
      let result;
      const serviceName = servicesInfo.find(s => s.id === serviceId)?.name || serviceId;
      
      switch (serviceId) {
        case 'yaciStore':
          switch (action) {
            case 'start':
              result = await api.startYaciStore();
              break;
            case 'stop':
              result = await api.stopYaciStore();
              break;
            case 'resync':
              result = await api.resyncYaciStore();
              break;
          }
          break;
        case 'ogmios':
          switch (action) {
            case 'start':
              result = await api.startOgmios();
              break;
            case 'stop':
              result = await api.stopOgmios();
              break;
          }
          break;
        case 'kupo':
          switch (action) {
            case 'start':
              result = await api.startKupomios();
              break;
            case 'stop':
              result = await api.stopOgmios();
              break;
          }
          break;
      }
      
      addNotification(`${serviceName} ${action} completed successfully`, 'success');
      
      // Update service status optimistically
      if (action === 'start') {
        services.update(s => ({
          ...s,
          [serviceId]: { ...s[serviceId], running: true }
        }));
      } else if (action === 'stop') {
        services.update(s => ({
          ...s,
          [serviceId]: { ...s[serviceId], running: false }
        }));
      }
      
    } catch (error) {
      addNotification(`Failed to ${action} ${serviceId}: ${error.message}`, 'error');
    } finally {
      actionInProgress[actionKey] = false;
    }
  }

  function getServiceStatus(serviceId) {
    const service = $services[serviceId];
    if (!service || typeof service !== 'object') return false;
    return service.running;
  }

  function getServicePort(serviceId) {
    const service = $services[serviceId];
    if (!service || typeof service !== 'object') return null;
    return service.port;
  }
</script>

<div class="services-tab">
  <div class="tab-header">
    <h2>Services Management</h2>
    <div class="services-summary">
      {Object.values($services).filter(s => typeof s === 'object' && s.running).length} / {Object.keys($services).length - 1} Running
    </div>
  </div>

  <div class="services-grid">
    {#each servicesInfo as service}
      <div class="service-card">
        <div class="service-header">
          <div class="service-title">
            <span class="service-icon">{service.icon}</span>
            <div>
              <h3>{service.name}</h3>
              <p class="service-description">{service.description}</p>
            </div>
          </div>
          <div class="service-status">
            <div class="status-indicator {getServiceStatus(service.id) ? 'running' : 'stopped'}">
              {getServiceStatus(service.id) ? 'Running' : 'Stopped'}
            </div>
          </div>
        </div>
        
        <div class="service-info">
          <div class="info-row">
            <span class="info-label">Port:</span>
            <span class="info-value">:{getServicePort(service.id) || service.port}</span>
          </div>
          <div class="info-row">
            <span class="info-label">Status:</span>
            <span class="info-value status-text {getServiceStatus(service.id) ? 'running' : 'stopped'}">
              {getServiceStatus(service.id) ? '✓ Active' : '✗ Inactive'}
            </span>
          </div>
        </div>

        {#if service.actions && service.actions.length > 0}
          <div class="service-actions">
            {#each service.actions as action}
              <button
                class="action-btn {action.variant}"
                on:click={() => executeAction(service.id, action.action)}
                disabled={actionInProgress[`${service.id}_${action.action}`] || $services.loading}
              >
                {#if actionInProgress[`${service.id}_${action.action}`]}
                  <span class="spinner"></span>
                {:else}
                  {action.name}
                {/if}
              </button>
            {/each}
          </div>
        {/if}
      </div>
    {/each}
  </div>

  <div class="service-logs">
    <div class="logs-header">
      <h3>Service Logs</h3>
      <div class="logs-controls">
        <button class="btn-small secondary">Clear</button>
        <button class="btn-small secondary">Download</button>
      </div>
    </div>
    <div class="logs-content">
      <div class="log-placeholder">
        <p>Service logs will appear here when available.</p>
        <p class="help-text">Start services to see real-time logs and status updates.</p>
      </div>
    </div>
  </div>
</div>

<style>
  .services-tab {
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

  .services-summary {
    font-size: 0.875rem;
    color: #6b7280;
    font-weight: 500;
    background-color: #f3f4f6;
    padding: 0.5rem 1rem;
    border-radius: 0.5rem;
  }

  .services-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
    gap: 1.5rem;
    margin-bottom: 2rem;
  }

  .service-card {
    background: white;
    border-radius: 0.75rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    overflow: hidden;
    border: 1px solid #e5e7eb;
  }

  .service-header {
    padding: 1.5rem;
    border-bottom: 1px solid #f3f4f6;
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
  }

  .service-title {
    display: flex;
    align-items: flex-start;
    gap: 0.75rem;
  }

  .service-icon {
    font-size: 1.5rem;
    margin-top: 0.25rem;
  }

  .service-title h3 {
    margin: 0;
    font-size: 1.125rem;
    font-weight: 600;
    color: #1f2937;
  }

  .service-description {
    margin: 0.25rem 0 0 0;
    font-size: 0.875rem;
    color: #6b7280;
  }

  .service-status {
    flex-shrink: 0;
  }

  .status-indicator {
    padding: 0.25rem 0.75rem;
    border-radius: 1rem;
    font-size: 0.75rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  .status-indicator.running {
    background-color: #d1fae5;
    color: #065f46;
  }

  .status-indicator.stopped {
    background-color: #fee2e2;
    color: #991b1b;
  }

  .service-info {
    padding: 1rem 1.5rem;
    background-color: #f9fafb;
    border-bottom: 1px solid #f3f4f6;
  }

  .info-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 0.5rem;
  }

  .info-row:last-child {
    margin-bottom: 0;
  }

  .info-label {
    font-size: 0.875rem;
    color: #6b7280;
    font-weight: 500;
  }

  .info-value {
    font-size: 0.875rem;
    color: #1f2937;
    font-weight: 600;
  }

  .info-value.status-text.running {
    color: #065f46;
  }

  .info-value.status-text.stopped {
    color: #991b1b;
  }

  .service-actions {
    padding: 1rem 1.5rem;
    display: flex;
    gap: 0.5rem;
    flex-wrap: wrap;
  }

  .action-btn {
    padding: 0.5rem 1rem;
    border: none;
    border-radius: 0.375rem;
    cursor: pointer;
    font-weight: 500;
    font-size: 0.875rem;
    transition: all 0.2s;
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }

  .action-btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  .action-btn.primary {
    background-color: #10b981;
    color: white;
  }

  .action-btn.primary:hover:not(:disabled) {
    background-color: #059669;
  }

  .action-btn.secondary {
    background-color: #6b7280;
    color: white;
  }

  .action-btn.secondary:hover:not(:disabled) {
    background-color: #4b5563;
  }

  .action-btn.warning {
    background-color: #f59e0b;
    color: white;
  }

  .action-btn.warning:hover:not(:disabled) {
    background-color: #d97706;
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

  .service-logs {
    background: white;
    border-radius: 0.75rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    overflow: hidden;
  }

  .logs-header {
    padding: 1.5rem;
    border-bottom: 1px solid #f3f4f6;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .logs-header h3 {
    margin: 0;
    font-size: 1.125rem;
    font-weight: 600;
    color: #1f2937;
  }

  .logs-controls {
    display: flex;
    gap: 0.5rem;
  }

  .btn-small {
    padding: 0.25rem 0.75rem;
    border: 1px solid #d1d5db;
    border-radius: 0.375rem;
    background: white;
    cursor: pointer;
    font-size: 0.875rem;
    font-weight: 500;
    transition: all 0.2s;
  }

  .btn-small:hover {
    background-color: #f9fafb;
    border-color: #9ca3af;
  }

  .logs-content {
    padding: 1.5rem;
    min-height: 200px;
  }

  .log-placeholder {
    text-align: center;
    color: #6b7280;
    padding: 2rem;
  }

  .help-text {
    font-size: 0.875rem;
    margin-top: 0.5rem;
  }
</style>