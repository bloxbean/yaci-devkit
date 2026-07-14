package com.bloxbean.cardano.yacicli.localcluster.scenario;

import com.bloxbean.cardano.client.crypto.KeyGenUtil;
import com.bloxbean.cardano.client.transaction.spec.script.ScriptPubkey;
import com.bloxbean.cardano.client.quicktx.signing.SignerRegistry;
import com.bloxbean.cardano.yacicli.localcluster.service.DefaultAddressService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScenarioSignerRegistryFactoryTest {
    private final DefaultAddressService addressService = new DefaultAddressService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void registersAllDefaultAccountsAndDefaultPolicy() {
        ScenarioSignerRegistryFactory factory = new ScenarioSignerRegistryFactory(addressService, new ScenarioSignerConfig());

        SignerRegistry registry = factory.create();

        assertThat(registry.resolve("account://acc0")).isPresent();
        assertThat(registry.resolve("account://acc19")).isPresent();
        assertThat(registry.resolve("policy://default")).isPresent();

        // Unknown refs must not resolve.
        assertThat(registry.resolve("account://acc20")).isEmpty();
        assertThat(registry.resolve("wallet://nobody")).isEmpty();
    }

    @Test
    void registersConfiguredAccountWalletPolicyAndCustomRefs() throws Exception {
        ScenarioSignerConfig config = new ScenarioSignerConfig();

        var account = new ScenarioSignerConfig.AccountSigner();
        account.setRef("account://treasury");
        account.setMnemonic(addressService.getDefaultMnemonic());
        account.setAccount(0);
        account.setIndex(0);
        config.getAccounts().add(account);

        var wallet = new ScenarioSignerConfig.WalletSigner();
        wallet.setRef("wallet://user1");
        wallet.setMnemonic(addressService.getDefaultMnemonic());
        config.getWallets().add(wallet);

        var policyKeys = KeyGenUtil.generateKey();
        var policyScript = ScriptPubkey.create(policyKeys.getVkey());
        var policy = new ScenarioSignerConfig.PolicySigner();
        policy.setRef("policy://nft");
        policy.setScriptJson(objectMapper.writeValueAsString(policyScript));
        policy.getSigningKeys().add(policyKeys.getSkey().getCborHex());
        config.getPolicies().add(policy);

        var rawKeys = KeyGenUtil.generateKey();
        var custom = new ScenarioSignerConfig.CustomSigner();
        custom.setRef("signer://payment");
        custom.setPrivateKey(rawKeys.getSkey().getCborHex());
        custom.setAddress("addr_test1qplx7z8w2k2r0g6zmxzft5g6h72m7v7dss0zla7g8mqlm0czw5d5upv3qg3n7j0s2qx7rj5r7f8fj3p9r4p5qg7l9qss5x6g0");
        config.getCustom().add(custom);

        ScenarioSignerRegistryFactory factory = new ScenarioSignerRegistryFactory(addressService, config);
        SignerRegistry registry = factory.create();

        assertThat(registry.resolve("account://treasury")).isPresent();
        assertThat(registry.resolve("account://treasury").flatMap(binding -> binding.preferredAddress())).isPresent();
        assertThat(registry.resolve("wallet://user1")).isPresent();
        assertThat(registry.resolve("wallet://user1").flatMap(binding -> binding.preferredAddress())).isPresent();
        assertThat(registry.resolve("policy://nft")).isPresent();
        assertThat(registry.resolve("policy://nft").orElseThrow().signerFor("policy")).isNotNull();
        assertThat(registry.resolve("signer://payment")).isPresent();
        assertThat(registry.resolve("signer://payment").orElseThrow().signerFor("payment")).isNotNull();
        assertThat(registry.resolve("signer://payment").flatMap(binding -> binding.preferredAddress())).isPresent();
    }

    @Test
    void rejectsDuplicateRefsByDefault() {
        ScenarioSignerConfig config = new ScenarioSignerConfig();
        var account = new ScenarioSignerConfig.AccountSigner();
        account.setRef("account://acc0");
        account.setMnemonic(addressService.getDefaultMnemonic());
        config.getAccounts().add(account);

        ScenarioSignerRegistryFactory factory = new ScenarioSignerRegistryFactory(addressService, config);

        assertThatThrownBy(factory::create)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate scenario signer ref: account://acc0");
    }

    @Test
    void canOverrideDefaultRefsWhenExplicitlyEnabled() {
        ScenarioSignerConfig config = new ScenarioSignerConfig();
        config.setOverrideDefaults(true);
        var account = new ScenarioSignerConfig.AccountSigner();
        account.setRef("account://acc0");
        account.setMnemonic(addressService.getDefaultMnemonic());
        config.getAccounts().add(account);

        ScenarioSignerRegistryFactory factory = new ScenarioSignerRegistryFactory(addressService, config);

        assertThat(factory.create().resolve("account://acc0")).isPresent();
    }
}
