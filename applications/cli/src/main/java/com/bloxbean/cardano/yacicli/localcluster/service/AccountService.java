package com.bloxbean.cardano.yacicli.localcluster.service;

import ch.qos.logback.classic.Level;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.yaci.core.protocol.localstate.api.Era;
import com.bloxbean.cardano.yacicli.commands.common.RootLogService;
import com.bloxbean.cardano.yacicli.localcluster.ClusterService;
import com.bloxbean.cardano.yacicli.localcluster.common.LocalClientProviderHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.error;
import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.writeLn;

@Component
@Slf4j
public class AccountService {
    private final ClusterService clusterService;
    private final LocalClientProviderHelper localQueryClientUtil;
    private final RootLogService rootLogService;
    private final Path plutusCostModelsBasePath;

    public AccountService(ClusterService clusterService,
                          LocalClientProviderHelper localQueryClientUtil,
                          RootLogService rootLogService,
                          @Value("${yaci.cli.plutus-costmodels-path:./config}") String plutusCostModelsBasePath) {
        this.clusterService = clusterService;
        this.localQueryClientUtil = localQueryClientUtil;
        this.rootLogService = rootLogService;
        this.plutusCostModelsBasePath = Paths.get(plutusCostModelsBasePath);
    }

    private Path resolveCostModelsFile(String clusterName) throws IOException {
        var clusterInfo = clusterService.getClusterInfo(clusterName);
        int protocolMajorVer = clusterInfo.getProtocolMajorVer();
        String fileName = protocolMajorVer >= 11
                ? "plutus-costmodels-v11.json"
                : "plutus-costmodels-v10.json";
        return plutusCostModelsBasePath.resolve(fileName);
    }

    public boolean topup(String clusterName, Era era, String address, double adaValue, Consumer<String> writer) {
        Level orgLevel = rootLogService.getLogLevel();
        if (!rootLogService.isDebugLevel())
            rootLogService.setLogLevel(Level.OFF);

        LocalNodeService localNodeService = null;
        try {
            Path clusterFolder = clusterService.getClusterFolder(clusterName);
            localNodeService = new LocalNodeService(clusterFolder, era, localQueryClientUtil, writer);

            return localNodeService.topUp(address, adaValue, msg -> writeLn(msg));
        } catch (Exception e) {
            // if (log.isDebugEnabled())
            log.error("Error", e);
            writer.accept(error("Topup error : " + e.getMessage()));
            return false;
        } finally {
            rootLogService.setLogLevel(orgLevel);
            if (localNodeService != null)
                localNodeService.shutdown();
        }
    }

    public boolean mint(String clusterName, Era era, String assetName, BigInteger quantity, String receiver,
                        Consumer<String> writer) {
        Level orgLevel = rootLogService.getLogLevel();
        if (!rootLogService.isDebugLevel())
            rootLogService.setLogLevel(Level.OFF);

        LocalNodeService localNodeService = null;
        try {
            Path clusterFolder = clusterService.getClusterFolder(clusterName);
            localNodeService = new LocalNodeService(clusterFolder, era, localQueryClientUtil, writer);

            return localNodeService.mint(assetName, quantity, receiver, msg -> writeLn(msg));
        } catch (Exception e) {
            log.error("Error", e);
            writer.accept(error("Mint error : " + e.getMessage()));
            return false;
        } finally {
            rootLogService.setLogLevel(orgLevel);
            if (localNodeService != null)
                localNodeService.shutdown();
        }
    }

    public boolean updateCostModels(String clusterName, Era era, Consumer<String> writer) {
        Level orgLevel = rootLogService.getLogLevel();
        if (!rootLogService.isDebugLevel())
            rootLogService.setLogLevel(Level.OFF);

        LocalNodeService localNodeService = null;
        try {
            Path clusterFolder = clusterService.getClusterFolder(clusterName);
            localNodeService = new LocalNodeService(clusterFolder, era, localQueryClientUtil, writer);

            Path costModelsFile = resolveCostModelsFile(clusterName);
            writer.accept("Using Plutus cost models file: " + costModelsFile.getFileName());
            return localNodeService.updateCostModels(costModelsFile, msg -> writeLn(msg));
        } catch (Exception e) {
            log.error("Error", e);
            writer.accept(error("Plutus cost models update error: " + e.getMessage()));
            return false;
        } finally {
            rootLogService.setLogLevel(orgLevel);
            if (localNodeService != null)
                localNodeService.shutdown();
        }
    }

    public Map<String, List<Utxo>> getUtxosAtDefaultAccounts(String clusterName, Era era, Consumer<String> writer) {
        Level orgLevel = rootLogService.getLogLevel();
        if (!rootLogService.isDebugLevel())
            rootLogService.setLogLevel(Level.OFF);

        LocalNodeService localNodeService = null;
        try {
            Path clusterFolder = clusterService.getClusterFolder(clusterName);
            localNodeService = new LocalNodeService(clusterFolder, era, localQueryClientUtil, writer);
            Map<String, List<Utxo>> utxosMap = localNodeService.getFundsAtGenesisKeys();

            return utxosMap;
        } catch (Exception e) {
            // if (log.isDebugEnabled())
            log.error("Error", e);
            writer.accept(error("Topup error" + e.getMessage()));
            return Collections.EMPTY_MAP;
        } finally {
            rootLogService.setLogLevel(orgLevel);
            if (localNodeService != null)
                localNodeService.shutdown();
        }
    }

    public List<Utxo> getUtxos(String clusterName, Era era, String address, Consumer<String> writer) {
        Level orgLevel = rootLogService.getLogLevel();
        if (!rootLogService.isDebugLevel())
            rootLogService.setLogLevel(Level.OFF);

        LocalNodeService localNodeService = null;
        try {
            Path clusterFolder = clusterService.getClusterFolder(clusterName);
            localNodeService = new LocalNodeService(clusterFolder, era, localQueryClientUtil, writer);

            List<Utxo> utxos = localNodeService.getUtxos(address);
            return utxos;
        } catch (Exception e) {
            // if (log.isDebugEnabled())
            log.error("Error", e);
            writeLn(error("Get utxos error : " + e.getMessage()));
            return Collections.EMPTY_LIST;
        } finally {
            rootLogService.setLogLevel(orgLevel);
            if (localNodeService != null)
                localNodeService.shutdown();
        }
    }
}
