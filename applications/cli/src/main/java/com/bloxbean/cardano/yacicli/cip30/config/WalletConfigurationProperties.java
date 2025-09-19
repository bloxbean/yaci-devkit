package com.bloxbean.cardano.yacicli.cip30.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "yaci.devkit.wallet")
public class WalletConfigurationProperties {
    
    /**
     * Enable/disable CIP-30 wallet functionality
     */
    private boolean enabled = true;
    
    /**
     * Yaci Store base URL for proxying requests
     */
    private String yaciStoreUrl = "http://localhost:8080";
    
    /**
     * Enable mock mode when Yaci Store is unavailable
     */
    private boolean mockMode = false;
    
    /**
     * Cardano network (preprod, preview, mainnet)
     */
    private String network = "preprod";
    
    /**
     * Use default addresses from DefaultAddressService
     */
    private boolean defaultMode = true;
    
    /**
     * Custom wallet storage path for user wallets
     */
    private String customStoragePath = "${user.home}/.yaci-devkit/wallets";
    
    /**
     * Number of default accounts to generate
     */
    private int defaultAccountCount = 20;
    
    /**
     * Default ADA amount per address
     */
    private String defaultAdaAmount = "10000";
    
    /**
     * Request timeout for Yaci Store proxy (milliseconds)
     */
    private int requestTimeoutMs = 5000;
    
    /**
     * Connection timeout for Yaci Store proxy (milliseconds)
     */
    private int connectionTimeoutMs = 3000;
    
    /**
     * Maximum retry attempts for failed requests
     */
    private int maxRetries = 3;
    
    /**
     * CORS configuration
     */
    private Cors cors = new Cors();
    
    @Data
    public static class Cors {
        /**
         * Allowed origins for CORS
         */
        private List<String> allowedOrigins = List.of(
            "http://localhost:*",
            "https://localhost:*", 
            "http://127.0.0.1:*",
            "https://127.0.0.1:*"
        );
        
        /**
         * Allowed methods for CORS
         */
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
        
        /**
         * Allowed headers for CORS
         */
        private List<String> allowedHeaders = List.of("*");
        
        /**
         * Allow credentials in CORS requests
         */
        private boolean allowCredentials = true;
        
        /**
         * Max age for preflight requests (seconds)
         */
        private long maxAge = 3600;
    }
}