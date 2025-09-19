package com.bloxbean.cardano.yacicli.cip30.service;

import com.bloxbean.cardano.yacicli.cip30.config.WalletConfigurationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "yaci.devkit.wallet", name = "enabled", havingValue = "true", matchIfMissing = true)
public class YaciStoreProxyService {
    
    private final WalletConfigurationProperties walletConfig;
    private final RestTemplate restTemplate;
    
    // Circuit breaker state
    private final AtomicBoolean circuitOpen = new AtomicBoolean(false);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final AtomicLong consecutiveFailures = new AtomicLong(0);
    
    // Health check cache
    private final Map<String, Object> healthCache = new ConcurrentHashMap<>();
    private volatile long lastHealthCheck = 0;
    private static final long HEALTH_CACHE_TTL = 30000; // 30 seconds
    
    // Circuit breaker configuration
    private static final long CIRCUIT_BREAKER_TIMEOUT = 60000; // 1 minute
    private static final int FAILURE_THRESHOLD = 5;
    
    public YaciStoreProxyService(WalletConfigurationProperties walletConfig) {
        this.walletConfig = walletConfig;
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * Proxy GET request to Yaci Store
     */
    public <T> Optional<T> proxyGetRequest(String endpoint, Class<T> responseType) {
        return proxyGetRequest(endpoint, responseType, null);
    }
    
    /**
     * Proxy GET request to Yaci Store with custom headers
     */
    public <T> Optional<T> proxyGetRequest(String endpoint, Class<T> responseType, HttpHeaders headers) {
        if (isCircuitOpen()) {
            log.debug("Circuit breaker is open, skipping Yaci Store request to: {}", endpoint);
            return Optional.empty();
        }
        
        try {
            String url = buildUrl(endpoint);
            log.debug("Proxying GET request to Yaci Store: {}", url);
            
            HttpHeaders requestHeaders = headers != null ? headers : new HttpHeaders();
            HttpEntity<?> entity = new HttpEntity<>(requestHeaders);
            
            ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET, entity, responseType);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                resetCircuitBreaker();
                return Optional.ofNullable(response.getBody());
            } else {
                log.warn("Non-success response from Yaci Store: {} - {}", response.getStatusCode(), url);
                return Optional.empty();
            }
            
        } catch (RestClientException e) {
            handleFailure(endpoint, e);
            return Optional.empty();
        }
    }
    
    /**
     * Proxy POST request to Yaci Store
     */
    public <T, R> Optional<R> proxyPostRequest(String endpoint, T requestBody, Class<R> responseType) {
        return proxyPostRequest(endpoint, requestBody, responseType, null);
    }
    
    /**
     * Proxy POST request to Yaci Store with custom headers
     */
    public <T, R> Optional<R> proxyPostRequest(String endpoint, T requestBody, Class<R> responseType, HttpHeaders headers) {
        if (isCircuitOpen()) {
            log.debug("Circuit breaker is open, skipping Yaci Store POST request to: {}", endpoint);
            return Optional.empty();
        }
        
        try {
            String url = buildUrl(endpoint);
            log.debug("Proxying POST request to Yaci Store: {}", url);
            
            HttpHeaders requestHeaders = headers != null ? headers : new HttpHeaders();
            requestHeaders.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<T> entity = new HttpEntity<>(requestBody, requestHeaders);
            
            ResponseEntity<R> response = restTemplate.exchange(url, HttpMethod.POST, entity, responseType);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                resetCircuitBreaker();
                return Optional.ofNullable(response.getBody());
            } else {
                log.warn("Non-success response from Yaci Store POST: {} - {}", response.getStatusCode(), url);
                return Optional.empty();
            }
            
        } catch (RestClientException e) {
            handleFailure(endpoint, e);
            return Optional.empty();
        }
    }
    
    /**
     * Check if Yaci Store is available and healthy
     */
    public boolean isYaciStoreAvailable() {
        // Use cached result if within TTL
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastHealthCheck < HEALTH_CACHE_TTL && !healthCache.isEmpty()) {
            return (Boolean) healthCache.getOrDefault("available", false);
        }
        
        try {
            String healthUrl = buildUrl("/actuator/health");
            log.debug("Checking Yaci Store health: {}", healthUrl);
            
            ResponseEntity<Map> response = restTemplate.getForEntity(healthUrl, Map.class);
            boolean available = response.getStatusCode().is2xxSuccessful();
            
            // Update cache
            healthCache.put("available", available);
            healthCache.put("lastCheck", currentTime);
            healthCache.put("status", response.getBody());
            lastHealthCheck = currentTime;
            
            if (available) {
                resetCircuitBreaker();
                log.debug("Yaci Store is healthy");
            } else {
                log.warn("Yaci Store health check failed with status: {}", response.getStatusCode());
            }
            
            return available;
            
        } catch (RestClientException e) {
            log.debug("Yaci Store health check failed: {}", e.getMessage());
            healthCache.put("available", false);
            healthCache.put("lastCheck", currentTime);
            healthCache.put("error", e.getMessage());
            lastHealthCheck = currentTime;
            
            handleFailure("/actuator/health", e);
            return false;
        }
    }
    
    /**
     * Get Yaci Store health information
     */
    public Map<String, Object> getYaciStoreHealthInfo() {
        if (isYaciStoreAvailable()) {
            return Map.of(
                "available", true,
                "url", walletConfig.getYaciStoreUrl(),
                "circuitBreakerOpen", circuitOpen.get(),
                "consecutiveFailures", consecutiveFailures.get(),
                "lastHealthCheck", lastHealthCheck
            );
        } else {
            return Map.of(
                "available", false,
                "url", walletConfig.getYaciStoreUrl(),
                "circuitBreakerOpen", circuitOpen.get(),
                "consecutiveFailures", consecutiveFailures.get(),
                "lastHealthCheck", lastHealthCheck,
                "error", healthCache.getOrDefault("error", "Unknown error")
            );
        }
    }
    
    /**
     * Reset circuit breaker state on successful request
     */
    private void resetCircuitBreaker() {
        if (consecutiveFailures.get() > 0 || circuitOpen.get()) {
            log.info("Resetting circuit breaker - Yaci Store is responding");
            consecutiveFailures.set(0);
            circuitOpen.set(false);
        }
    }
    
    /**
     * Handle request failure and update circuit breaker state
     */
    private void handleFailure(String endpoint, Exception e) {
        long failures = consecutiveFailures.incrementAndGet();
        lastFailureTime.set(System.currentTimeMillis());
        
        log.debug("Yaci Store request failed (attempt {}/{}): {} - {}", 
                failures, FAILURE_THRESHOLD, endpoint, e.getMessage());
        
        if (failures >= FAILURE_THRESHOLD && !circuitOpen.get()) {
            circuitOpen.set(true);
            log.warn("Opening circuit breaker for Yaci Store after {} consecutive failures", failures);
        }
    }
    
    /**
     * Check if circuit breaker is open
     */
    private boolean isCircuitOpen() {
        if (!circuitOpen.get()) {
            return false;
        }
        
        // Check if we should attempt to close the circuit
        long timeSinceLastFailure = System.currentTimeMillis() - lastFailureTime.get();
        if (timeSinceLastFailure > CIRCUIT_BREAKER_TIMEOUT) {
            log.info("Attempting to close circuit breaker - timeout period elapsed");
            circuitOpen.set(false);
            return false;
        }
        
        return true;
    }
    
    /**
     * Build full URL for Yaci Store endpoint
     */
    private String buildUrl(String endpoint) {
        String baseUrl = walletConfig.getYaciStoreUrl();
        if (baseUrl.endsWith("/") && endpoint.startsWith("/")) {
            return baseUrl + endpoint.substring(1);
        } else if (!baseUrl.endsWith("/") && !endpoint.startsWith("/")) {
            return baseUrl + "/" + endpoint;
        }
        return baseUrl + endpoint;
    }
    
    /**
     * Force reset circuit breaker (for testing/admin purposes)
     */
    public void resetCircuitBreakerManually() {
        log.info("Manually resetting circuit breaker");
        circuitOpen.set(false);
        consecutiveFailures.set(0);
        healthCache.clear();
        lastHealthCheck = 0;
    }
}