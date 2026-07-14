package com.bloxbean.cardano.yacicli.localcluster.scenario;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "yaci.scenario.signers", ignoreUnknownFields = true)
public class ScenarioSignerConfig {
    private boolean includeDefaultAccounts = true;
    private boolean includeDefaultPolicy = true;
    private boolean overrideDefaults = false;

    private List<AccountSigner> accounts = new ArrayList<>();
    private List<WalletSigner> wallets = new ArrayList<>();
    private List<PolicySigner> policies = new ArrayList<>();
    private List<CustomSigner> custom = new ArrayList<>();

    @Data
    public static class AccountSigner {
        private String ref;
        private String mnemonic;
        private String mnemonicFile;
        private String rootKey;
        private String rootKeyFile;
        private String accountKey;
        private String accountKeyFile;
        private int account = 0;
        private int index = 0;
    }

    @Data
    public static class WalletSigner {
        private String ref;
        private String mnemonic;
        private String mnemonicFile;
        private String rootKey;
        private String rootKeyFile;
        private String accountKey;
        private String accountKeyFile;
        private int account = 0;
    }

    @Data
    public static class PolicySigner {
        private String ref;
        private String scriptFile;
        private String scriptJson;
        private String scriptCbor;
        private List<String> signingKeys = new ArrayList<>();
        private List<String> signingKeyFiles = new ArrayList<>();
    }

    @Data
    public static class CustomSigner {
        private String ref;
        private String privateKey;
        private String privateKeyFile;
        private String address;
    }
}
