import { writable, derived } from 'svelte/store';

// Network state
export const networkStatus = writable({
    initialized: false,
    tip: null,
    epoch: null,
    slot: null,
    blockHeight: null,
    era: 'Conway',
    loading: true,
    error: null
});

// Services state
export const services = writable({
    yaciStore: { running: false, port: 8080 },
    ogmios: { running: false, port: 1337 },
    kupo: { running: false, port: 1442 },
    submitApi: { running: false, port: 8090 },
    loading: false
});

// DevNet configuration
export const devnetConfig = writable({
    loading: false,
    error: null,
    info: null
});

// Current active tab
export const activeTab = writable('dashboard');

// Notifications
export const notifications = writable([]);

// WebSocket connection status
export const connectionStatus = writable({
    connected: false,
    reconnecting: false,
    error: null
});

// Derived stores
export const isNetworkRunning = derived(
    networkStatus,
    $networkStatus => $networkStatus.initialized && !$networkStatus.loading
);

export const runningServicesCount = derived(
    services,
    $services => Object.values($services).filter(service => 
        typeof service === 'object' && service.running
    ).length
);

// Utility functions for stores
export function addNotification(message, type = 'info', duration = 5000) {
    const id = Date.now();
    const notification = { id, message, type, duration };
    
    notifications.update(n => [...n, notification]);
    
    if (duration > 0) {
        setTimeout(() => {
            removeNotification(id);
        }, duration);
    }
}

export function removeNotification(id) {
    notifications.update(n => n.filter(notification => notification.id !== id));
}

export function updateNetworkStatus(updates) {
    networkStatus.update(status => ({
        ...status,
        ...updates,
        loading: false
    }));
}

export function updateServiceStatus(serviceName, status) {
    services.update(s => ({
        ...s,
        [serviceName]: { ...s[serviceName], ...status }
    }));
}