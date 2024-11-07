package com.bloxbean.cardano.yacicli.localcluster.api.service;

import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.backend.api.BackendService;
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.plutus.spec.BigIntPlutusData;
import com.bloxbean.cardano.client.plutus.spec.PlutusV2Script;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.Tx;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import com.bloxbean.cardano.yacicli.localcluster.ClusterConfig;
import com.bloxbean.cardano.yacicli.localcluster.ClusterService;
import com.bloxbean.cardano.yacicli.localcluster.service.DefaultAddressService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

/**
 * Generate test transactions for testing
 */
@Component
@RequiredArgsConstructor
//@ConditionalOnExpression("${yaci.cli.test-transactions.enabled:true} && ${yaci.store.enabled:false}")
@Slf4j
public class TestTransactionService {
    private final DefaultAddressService defaultAddressService;
    private final ClusterService clusterService;

    PlutusV2Script alwaysSuccessScript = PlutusV2Script.builder()
            .type("PlutusScriptV2")
            .cborHex("49480100002221200101")
            .build();

    @SneakyThrows
    public Optional<String> generateReferenceInputTransaction(int defaultAccountIndex) {
        try {
            writeLn(infoLabel("Test Data", "Generating reference input transaction"));
            Optional<BackendService> backendService = getBackendService();
            if (backendService.isEmpty()) {
                log.error("Backend service not available. Cannot generate test transactions");
                return Optional.empty();
            }

            var account = defaultAddressService.getAccount(defaultAccountIndex);
            if (account == null) {
                log.error("Default account not found");
                return Optional.empty();
            }

            var quickTxBuilder = new QuickTxBuilder(backendService.get());

            Tx tx = new Tx()
                    .payToAddress(account.baseAddress(), List.of(Amount.ada(1)), alwaysSuccessScript)
                    .from(account.baseAddress());

            var result = quickTxBuilder
                    .compose(tx)
                    .withSigner(SignerProviders.signerFrom(account))
                    .completeAndWait(System.out::println);

            if (!result.isSuccessful()) {
                log.error("Error generating reference input transaction. " + result.getResponse());
                return Optional.empty();
            }

            var scriptHash = HexUtil.encodeHexString(alwaysSuccessScript.getScriptHash());
            writeLn(infoLabel("Test Data", "Generated reference input transaction. Script Hash: " + scriptHash));
            return Optional.of(scriptHash);
        } catch (Exception e) {
            log.error("Error generating reference input transaction", e);
            return Optional.empty();
        }
    }

    @SneakyThrows
    public Optional<String> generateOutputWithDatum(int defaultAccountIndex) {
        try {
            writeLn(infoLabel("Test Data", "Generating output with datum transaction"));
            Optional<BackendService> backendService = getBackendService();
            if (backendService.isEmpty()) {
                log.error("Backend service not available. Cannot generate test transactions");
                return Optional.empty();
            }

            var account = defaultAddressService.getAccount(defaultAccountIndex);
            if (account == null) {
                log.error("Default account not found");
                return Optional.empty();
            }

            var quickTxBuilder = new QuickTxBuilder(backendService.get());

            var datum = BigIntPlutusData.of(1000);
            var scriptAddr = AddressProvider.getEntAddress(alwaysSuccessScript, Networks.testnet()).toBech32();

            Tx tx = new Tx()
                    .payToContract(scriptAddr, Amount.ada(1), datum)
                    .from(account.baseAddress());

            var result = quickTxBuilder
                    .compose(tx)
                    .withSigner(SignerProviders.signerFrom(account))
                    .completeAndWait(System.out::println);

            if (!result.isSuccessful()) {
                log.error("Error generating output datum tx. " + result.getResponse());
                return Optional.empty();
            }

            var datumHash = datum.getDatumHash();
            writeLn(infoLabel("Test Data", "Generated utxo with datum. Datum: " + datumHash));
            return Optional.of(datumHash);
        } catch (Exception e) {
            log.error("Error generating utxo with datum transaction", e);
            return Optional.empty();
        }
    }


    private Optional<BackendService> getBackendService() {
        try {
            String clusterName = CommandContext.INSTANCE.getProperty(ClusterConfig.CLUSTER_NAME);
            var clusterInfo = clusterService.getClusterInfo(clusterName);
            if (clusterInfo == null) {
                log.error("Cluster {} not found", clusterName);
                return Optional.empty();
            }

            var yaciStorePort = clusterInfo.getYaciStorePort();

            String backendUrl = "http://localhost:" + yaciStorePort + "/api/v1/";
            return Optional.of(new BFBackendService(backendUrl, "dummy_key"));
        } catch (Exception e) {
            log.error("Error getting backend service", e);
            return Optional.empty();
        }
    }
}
