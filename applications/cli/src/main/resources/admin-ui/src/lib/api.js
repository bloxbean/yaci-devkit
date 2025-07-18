// API client for Yaci DevKit admin endpoints
const BASE_URL = '/local-cluster/api/admin';
const API_BASE_URL = '/local-cluster/api';

class ApiClient {
    constructor() {
        this.baseURL = BASE_URL;
        this.apiBaseURL = API_BASE_URL;
    }

    async request(endpoint, options = {}, useApiBase = false) {
        const url = useApiBase ? `${this.apiBaseURL}${endpoint}` : `${this.baseURL}${endpoint}`;
        const config = {
            headers: {
                'Content-Type': 'application/json',
                ...options.headers
            },
            ...options
        };

        try {
            const response = await fetch(url, config);
            
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                return await response.json();
            }
            
            return await response.text();
        } catch (error) {
            console.error('API request failed:', error);
            throw error;
        }
    }

    // DevNet operations
    async getDevnetInfo() {
        return this.request('/devnet');
    }

    async getDevnetStatus() {
        return this.request('/devnet/status');
    }

    async getDevnetTip() {
        return this.request('/devnet/tip');
    }

    async resetDevnet() {
        return this.request('/devnet/reset', { method: 'POST' });
    }

    async createDevnet(config = {}) {
        return this.request('/devnet/create', {
            method: 'POST',
            body: JSON.stringify(config)
        });
    }

    async getKesPeriod() {
        return this.request('/devnet/kes-period');
    }

    async getGenesisHash() {
        return this.request('/devnet/genesis/hash');
    }

    // Yaci Store operations
    async startYaciStore() {
        return this.request('/yaci-store/start', { method: 'POST' });
    }

    async stopYaciStore() {
        return this.request('/yaci-store/stop', { method: 'POST' });
    }

    async resyncYaciStore() {
        return this.request('/yaci-store/resync', { method: 'POST' });
    }

    // Ogmios operations
    async startOgmios() {
        return this.request('/ogmios/start-ogmios', { method: 'POST' });
    }

    async startKupomios() {
        return this.request('/ogmios/start-kupomios', { method: 'POST' });
    }

    async stopOgmios() {
        return this.request('/ogmios/stop', { method: 'POST' });
    }

    // Epochs and protocol
    async getLatestEpoch() {
        return this.request('/epochs/latest', {}, true);
    }

    async getProtocolParameters() {
        return this.request('/epochs/parameters', {}, true);
    }

    // Address operations
    async topupAddress(address, amount) {
        return this.request('/addresses/topup', {
            method: 'POST',
            body: JSON.stringify({ address, amount })
        }, true);
    }

    async getAddressUtxos(address) {
        return this.request(`/addresses/${address}/utxos`, {}, true);
    }

    // Downloads
    async downloadDevnetFiles() {
        window.open(`${this.baseURL}/devnet/download`);
    }

    async downloadGenesisFiles() {
        window.open(`${this.baseURL}/devnet/genesis/download`);
    }
}

export const api = new ApiClient();