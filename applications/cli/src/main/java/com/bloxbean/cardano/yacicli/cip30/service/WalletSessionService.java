package com.bloxbean.cardano.yacicli.cip30.service;

import com.bloxbean.cardano.yacicli.cip30.config.WalletConfigurationProperties;
import com.bloxbean.cardano.yacicli.localcluster.service.DefaultAddressService;
import com.bloxbean.cardano.yacicli.localcluster.service.model.DefaultAddress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "yaci.devkit.wallet", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class WalletSessionService {
    
    private final DefaultAddressService defaultAddressService;
    private final WalletConfigurationProperties walletConfig;
    
    // Session state (in-memory for development)
    private String customMnemonic = null;
    private boolean useCustomWallet = false;
    
    /**
     * Get wallet addresses based on current mode
     */
    public List<String> getWalletAddresses() {
        if (useCustomWallet && customMnemonic != null) {
            // TODO: Implement custom wallet address generation
            log.warn("Custom wallet not yet implemented, falling back to default addresses");
            return getDefaultWalletAddresses();
        } else {
            return getDefaultWalletAddresses();
        }
    }
    
    /**
     * Get default wallet addresses from DefaultAddressService
     */
    public List<String> getDefaultWalletAddresses() {
        List<DefaultAddress> defaultAddresses = defaultAddressService.getDefaultAddresses();
        
        // Limit to configured account count
        int limit = Math.min(walletConfig.getDefaultAccountCount(), defaultAddresses.size());
        
        return defaultAddresses.stream()
                .limit(limit)
                .map(DefaultAddress::getAddress)
                .collect(Collectors.toList());
    }
    
    /**
     * Get stake addresses for wallets
     */
    public List<String> getStakeAddresses() {
        if (useCustomWallet && customMnemonic != null) {
            // TODO: Implement custom wallet stake address generation
            return new ArrayList<>();
        } else {
            List<DefaultAddress> defaultAddresses = defaultAddressService.getDefaultAddresses();
            int limit = Math.min(walletConfig.getDefaultAccountCount(), defaultAddresses.size());
            
            return defaultAddresses.stream()
                    .limit(limit)
                    .map(DefaultAddress::getStakeAddress)
                    .collect(Collectors.toList());
        }
    }
    
    /**
     * Get the primary address (first address)
     */
    public String getPrimaryAddress() {
        List<String> addresses = getWalletAddresses();
        return addresses.isEmpty() ? null : addresses.get(0);
    }
    
    /**
     * Get default address by index
     */
    public DefaultAddress getDefaultAddress(int index) {
        List<DefaultAddress> addresses = defaultAddressService.getDefaultAddresses();
        if (index >= 0 && index < addresses.size()) {
            return addresses.get(index);
        }
        return null;
    }
    
    /**
     * Get address by string
     */
    public DefaultAddress getDefaultAddressByAddress(String address) {
        return defaultAddressService.getDefaultAddresses().stream()
                .filter(da -> da.getAddress().equals(address))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Check if an address belongs to the wallet
     */
    public boolean isWalletAddress(String address) {
        return getWalletAddresses().contains(address);
    }
    
    /**
     * Set custom wallet mnemonic
     */
    public void setCustomWallet(String mnemonic) {
        this.customMnemonic = mnemonic;
        this.useCustomWallet = true;
        log.info("Custom wallet set");
    }
    
    /**
     * Reset to default wallet
     */
    public void resetToDefaultWallet() {
        this.customMnemonic = null;
        this.useCustomWallet = false;
        log.info("Reset to default wallet");
    }
    
    /**
     * Check if using custom wallet
     */
    public boolean isUsingCustomWallet() {
        return useCustomWallet;
    }
    
    /**
     * Get wallet info
     */
    public WalletInfo getWalletInfo() {
        return WalletInfo.builder()
                .isDefaultMode(!useCustomWallet)
                .addressCount(getWalletAddresses().size())
                .primaryAddress(getPrimaryAddress())
                .build();
    }
    
    @lombok.Data
    @lombok.Builder
    public static class WalletInfo {
        private boolean isDefaultMode;
        private int addressCount;
        private String primaryAddress;
    }
}