package com.bloxbean.cardano.yacicli.cip30.controller;

import com.bloxbean.cardano.yacicli.cip30.config.WalletConfigurationProperties;
import com.bloxbean.cardano.yacicli.cip30.service.YaciStoreProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/wallet")
@CrossOrigin(origins = {"http://localhost:*", "https://localhost:*", "http://127.0.0.1:*", "https://127.0.0.1:*"})
@ConditionalOnProperty(prefix = "yaci.devkit.wallet", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class WalletSdkController {
    
    private final WalletConfigurationProperties walletConfig;
    private final YaciStoreProxyService proxyService;
    
    private static final String SDK_VERSION = "1.0.0";
    
    /**
     * Serve the CIP-30 SDK JavaScript file
     */
    @GetMapping(value = "/sdk.js", produces = "application/javascript")
    public ResponseEntity<String> getSdkJs() {
        try {
            log.debug("Serving wallet SDK JavaScript file");
            
            ClassPathResource resource = new ClassPathResource("static/wallet-sdk.js");
            String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("application/javascript"));
            
            // Disable caching for development
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");
            
            headers.add("X-SDK-Version", SDK_VERSION);
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type, Accept");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
                    
        } catch (IOException e) {
            log.error("Failed to load wallet SDK JavaScript file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("// Error loading Yaci DevKit SDK\nconsole.error('Failed to load Yaci DevKit SDK');");
        }
    }
    
    /**
     * Health check endpoint for wallet service
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Basic health info
            health.put("status", "UP");
            health.put("sdkVersion", SDK_VERSION);
            health.put("enabled", walletConfig.isEnabled());
            health.put("mockMode", walletConfig.isMockMode());
            health.put("defaultMode", walletConfig.isDefaultMode());
            health.put("network", walletConfig.getNetwork());
            
            // Yaci Store health
            Map<String, Object> yaciStoreHealth = proxyService.getYaciStoreHealthInfo();
            health.put("yaciStore", yaciStoreHealth);
            
            // Configuration info
            Map<String, Object> config = new HashMap<>();
            config.put("yaciStoreUrl", walletConfig.getYaciStoreUrl());
            config.put("defaultAccountCount", walletConfig.getDefaultAccountCount());
            config.put("defaultAdaAmount", walletConfig.getDefaultAdaAmount());
            config.put("requestTimeoutMs", walletConfig.getRequestTimeoutMs());
            health.put("configuration", config);
            
            log.debug("Wallet health check completed successfully");
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            log.error("Wallet health check failed", e);
            
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            health.put("sdkVersion", SDK_VERSION);
            
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(health);
        }
    }
    
    /**
     * Get wallet service information
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getInfo() {
        Map<String, Object> info = new HashMap<>();
        
        info.put("name", "Yaci DevKit CIP-30 Wallet");
        info.put("version", SDK_VERSION);
        info.put("description", "CIP-30 compatible wallet for Cardano dApp development");
        info.put("apiVersion", "0.1.0");
        info.put("supportedExtensions", new String[]{});
        info.put("network", walletConfig.getNetwork());
        info.put("enabled", walletConfig.isEnabled());
        
        // Capabilities
        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("signTx", true);
        capabilities.put("signData", true);
        capabilities.put("submitTx", true);
        capabilities.put("getUtxos", true);
        capabilities.put("getBalance", true);
        capabilities.put("getAddresses", true);
        capabilities.put("getNetworkId", true);
        capabilities.put("getCollateral", false); // Not implemented for dev environment
        info.put("capabilities", capabilities);
        
        // Development features
        Map<String, Object> devFeatures = new HashMap<>();
        devFeatures.put("defaultAddresses", walletConfig.isDefaultMode());
        devFeatures.put("defaultAccountCount", walletConfig.getDefaultAccountCount());
        devFeatures.put("defaultAdaAmount", walletConfig.getDefaultAdaAmount());
        devFeatures.put("mockMode", walletConfig.isMockMode());
        devFeatures.put("yaciStoreIntegration", true);
        info.put("developmentFeatures", devFeatures);
        
        return ResponseEntity.ok(info);
    }
    
    /**
     * Get SDK configuration for frontend
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        Map<String, Object> config = new HashMap<>();
        
        // Public configuration that can be safely exposed to frontend
        config.put("enabled", walletConfig.isEnabled());
        config.put("network", walletConfig.getNetwork());
        config.put("defaultMode", walletConfig.isDefaultMode());
        config.put("sdkVersion", SDK_VERSION);
        config.put("apiVersion", "0.1.0");
        
        // CORS configuration
        Map<String, Object> cors = new HashMap<>();
        cors.put("allowedOrigins", walletConfig.getCors().getAllowedOrigins());
        cors.put("allowedMethods", walletConfig.getCors().getAllowedMethods());
        config.put("cors", cors);
        
        return ResponseEntity.ok(config);
    }
    
    /**
     * OPTIONS handler for CORS preflight requests
     */
    @RequestMapping(method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> handleOptions() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type, Accept, Authorization");
        headers.add("Access-Control-Max-Age", "3600");
        
        return ResponseEntity.ok().headers(headers).build();
    }
}