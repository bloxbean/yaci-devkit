/**
 * Yaci DevKit CIP-30 Wallet SDK
 *
 * Thin wrapper that proxies CIP-30 calls to CLI backend.
 * All CBOR encoding/serialization is handled by the CLI using cardano-client-lib.
 * NO client-side CBOR library required.
 *
 * CIP-30 spec: https://cips.cardano.org/cip/CIP-0030
 *
 * @version 4.0.0
 * @author Yaci DevKit Team
 */
(function() {
    'use strict';

    // Configuration
    const CLI_URL = 'http://localhost:10000';
    const SDK_VERSION = '4.0.0';
    const API_VERSION = '1';

    // Logging utility
    const log = {
        info: (msg, ...args) => console.log(`[YaciDevKit] ${msg}`, ...args),
        warn: (msg, ...args) => console.warn(`[YaciDevKit] ${msg}`, ...args),
        error: (msg, ...args) => console.error(`[YaciDevKit] ${msg}`, ...args),
        debug: (msg, ...args) => console.debug(`[YaciDevKit] ${msg}`, ...args)
    };

    // ==================== CIP-30 Error Types ====================

    /**
     * CIP-30 APIError
     * @see https://cips.cardano.org/cip/CIP-0030#apierror
     */
    class APIError extends Error {
        static InvalidRequest = -1;
        static InternalError = -2;
        static Refused = -3;
        static AccountChange = -4;

        constructor(code, info) {
            super(info);
            this.name = 'APIError';
            this.code = code;
            this.info = info;
        }
    }

    /**
     * CIP-30 TxSignError
     * @see https://cips.cardano.org/cip/CIP-0030#txsignerror
     */
    class TxSignError extends Error {
        static ProofGeneration = 1;
        static UserDeclined = 2;

        constructor(code, info) {
            super(info);
            this.name = 'TxSignError';
            this.code = code;
            this.info = info;
        }
    }

    /**
     * CIP-30 TxSendError
     * @see https://cips.cardano.org/cip/CIP-0030#txsenderror
     */
    class TxSendError extends Error {
        static Refused = 1;
        static Failure = 2;

        constructor(code, info) {
            super(info);
            this.name = 'TxSendError';
            this.code = code;
            this.info = info;
        }
    }

    /**
     * CIP-30 DataSignError
     * @see https://cips.cardano.org/cip/CIP-0030#datasignerror
     */
    class DataSignError extends Error {
        static ProofGeneration = 1;
        static AddressNotPK = 2;
        static UserDeclined = 3;

        constructor(code, info) {
            super(info);
            this.name = 'DataSignError';
            this.code = code;
            this.info = info;
        }
    }

    /**
     * CIP-30 PaginateError
     * @see https://cips.cardano.org/cip/CIP-0030#paginateerror
     */
    class PaginateError extends Error {
        constructor(maxSize) {
            super(`Pagination limit exceeded. Max size: ${maxSize}`);
            this.name = 'PaginateError';
            this.maxSize = maxSize;
        }
    }

    /**
     * CIP-30 Wallet API implementation
     * All data fetching and CBOR encoding is done by the CLI backend
     */
    class YaciWalletAPI {
        constructor(accountId = '0', enabledExtensions = []) {
            this._accountId = accountId;
            this._accounts = [];
            this._enabledExtensions = enabledExtensions;
        }

        /**
         * Initialize wallet API by loading accounts
         */
        async initialize() {
            try {
                const response = await this._apiCall('/accounts');
                this._accounts = response.accounts || [];
                log.info(`Loaded ${this._accounts.length} accounts`);
            } catch (error) {
                log.error('Failed to load accounts:', error);
                this._accounts = [];
            }
        }

        /**
         * Make API call to CLI
         */
        async _apiCall(endpoint, options = {}) {
            const url = CLI_URL + '/api/v1/wallet' + endpoint;

            const defaultOptions = {
                headers: {
                    'Accept': 'application/json, text/plain, */*'
                }
            };

            // Only add Content-Type for requests with a body (POST, PUT, etc.)
            // Setting Content-Type: application/json on GET requests triggers CORS preflight in Chrome
            if (options.method && options.method !== 'GET') {
                defaultOptions.headers['Content-Type'] = 'application/json';
            }

            const requestOptions = {
                ...defaultOptions,
                ...options,
                headers: {
                    ...defaultOptions.headers,
                    ...options.headers
                }
            };

            try {
                log.debug('API call:', options.method || 'GET', url);
                const response = await fetch(url, requestOptions);

                if (!response.ok) {
                    // Try to parse structured error from backend
                    let errorBody = null;
                    const errorText = await response.text();
                    try { errorBody = JSON.parse(errorText); } catch (_) { /* not JSON */ }

                    const err = new APIError(
                        (errorBody && errorBody.code) || APIError.InternalError,
                        (errorBody && errorBody.info) || `API call failed: ${response.status} - ${errorText}`
                    );
                    err._raw = errorBody;
                    throw err;
                }

                const contentType = response.headers.get('content-type');
                if (contentType && contentType.includes('application/json')) {
                    return await response.json();
                } else {
                    return await response.text();
                }
            } catch (error) {
                if (error instanceof APIError) throw error;
                log.error('API call failed:', error);
                throw new APIError(APIError.InternalError, error.message);
            }
        }

        /**
         * Get extensions enabled for this session (CIP-30)
         * @returns {Promise<Extension[]>}
         */
        async getExtensions() {
            return this._enabledExtensions;
        }

        /**
         * Get network ID (CIP-30)
         * @returns {Promise<number>} Network ID (0 = testnet)
         */
        async getNetworkId() {
            const resp = await this._apiCall('/cip30/network-id');
            return resp;
        }

        /**
         * Get wallet balance (CIP-30)
         * @returns {Promise<string>} CBOR hex of Value type
         */
        async getBalance() {
            const resp = await this._apiCall(`/cip30/${this._accountId}/balance`);
            return resp;
        }

        /**
         * Get UTXOs (CIP-30)
         * @param {string} [amount] - Optional CBOR-encoded Value specifying minimum amount
         * @param {Object} [paginate] - Optional pagination {page, limit}
         * @returns {Promise<string[]|null>} Array of CBOR hex strings, or null if amount cannot be met
         */
        async getUtxos(amount = undefined, paginate = undefined) {
            const params = new URLSearchParams();
            if (amount !== undefined && amount !== null) {
                params.set('amount', amount);
            }
            if (paginate) {
                params.set('page', paginate.page || 0);
                params.set('limit', paginate.limit || 50);
            }
            const queryString = params.toString();
            const endpoint = `/cip30/${this._accountId}/utxos${queryString ? '?' + queryString : ''}`;
            const resp = await this._apiCall(endpoint);

            // CIP-30: if amount was specified and we got an empty array, return null
            // (meaning the wallet cannot satisfy the requested amount)
            if (amount !== undefined && amount !== null && Array.isArray(resp) && resp.length === 0) {
                return null;
            }

            return resp;
        }

        /**
         * Get used addresses (CIP-30)
         * @param {Object} [paginate] - Optional pagination {page, limit}
         * @returns {Promise<string[]>} Array of address bytes as hex
         */
        async getUsedAddresses(paginate = undefined) {
            const resp = await this._apiCall(`/cip30/${this._accountId}/used-addresses`);

            // Apply client-side pagination if requested
            if (paginate && Array.isArray(resp)) {
                const page = paginate.page || 0;
                const limit = paginate.limit || resp.length;
                const start = page * limit;
                return resp.slice(start, start + limit);
            }

            return resp;
        }

        /**
         * Get unused addresses (CIP-30)
         * @returns {Promise<string[]>} Array of address bytes as hex
         */
        async getUnusedAddresses() {
            // For dev wallet, all addresses are considered used
            return [];
        }

        /**
         * Get change address (CIP-30)
         * @returns {Promise<string>} Address bytes as hex
         */
        async getChangeAddress() {
            const resp = await this._apiCall(`/cip30/${this._accountId}/change-address`);
            return resp;
        }

        /**
         * Get reward addresses (CIP-30)
         * @returns {Promise<string[]>} Array of stake address bytes as hex
         */
        async getRewardAddresses() {
            const resp = await this._apiCall(`/cip30/${this._accountId}/reward-addresses`);
            return resp;
        }

        /**
         * Get collateral UTXOs (CIP-30)
         * @param {Object} [params] - Optional {amount: cbor<Coin>}
         * @returns {Promise<string[]|null>} Array of CBOR hex strings, or null if unavailable
         */
        async getCollateral(_params = undefined) {
            try {
                const resp = await this._apiCall(`/cip30/${this._accountId}/collateral`);
                // Backend returns null if no suitable collateral found
                if (resp === null || resp === '' || resp === 'null') {
                    return null;
                }
                if (Array.isArray(resp) && resp.length === 0) {
                    return null;
                }
                return resp;
            } catch (error) {
                log.warn('Failed to get collateral:', error.message);
                return null;
            }
        }

        /**
         * Sign transaction (CIP-30)
         * @param {string} txCbor - Transaction CBOR hex
         * @param {boolean} [partialSign=false] - Allow partial signing
         * @returns {Promise<string>} Witness set CBOR hex
         * @throws {TxSignError}
         */
        async signTx(txCbor, partialSign = false) {
            log.info('Signing transaction (partial:', partialSign, ')');

            try {
                const result = await this._apiCall('/sign-tx', {
                    method: 'POST',
                    body: JSON.stringify({
                        txCbor: txCbor,
                        accountId: this._accountId,
                        partialSign: partialSign
                    })
                });

                if (!result || !result.witnessSet) {
                    throw new TxSignError(
                        TxSignError.ProofGeneration,
                        'Failed to generate transaction witness'
                    );
                }

                return result.witnessSet;
            } catch (error) {
                if (error instanceof TxSignError) throw error;
                throw new TxSignError(TxSignError.ProofGeneration, error.message);
            }
        }

        /**
         * Sign data (CIP-30 / CIP-8)
         * Returns proper COSE_Sign1 signature and COSE_Key per CIP-30 spec.
         * @param {string} address - Address hex
         * @param {string} payload - Payload hex
         * @returns {Promise<{signature: string, key: string}>} DataSignature with COSE_Sign1 and COSE_Key
         * @throws {DataSignError}
         */
        async signData(address, payload) {
            log.info('Signing data for address:', address);

            try {
                const result = await this._apiCall('/sign-data', {
                    method: 'POST',
                    body: JSON.stringify({
                        address: address,
                        payload: payload,
                        accountId: this._accountId
                    })
                });

                if (!result || !result.signature || !result.key) {
                    throw new DataSignError(
                        DataSignError.ProofGeneration,
                        'Failed to generate data signature'
                    );
                }

                return result;
            } catch (error) {
                if (error instanceof DataSignError) throw error;
                throw new DataSignError(DataSignError.ProofGeneration, error.message);
            }
        }

        /**
         * Submit transaction (CIP-30)
         * Proxies through CLI backend to avoid hardcoded Yaci Store port.
         * @param {string} txCbor - Signed transaction CBOR hex
         * @returns {Promise<string>} Transaction hash
         * @throws {TxSendError}
         */
        async submitTx(txCbor) {
            log.info('Submitting transaction');

            try {
                const response = await fetch(CLI_URL + '/api/v1/wallet/submit-tx', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ txCbor: txCbor })
                });

                const result = await response.text();

                if (!response.ok) {
                    throw new TxSendError(TxSendError.Failure, result || 'Transaction submission failed');
                }

                log.info('Transaction submitted:', result);
                return result;
            } catch (error) {
                if (error instanceof TxSendError) throw error;
                throw new TxSendError(TxSendError.Failure, error.message);
            }
        }

        // ==================== Utility Methods (not part of CIP-30) ====================

        /**
         * Select a different account by index or ID
         * @param {number|string} indexOrId - Account index or ID
         */
        selectAccount(indexOrId) {
            if (typeof indexOrId === 'number') {
                this._accountId = String(indexOrId);
            } else {
                this._accountId = indexOrId;
            }
            log.info('Selected account:', this._accountId);
        }

        /**
         * Get current account ID
         * @returns {string}
         */
        getAccountId() {
            return this._accountId;
        }

        /**
         * Get all accounts
         * @returns {Object[]}
         */
        getAccounts() {
            return this._accounts;
        }

        /**
         * Decode CBOR address to bech32 (for display)
         * @param {string} cborHex - Address bytes as hex
         * @returns {Promise<string>} Bech32 address
         */
        async decodeAddress(cborHex) {
            const resp = await this._apiCall(`/decode/address?cborHex=${encodeURIComponent(cborHex)}`);
            return resp;
        }

        /**
         * Decode CBOR value to JSON (for display)
         * @param {string} cborHex - Value CBOR as hex
         * @returns {Promise<{lovelace: string, assets?: Object[]}>}
         */
        async decodeValue(cborHex) {
            const resp = await this._apiCall(`/decode/value?cborHex=${encodeURIComponent(cborHex)}`);
            return resp;
        }
    }

    /**
     * Main SDK class that manages wallet initialization and injection
     */
    class YaciDevKitSDK {
        constructor() {
            this.isReady = false;
            this.walletApi = null;
            this._enabled = false;
            this.init();
        }

        async init() {
            try {
                log.info('Initializing Yaci DevKit CIP-30 SDK v' + SDK_VERSION);

                if (!this.isDevelopmentEnvironment()) {
                    log.warn('Not in development environment, SDK will not be injected. ' +
                        'Set window.YACI_DEVKIT_FORCE_INJECT = true before loading to override.');
                    return;
                }

                const isAvailable = await this.checkDevKitAvailability();
                if (!isAvailable) {
                    log.warn('Yaci DevKit not available');
                    return;
                }

                this.injectWallet();
                this.isReady = true;
                log.info('SDK initialized successfully');

                // Dispatch ready event
                window.dispatchEvent(new CustomEvent('yacidevkit:ready', {
                    detail: { version: SDK_VERSION }
                }));

            } catch (error) {
                log.error('Failed to initialize SDK:', error);
            }
        }

        /**
         * Check if running in development environment.
         * Can be overridden by setting window.YACI_DEVKIT_FORCE_INJECT = true
         */
        isDevelopmentEnvironment() {
            // Allow explicit override for staging/testing environments
            if (window.YACI_DEVKIT_FORCE_INJECT === true) {
                return true;
            }

            const hostname = window.location.hostname;
            const port = window.location.port;

            return hostname === 'localhost' ||
                   hostname === '127.0.0.1' ||
                   hostname.endsWith('.local') ||
                   port !== '';
        }

        /**
         * Check if Yaci DevKit CLI is available
         */
        async checkDevKitAvailability() {
            try {
                const response = await fetch(`${CLI_URL}/api/v1/wallet/health`, {
                    method: 'GET',
                    headers: { 'Accept': 'application/json' }
                });
                return response.ok;
            } catch (error) {
                log.debug('DevKit availability check failed:', error.message);
                return false;
            }
        }

        /**
         * Inject wallet into window.cardano (CIP-30 initial API)
         */
        injectWallet() {
            if (!window.cardano) {
                window.cardano = {};
            }

            const self = this;

            window.cardano.yacidevkit = {
                name: 'Yaci DevKit',
                icon: this.getWalletIcon(),
                apiVersion: API_VERSION,
                supportedExtensions: [],

                /**
                 * CIP-30: isEnabled()
                 * Returns true if the dApp has previously been granted access
                 */
                isEnabled: () => Promise.resolve(self._enabled),

                /**
                 * CIP-30: enable(options?)
                 * Request access to the wallet. Returns the full API object.
                 * @param {Object} [options] - { extensions: [{cip: number}] }
                 */
                enable: async (options) => {
                    log.info('Wallet enable requested', options || '');

                    // Negotiate extensions
                    let enabledExtensions = [];
                    if (options && options.extensions && Array.isArray(options.extensions)) {
                        // Filter to only extensions we support (none currently)
                        enabledExtensions = options.extensions.filter(ext => {
                            const supported = false; // No extensions supported yet
                            if (!supported) {
                                log.warn(`Extension CIP-${ext.cip} requested but not supported`);
                            }
                            return supported;
                        });
                    }

                    // Fetch active account from backend
                    let activeAccountId = '0';
                    try {
                        const resp = await fetch(CLI_URL + '/api/v1/wallet/active-account');
                        if (resp.ok) {
                            const data = await resp.json();
                            activeAccountId = data.accountId || '0';
                        }
                    } catch (e) {
                        log.warn('Could not fetch active account, using default:', e.message);
                    }

                    if (!self.walletApi || self.walletApi._accountId !== activeAccountId) {
                        self.walletApi = new YaciWalletAPI(activeAccountId, enabledExtensions);
                        await self.walletApi.initialize();
                    } else {
                        // Update extensions on re-enable
                        self.walletApi._enabledExtensions = enabledExtensions;
                    }

                    self._enabled = true;
                    return self.walletApi;
                }
            };

            log.info('Wallet injected as window.cardano.yacidevkit');
        }

        /**
         * Get base64 encoded wallet icon
         */
        getWalletIcon() {
            const svg = `
                <svg width="48" height="48" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <circle cx="24" cy="24" r="24" fill="#667eea"/>
                    <path d="M18 18h12v4H18zM16 24h16v4H16zM20 30h8v4h-8z" fill="white"/>
                </svg>
            `;
            return 'data:image/svg+xml;base64,' + btoa(svg);
        }
    }

    // Expose CIP-30 error types globally for dApp error handling
    window.YaciDevKitErrors = {
        APIError,
        TxSignError,
        TxSendError,
        DataSignError,
        PaginateError
    };

    // Error handling for unhandled promises
    window.addEventListener('unhandledrejection', function(event) {
        if (event.reason && event.reason.message && event.reason.message.includes('YaciDevKit')) {
            log.error('Unhandled promise rejection in SDK:', event.reason);
        }
    });

    // Auto-initialize SDK
    const sdk = new YaciDevKitSDK();

    // Expose SDK for debugging
    window.yaciDevKitSDK = sdk;

    log.info('Yaci DevKit CIP-30 SDK v' + SDK_VERSION + ' loaded');

})();
