package com.bloxbean.cardano.yacicli.localcluster.scenario;

import co.nstant.in.cbor.model.Array;
import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.crypto.Bech32;
import com.bloxbean.cardano.client.crypto.SecretKey;
import com.bloxbean.cardano.client.crypto.VerificationKey;
import com.bloxbean.cardano.client.quicktx.signing.DefaultSignerRegistry;
import com.bloxbean.cardano.client.quicktx.signing.SignerBinding;
import com.bloxbean.cardano.client.quicktx.signing.SignerRegistry;
import com.bloxbean.cardano.client.quicktx.signing.SignerScopes;
import com.bloxbean.cardano.client.function.TxSigner;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.transaction.spec.Policy;
import com.bloxbean.cardano.client.transaction.spec.script.NativeScript;
import com.bloxbean.cardano.client.transaction.spec.script.ScriptPubkey;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yacicli.localcluster.service.DefaultAddressService;
import com.bloxbean.cardano.hdwallet.Wallet;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Builds a {@link SignerRegistry} for declarative scenarios from the 20 pre-funded default
 * accounts, so scenario YAML can reference signers without embedding any keys:
 *
 * <ul>
 *   <li>{@code account://acc0} .. {@code account://acc19} — the default accounts</li>
 *   <li>{@code policy://default} — a deterministic single-sig native-script minting policy keyed by account 0</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScenarioSignerRegistryFactory {
    public static final String ACCOUNT_REF_PREFIX = "account://acc";
    public static final String DEFAULT_POLICY_REF = "policy://default";

    private final DefaultAddressService defaultAddressService;
    private final ScenarioSignerConfig signerConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SignerRegistry create() {
        DefaultSignerRegistry registry = new DefaultSignerRegistry();
        Set<String> refs = new HashSet<>();

        if (signerConfig.isIncludeDefaultAccounts()) {
            int count = defaultAddressService.getDefaultAddresses().size();
            for (int i = 0; i < count; i++) {
                String ref = ACCOUNT_REF_PREFIX + i;
                registry.addAccount(ref, defaultAddressService.getAccount(i));
                refs.add(ref);
            }
        }

        if (signerConfig.isIncludeDefaultPolicy()) {
            try {
                registry.addPolicy(DEFAULT_POLICY_REF, defaultPolicy());
                refs.add(DEFAULT_POLICY_REF);
            } catch (Exception e) {
                // Non-fatal: account refs still work; only policy://default minting is affected.
                log.warn("Could not register {} for scenarios: {}", DEFAULT_POLICY_REF, e.getMessage());
            }
        }

        registerConfiguredAccounts(registry, refs);
        registerConfiguredWallets(registry, refs);
        registerConfiguredPolicies(registry, refs);
        registerConfiguredCustomSigners(registry, refs);

        return registry;
    }

    /** Deterministic single-sig native-script policy keyed by default account 0. */
    private Policy defaultPolicy() throws Exception {
        Account acc0 = defaultAddressService.getAccount(0);
        VerificationKey vk = VerificationKey.create(acc0.publicKeyBytes());
        ScriptPubkey scriptPubkey = ScriptPubkey.create(vk);
        SecretKey sk = SecretKey.create(acc0.privateKeyBytes());
        return new Policy(scriptPubkey, List.of(sk));
    }

    private void registerConfiguredAccounts(DefaultSignerRegistry registry, Set<String> refs) {
        for (ScenarioSignerConfig.AccountSigner signer : signerConfig.getAccounts()) {
            String ref = requireRef(signer.getRef(), "account signer");
            registerRef(refs, ref);
            registry.addAccount(ref, accountFrom(signer));
            log.info("Registered scenario account signer ref {}", ref);
        }
    }

    private void registerConfiguredWallets(DefaultSignerRegistry registry, Set<String> refs) {
        for (ScenarioSignerConfig.WalletSigner signer : signerConfig.getWallets()) {
            String ref = requireRef(signer.getRef(), "wallet signer");
            registerRef(refs, ref);
            registry.addWallet(ref, walletFrom(signer));
            log.info("Registered scenario wallet signer ref {}", ref);
        }
    }

    private void registerConfiguredPolicies(DefaultSignerRegistry registry, Set<String> refs) {
        for (ScenarioSignerConfig.PolicySigner signer : signerConfig.getPolicies()) {
            String ref = requireRef(signer.getRef(), "policy signer");
            registerRef(refs, ref);
            registry.addPolicy(ref, policyFrom(signer));
            log.info("Registered scenario policy signer ref {}", ref);
        }
    }

    private void registerConfiguredCustomSigners(DefaultSignerRegistry registry, Set<String> refs) {
        for (ScenarioSignerConfig.CustomSigner signer : signerConfig.getCustom()) {
            String ref = requireRef(signer.getRef(), "custom signer");
            registerRef(refs, ref);
            registry.addCustom(ref, customBinding(signer));
            log.info("Registered scenario custom signer ref {}", ref);
        }
    }

    private Account accountFrom(ScenarioSignerConfig.AccountSigner signer) {
        String mnemonic = valueOrFile(signer.getMnemonic(), signer.getMnemonicFile());
        String rootKey = valueOrFile(signer.getRootKey(), signer.getRootKeyFile());
        String accountKey = valueOrFile(signer.getAccountKey(), signer.getAccountKeyFile());

        if (hasText(mnemonic)) {
            return Account.createFromMnemonic(Networks.testnet(), mnemonic, signer.getAccount(), signer.getIndex());
        }
        if (hasText(rootKey)) {
            return Account.createFromRootKey(Networks.testnet(), keyBytes(rootKey), signer.getAccount(), signer.getIndex());
        }
        if (hasText(accountKey)) {
            return Account.createFromAccountKey(Networks.testnet(), keyBytes(accountKey), signer.getAccount(), signer.getIndex());
        }
        throw new IllegalArgumentException("Scenario account signer " + signer.getRef()
                + " must define one of mnemonic, mnemonicFile, rootKey, rootKeyFile, accountKey, or accountKeyFile");
    }

    private Wallet walletFrom(ScenarioSignerConfig.WalletSigner signer) {
        String mnemonic = valueOrFile(signer.getMnemonic(), signer.getMnemonicFile());
        String rootKey = valueOrFile(signer.getRootKey(), signer.getRootKeyFile());
        String accountKey = valueOrFile(signer.getAccountKey(), signer.getAccountKeyFile());

        if (hasText(mnemonic)) {
            return Wallet.createFromMnemonic(Networks.testnet(), mnemonic, signer.getAccount());
        }
        if (hasText(rootKey)) {
            return Wallet.createFromRootKey(Networks.testnet(), keyBytes(rootKey), signer.getAccount());
        }
        if (hasText(accountKey)) {
            return Wallet.createFromAccountKey(Networks.testnet(), keyBytes(accountKey));
        }
        throw new IllegalArgumentException("Scenario wallet signer " + signer.getRef()
                + " must define one of mnemonic, mnemonicFile, rootKey, rootKeyFile, accountKey, or accountKeyFile");
    }

    private Policy policyFrom(ScenarioSignerConfig.PolicySigner signer) {
        NativeScript script = policyScript(signer);
        List<SecretKey> signingKeys = new ArrayList<>();

        for (String key : signer.getSigningKeys()) {
            signingKeys.add(secretKey(key));
        }
        for (String keyFile : signer.getSigningKeyFiles()) {
            signingKeys.add(secretKeyFromFile(keyFile));
        }

        if (signingKeys.isEmpty()) {
            throw new IllegalArgumentException("Scenario policy signer " + signer.getRef()
                    + " must define at least one signing key");
        }

        return new Policy(script, signingKeys);
    }

    private NativeScript policyScript(ScenarioSignerConfig.PolicySigner signer) {
        try {
            if (hasText(signer.getScriptJson())) {
                return NativeScript.deserializeJson(signer.getScriptJson());
            }
            if (hasText(signer.getScriptFile())) {
                return NativeScript.deserializeJson(Files.readString(path(signer.getScriptFile())));
            }
            if (hasText(signer.getScriptCbor())) {
                var dataItem = CborSerializationUtil.deserialize(HexUtil.decodeHexString(normalizedHex(signer.getScriptCbor())));
                if (!(dataItem instanceof Array array)) {
                    throw new IllegalArgumentException("Native script CBOR must decode to a CBOR array");
                }
                return NativeScript.deserialize(array);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not load policy script for " + signer.getRef() + ": " + e.getMessage(), e);
        }

        throw new IllegalArgumentException("Scenario policy signer " + signer.getRef()
                + " must define one of scriptJson, scriptFile, or scriptCbor");
    }

    private SignerBinding customBinding(ScenarioSignerConfig.CustomSigner signer) {
        SecretKey key = hasText(signer.getPrivateKeyFile())
                ? secretKeyFromFile(signer.getPrivateKeyFile())
                : secretKey(signer.getPrivateKey());
        return new RawSecretKeyBinding(signer.getRef(), key, signer.getAddress());
    }

    private String requireRef(String ref, String label) {
        if (!hasText(ref)) {
            throw new IllegalArgumentException("Scenario " + label + " is missing ref");
        }
        return ref.trim();
    }

    private void registerRef(Set<String> refs, String ref) {
        if (refs.contains(ref) && !signerConfig.isOverrideDefaults()) {
            throw new IllegalArgumentException("Duplicate scenario signer ref: " + ref
                    + ". Set yaci.scenario.signers.override-defaults=true to replace an existing ref.");
        }
        refs.add(ref);
    }

    private SecretKey secretKeyFromFile(String file) {
        try {
            Path path = path(file);
            String content = Files.readString(path).trim();
            if (content.startsWith("{")) {
                return objectMapper.readValue(content, SecretKey.class);
            }
            return secretKey(content);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not load signing key file " + file + ": " + e.getMessage(), e);
        }
    }

    private SecretKey secretKey(String value) {
        if (!hasText(value)) {
            throw new IllegalArgumentException("Signing key value is required");
        }

        String trimmed = value.trim();
        try {
            if (trimmed.startsWith("{")) {
                return objectMapper.readValue(trimmed, SecretKey.class);
            }
            if (looksLikeBech32(trimmed)) {
                return SecretKey.create(Bech32.decode(trimmed).data);
            }

            String hex = normalizedHex(trimmed);
            if (looksLikeCborEncodedKey(hex)) {
                return new SecretKey(hex);
            }
            return SecretKey.create(HexUtil.decodeHexString(hex));
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not parse signing key: " + e.getMessage(), e);
        }
    }

    private byte[] keyBytes(String value) {
        if (!hasText(value)) {
            throw new IllegalArgumentException("Key value is required");
        }

        String trimmed = value.trim();
        try {
            if (looksLikeBech32(trimmed)) {
                return Bech32.decode(trimmed).data;
            }
            return HexUtil.decodeHexString(normalizedHex(trimmed));
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not parse key bytes: " + e.getMessage(), e);
        }
    }

    private Path path(String file) {
        if (!hasText(file)) {
            throw new IllegalArgumentException("File path is required");
        }
        Path path = Path.of(file.trim());
        return path.isAbsolute() ? path : Path.of(System.getProperty("user.dir")).resolve(path).normalize();
    }

    private String valueOrFile(String value, String file) {
        if (hasText(value)) {
            return value;
        }
        if (!hasText(file)) {
            return null;
        }
        try {
            return Files.readString(path(file)).trim();
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not load signer value file " + file + ": " + e.getMessage(), e);
        }
    }

    private String normalizedHex(String value) {
        String normalized = value.trim().replaceAll("\\s+", "");
        return normalized.regionMatches(true, 0, "0x", 0, 2)
                ? normalized.substring(2)
                : normalized;
    }

    private boolean looksLikeBech32(String value) {
        return value.contains("1") && !value.matches("(?i)^(0x)?[0-9a-f\\s]+$");
    }

    private boolean looksLikeCborEncodedKey(String hex) {
        String lower = hex.toLowerCase();
        return lower.startsWith("5820")
                || lower.startsWith("5840")
                || lower.startsWith("5860")
                || lower.startsWith("5880");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static class RawSecretKeyBinding implements SignerBinding {
        private final String ref;
        private final SecretKey secretKey;
        private final String address;

        private RawSecretKeyBinding(String ref, SecretKey secretKey, String address) {
            this.ref = ref;
            this.secretKey = secretKey;
            this.address = address;
        }

        @Override
        public TxSigner signerFor(String scope) {
            String normalized = scope == null ? "" : scope.trim().toLowerCase();
            if (!SignerScopes.PAYMENT.equals(normalized)) {
                throw new IllegalArgumentException("Unsupported scope for raw signer " + ref + ": " + scope);
            }
            return SignerProviders.signerFrom(secretKey);
        }

        @Override
        public Optional<Wallet> asWallet() {
            return Optional.empty();
        }

        @Override
        public Optional<String> preferredAddress() {
            return Optional.ofNullable(address).filter(value -> !value.isBlank());
        }
    }
}
