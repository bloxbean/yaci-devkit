package com.bloxbean.cardano.yacicli.cip30.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "yaci.devkit.wallet", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class WalletCorsConfiguration {
    
    private final WalletConfigurationProperties walletConfig;
    
    @Bean
    public CorsFilter walletCorsFilter() {
        log.info("Configuring CORS for CIP-30 wallet endpoints");
        
        CorsConfiguration config = new CorsConfiguration();
        
        // Add allowed origins
        walletConfig.getCors().getAllowedOrigins().forEach(origin -> {
            if (origin.contains("*")) {
                // Handle wildcard ports by allowing pattern
                config.addAllowedOriginPattern(origin);
            } else {
                config.addAllowedOrigin(origin);
            }
        });
        
        // Add allowed methods
        walletConfig.getCors().getAllowedMethods().forEach(config::addAllowedMethod);
        
        // Add allowed headers
        walletConfig.getCors().getAllowedHeaders().forEach(config::addAllowedHeader);
        
        // Set credentials and max age
        config.setAllowCredentials(walletConfig.getCors().isAllowCredentials());
        config.setMaxAge(walletConfig.getCors().getMaxAge());
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/wallet/**", config);
        source.registerCorsConfiguration("/wallet-demo/**", config);
        
        log.info("CORS configured for wallet endpoints with origins: {}", 
                walletConfig.getCors().getAllowedOrigins());
        
        return new CorsFilter(source);
    }
    
    @Bean
    public CorsConfigurationSource walletCorsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        walletConfig.getCors().getAllowedOrigins().forEach(origin -> {
            if (origin.contains("*")) {
                configuration.addAllowedOriginPattern(origin);
            } else {
                configuration.addAllowedOrigin(origin);
            }
        });
        
        walletConfig.getCors().getAllowedMethods().forEach(configuration::addAllowedMethod);
        walletConfig.getCors().getAllowedHeaders().forEach(configuration::addAllowedHeader);
        configuration.setAllowCredentials(walletConfig.getCors().isAllowCredentials());
        configuration.setMaxAge(walletConfig.getCors().getMaxAge());
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/wallet/**", configuration);
        source.registerCorsConfiguration("/wallet-demo/**", configuration);
        
        return source;
    }
}