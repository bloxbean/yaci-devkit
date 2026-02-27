package com.bloxbean.cardano.yacicli.cip30.service;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.yacicli.cip30.dto.WalletAccountDto;
import com.bloxbean.cardano.yacicli.localcluster.service.DefaultAddressService;
import com.bloxbean.cardano.yacicli.localcluster.service.model.DefaultAddress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletAccountService {
    private final DefaultAddressService defaultAddressService;

    // Custom imported accounts (in-memory storage)
    private final Map<String, CustomAccount> customAccounts = new ConcurrentHashMap<>();
    private final AtomicInteger customAccountCounter = new AtomicInteger(0);
    private final AtomicReference<String> activeAccountId = new AtomicReference<>("0");

    public String getActiveAccountId() {
        return activeAccountId.get();
    }

    public boolean setActiveAccountId(String accountId) {
        if (getAccount(accountId).isPresent()) {
            activeAccountId.set(accountId);
            return true;
        }
        return false;
    }

    public List<WalletAccountDto> getAllAccounts() {
        List<WalletAccountDto> accounts = new ArrayList<>();

        // Add default accounts
        List<DefaultAddress> defaultAddresses = defaultAddressService.getDefaultAddresses();
        for (int i = 0; i < defaultAddresses.size(); i++) {
            DefaultAddress addr = defaultAddresses.get(i);
            accounts.add(WalletAccountDto.builder()
                    .id(String.valueOf(i))
                    .name("Account " + i)
                    .address(addr.getAddress())
                    .stakeAddress(addr.getStakeAddress())
                    .isDefault(true)
                    .build());
        }

        // Add custom accounts
        customAccounts.forEach((id, customAccount) -> {
            accounts.add(WalletAccountDto.builder()
                    .id(id)
                    .name(customAccount.getName())
                    .address(customAccount.getAddress())
                    .stakeAddress(customAccount.getStakeAddress())
                    .isDefault(false)
                    .build());
        });

        return accounts;
    }

    public Optional<WalletAccountDto> getAccount(String id) {
        // Check if it's a default account (numeric id 0-19)
        try {
            int index = Integer.parseInt(id);
            if (index >= 0 && index < 20) {
                List<DefaultAddress> defaultAddresses = defaultAddressService.getDefaultAddresses();
                if (index < defaultAddresses.size()) {
                    DefaultAddress addr = defaultAddresses.get(index);
                    return Optional.of(WalletAccountDto.builder()
                            .id(String.valueOf(index))
                            .name("Account " + index)
                            .address(addr.getAddress())
                            .stakeAddress(addr.getStakeAddress())
                            .isDefault(true)
                            .build());
                }
            }
        } catch (NumberFormatException e) {
            // Not a numeric ID, check custom accounts
        }

        // Check custom accounts
        CustomAccount customAccount = customAccounts.get(id);
        if (customAccount != null) {
            return Optional.of(WalletAccountDto.builder()
                    .id(id)
                    .name(customAccount.getName())
                    .address(customAccount.getAddress())
                    .stakeAddress(customAccount.getStakeAddress())
                    .isDefault(false)
                    .build());
        }

        return Optional.empty();
    }

    public WalletAccountDto importAccount(String mnemonic, String name) {
        Account account = new Account(Networks.testnet(), mnemonic);
        String id = "custom-" + customAccountCounter.incrementAndGet();

        CustomAccount customAccount = new CustomAccount(
                id,
                name,
                mnemonic,
                account.baseAddress(),
                account.stakeAddress()
        );

        customAccounts.put(id, customAccount);

        log.info("Imported custom account: {} with address: {}", name, account.baseAddress());

        return WalletAccountDto.builder()
                .id(id)
                .name(name)
                .address(account.baseAddress())
                .stakeAddress(account.stakeAddress())
                .isDefault(false)
                .build();
    }

    public boolean deleteAccount(String id) {
        if (id.startsWith("custom-")) {
            return customAccounts.remove(id) != null;
        }
        return false; // Cannot delete default accounts
    }

    /**
     * Get the Account object for signing operations
     */
    public Optional<Account> getSigningAccount(String accountId) {
        // Check if it's a default account
        try {
            int index = Integer.parseInt(accountId);
            if (index >= 0 && index < 20) {
                return Optional.of(defaultAddressService.getAccount(index));
            }
        } catch (NumberFormatException e) {
            // Not a numeric ID, check custom accounts
        }

        // Check custom accounts
        CustomAccount customAccount = customAccounts.get(accountId);
        if (customAccount != null) {
            return Optional.of(new Account(Networks.testnet(), customAccount.getMnemonic()));
        }

        return Optional.empty();
    }

    /**
     * Find account by address
     */
    public Optional<Account> getSigningAccountByAddress(String address) {
        // Check default accounts
        List<DefaultAddress> defaultAddresses = defaultAddressService.getDefaultAddresses();
        for (int i = 0; i < defaultAddresses.size(); i++) {
            if (defaultAddresses.get(i).getAddress().equals(address)) {
                return Optional.of(defaultAddressService.getAccount(i));
            }
        }

        // Check custom accounts
        for (CustomAccount customAccount : customAccounts.values()) {
            if (customAccount.getAddress().equals(address)) {
                return Optional.of(new Account(Networks.testnet(), customAccount.getMnemonic()));
            }
        }

        return Optional.empty();
    }

    // Inner class for custom accounts
    private static class CustomAccount {
        private final String id;
        private final String name;
        private final String mnemonic;
        private final String address;
        private final String stakeAddress;

        public CustomAccount(String id, String name, String mnemonic, String address, String stakeAddress) {
            this.id = id;
            this.name = name;
            this.mnemonic = mnemonic;
            this.address = address;
            this.stakeAddress = stakeAddress;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getMnemonic() { return mnemonic; }
        public String getAddress() { return address; }
        public String getStakeAddress() { return stakeAddress; }
    }
}
