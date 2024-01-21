package com.bloxbean.cardano.yacicli.commands.localcluster.service;

import ch.qos.logback.classic.Level;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.yaci.core.protocol.localstate.api.Era;
import com.bloxbean.cardano.yacicli.commands.common.RootLogService;
import com.bloxbean.cardano.yacicli.commands.localcluster.ClusterService;
import com.bloxbean.cardano.yacicli.commands.localcluster.common.LocalClientProviderHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.error;
import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.writeLn;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountService {
    private final ClusterService clusterService;
    private final LocalClientProviderHelper localQueryClientUtil;
    private final RootLogService rootLogService;

    public boolean topup(String clusterName, Era era, String address, double adaValue, Consumer<String> writer) {
        Level orgLevel = rootLogService.getLogLevel();
        if (!rootLogService.isDebugLevel())
            rootLogService.setLogLevel(Level.OFF);

        LocalNodeService localNodeService = null;
        try {
            Path clusterFolder = clusterService.getClusterFolder(clusterName);
            localNodeService = new LocalNodeService(clusterFolder, era, localQueryClientUtil, writer);

            localNodeService.topUp(address, adaValue, msg -> writeLn(msg));
            return true;
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
