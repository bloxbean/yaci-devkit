package com.bloxbean.cardano.yacicli.localcluster.service;

import ch.qos.logback.classic.Level;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.yaci.core.protocol.localstate.api.Era;
import com.bloxbean.cardano.yacicli.commands.common.RootLogService;
import com.bloxbean.cardano.yacicli.localcluster.ClusterService;
import com.bloxbean.cardano.yacicli.localcluster.NodeMode;
import com.bloxbean.cardano.yacicli.localcluster.common.GenesisUtil;
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
    private final YanoHttpNodeService yanoHttpNodeService;
    private final Path plutusCostModelsBasePath;

    public AccountService(ClusterService clusterService,
                          LocalClientProviderHelper localQueryClientUtil,
                          RootLogService rootLogService,
                          YanoHttpNodeService yanoHttpNodeService,
                          @Value("${yaci.cli.plutus-costmodels-path:./config}") String plutusCostModelsBasePath) {
        this.clusterService = clusterService;
        this.localQueryClientUtil = localQueryClientUtil;
        this.rootLogService = rootLogService;
        this.yanoHttpNodeService = yanoHttpNodeService;
        this.plutusCostModelsBasePath = Paths.get(plutusCostModelsBasePath);
    }

    private boolean isYanoOnlyMode(String clusterName) {
        try {
            var info = clusterService.getClusterInfo(clusterName);
            return NodeMode.YANO_ONLY == info.getNodeMode();
        } catch (Exception e) {
            return false;
        }
    }

    private Path resolveCostModelsFile(String clusterName) throws IOException {
        var clusterInfo = clusterService.getClusterInfo(clusterName);
        return GenesisUtil.resolveCostModelsFile(plutusCostModelsBasePath, clusterInfo.getProtocolMajorVer());
    }

    public boolean topup(String clusterName, Era era, String address, double adaValue, Consumer<String> writer) {
        if (isYanoOnlyMode(clusterName)) {
            return yanoHttpNodeService.topUp(clusterName, address, adaValue, writer);
        }

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
        if (isYanoOnlyMode(clusterName)) {
            return yanoHttpNodeService.mint(clusterName, assetName, quantity, receiver, writer);
        }

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
        if (isYanoOnlyMode(clusterName)) {
            return yanoHttpNodeService.getFundsAtGenesisKeys(clusterName);
        }

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
        if (isYanoOnlyMode(clusterName)) {
            return yanoHttpNodeService.getUtxos(clusterName, address);
        }

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
