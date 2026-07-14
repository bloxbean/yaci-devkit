package com.bloxbean.cardano.yacicli.localcluster.scenario;

import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.ProtocolParams;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.backend.api.BackendService;
import com.bloxbean.cardano.client.backend.api.DefaultProtocolParamsSupplier;
import com.bloxbean.cardano.client.backend.model.Asset;
import com.bloxbean.cardano.client.backend.model.EpochContent;
import com.bloxbean.cardano.client.backend.model.ScriptDatum;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.core.protocol.localstate.api.Era;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import com.bloxbean.cardano.yacicli.common.Tuple;
import com.bloxbean.cardano.yacicli.localcluster.ClusterConfig;
import com.bloxbean.cardano.yacicli.localcluster.ClusterInfo;
import com.bloxbean.cardano.yacicli.localcluster.ClusterInfoService;
import com.bloxbean.cardano.yacicli.localcluster.ClusterService;
import com.bloxbean.cardano.yacicli.localcluster.NodeMode;
import com.bloxbean.cardano.yacicli.localcluster.service.AccountService;
import com.bloxbean.cardano.yacicli.localcluster.service.ClusterUtilService;
import com.bloxbean.cardano.yacicli.localcluster.service.DefaultAddressService;
import com.bloxbean.cardano.yacicli.localcluster.service.LocalBackendServiceProvider;
import com.bloxbean.cardano.yacicli.localcluster.service.RollbackService;
import com.bloxbean.cardano.yacicli.localcluster.service.YanoHttpNodeService;
import com.bloxbean.cardano.yacicli.localcluster.service.model.DefaultAddress;
import com.bloxbean.cardano.yacicli.localcluster.yano.YanoBootstrapService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScenarioControlService {
    private static final String DEFAULT_CLUSTER_NAME = "default";

    private final ClusterService clusterService;
    private final ClusterInfoService clusterInfoService;
    private final ClusterUtilService clusterUtilService;
    private final AccountService accountService;
    private final RollbackService rollbackService;
    private final YanoBootstrapService yanoBootstrapService;
    private final YanoHttpNodeService yanoHttpNodeService;
    private final DefaultAddressService defaultAddressService;
    private final LocalBackendServiceProvider backendServiceProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void ensureContext() {
        String clusterName = clusterName();
        CommandContext.INSTANCE.setProperty(ClusterConfig.CLUSTER_NAME, clusterName);
        try {
            ClusterInfo info = clusterService.getClusterInfo(clusterName);
            if (info != null && info.getEra() != null) {
                CommandContext.INSTANCE.setEra(info.getEra());
            }
        } catch (Exception e) {
            log.debug("Could not load cluster info for scenario context", e);
        }
    }

    public String clusterName() {
        String clusterName = CommandContext.INSTANCE.getProperty(ClusterConfig.CLUSTER_NAME);
        return clusterName == null || clusterName.isBlank() ? DEFAULT_CLUSTER_NAME : clusterName;
    }

    public ClusterInfo clusterInfo() {
        try {
            return clusterInfoService.getClusterInfo(clusterName());
        } catch (Exception e) {
            throw new IllegalStateException("Cluster not available: " + e.getMessage(), e);
        }
    }

    public boolean topup(String accountOrAddress, double ada, Consumer<String> writer) {
        ensureContext();
        String address = resolveAddress(accountOrAddress);
        Era era = CommandContext.INSTANCE.getEra();
        return accountService.topup(clusterName(), era, address, ada, writer);
    }

    public boolean rollback(long blocks, Consumer<String> writer) {
        ensureContext();
        return rollbackService.rollback(blocks, writer);
    }

    public boolean snapshot(String name, Consumer<String> writer) {
        ensureContext();
        ClusterInfo info = clusterInfo();
        if (isYanoDevnet(info)) {
            return yanoBootstrapService.createSnapshot(info.getYanoHttpPort(), name, writer) != null;
        }
        rollbackService.takeDBSnapshot(writer);
        return true;
    }

    public boolean restore(String name, Consumer<String> writer) {
        ensureContext();
        ClusterInfo info = clusterInfo();
        if (isYanoDevnet(info)) {
            return yanoBootstrapService.restoreSnapshot(info.getYanoHttpPort(), name, writer) != null;
        }
        return rollbackService.rollbackToLastDBSnapshot(writer);
    }

    public boolean reset(Consumer<String> writer) {
        ensureContext();
        String clusterName = clusterName();
        try {
            clusterService.stopCluster(writer);
            clusterService.deleteClusterDataFolder(clusterName, writer);
            return clusterService.startCluster(clusterName).stared();
        } catch (Exception e) {
            writer.accept("Reset failed: " + e.getMessage());
            return false;
        }
    }

    public boolean advance(Map<String, Object> request, Consumer<String> writer) {
        ensureContext();
        if (request == null || request.isEmpty()) {
            writer.accept("Advance step requires blocks, slots, seconds, epochs, or until.");
            return false;
        }

        if (request.containsKey("until")) {
            Object until = request.get("until");
            if (until instanceof Map<?, ?> map) {
                return advanceUntil(map, writer);
            }
            writer.accept("advance.until must be a map.");
            return false;
        }

        long slots = 0;
        if (request.containsKey("epochs")) {
            slots += toLong(request.get("epochs")) * Math.max(1, clusterInfo().getEpochLength());
        }
        if (request.containsKey("slots")) {
            slots += toLong(request.get("slots"));
        }
        if (request.containsKey("seconds")) {
            double slotLength = Math.max(0.1, clusterInfo().getSlotLength());
            slots += Math.max(1, Math.round(toDouble(request.get("seconds")) / slotLength));
        }
        if (request.containsKey("blocks")) {
            long blocks = toLong(request.get("blocks"));
            if (slots == 0) {
                return waitBlocks(blocks, writer);
            }
            slots += blocks;
        }

        if (slots <= 0) {
            writer.accept("Advance amount must be positive.");
            return false;
        }
        return advanceSlots(slots, writer);
    }

    public boolean advanceSlots(long slots, Consumer<String> writer) {
        ClusterInfo info = clusterInfo();
        if (isYanoDevnet(info)) {
            return yanoBootstrapService.advanceTime(info.getYanoHttpPort(), slots, writer) != null;
        }
        return waitSlots(slots, writer);
    }

    public boolean waitBlocks(long blocks, Consumer<String> writer) {
        if (blocks <= 0) {
            writer.accept("Block wait amount must be positive.");
            return false;
        }
        ClusterInfo info = clusterInfo();
        Tuple<Long, Point> start = getAvailableTip(info, writer);
        if (start == null) {
            writer.accept("Node tip is not available; cannot wait for blocks.");
            return false;
        }
        long startBlock = start._1;
        long target = startBlock + blocks;
        long waitMs = Math.max(250L, Math.round(info.getBlockTime() * 1000));
        long deadline = System.currentTimeMillis() + Math.max(30_000L, waitMs * blocks * 30);
        while (System.currentTimeMillis() < deadline) {
            Tuple<Long, Point> tip = getAvailableTip(info, writer);
            if (tip != null && tip._1 >= target) {
                return true;
            }
            sleep(waitMs);
        }
        writer.accept("Timed out waiting for block " + target);
        return false;
    }

    public List<Utxo> utxos(String accountOrAddress, Consumer<String> writer) {
        ensureContext();
        return accountService.getUtxos(clusterName(), CommandContext.INSTANCE.getEra(), resolveAddress(accountOrAddress), writer);
    }

    public BigInteger lovelaceAt(String accountOrAddress, Consumer<String> writer) {
        BigInteger total = BigInteger.ZERO;
        for (Utxo utxo : utxos(accountOrAddress, writer)) {
            for (Amount amount : utxo.getAmount()) {
                if (LOVELACE.equals(amount.getUnit())) {
                    total = total.add(amount.getQuantity());
                }
            }
        }
        return total;
    }

    public int latestEpoch() {
        ensureContext();
        ClusterInfo info = clusterInfo();
        if (NodeMode.YANO_ONLY == info.getNodeMode() && isTcpListening(info.getYanoHttpPort())) {
            EpochContent epoch = yanoHttpNodeService.getLatestEpoch(clusterName());
            return epoch != null && epoch.getEpoch() != null ? epoch.getEpoch() : slotEpoch();
        }
        return slotEpoch();
    }

    public ProtocolParams protocolParams() {
        ensureContext();
        try {
            BackendService backendService = backendServiceProvider.getBackendService(clusterName())
                    .orElseThrow(() -> new IllegalStateException("Backend service is not available"));
            return new DefaultProtocolParamsSupplier(backendService.getEpochService()).getProtocolParams();
        } catch (Exception e) {
            log.debug("Could not query protocol params through backend", e);
            return yanoHttpNodeService.getProtocolParams(clusterName());
        }
    }

    public String resolveAddress(String accountOrAddress) {
        if (accountOrAddress == null) {
            return null;
        }
        if (accountOrAddress.startsWith("account://acc")) {
            int index = Integer.parseInt(accountOrAddress.substring("account://acc".length()));
            List<DefaultAddress> addresses = defaultAddressService.getDefaultAddresses();
            if (index < 0 || index >= addresses.size()) {
                throw new IllegalArgumentException("Unknown account ref: " + accountOrAddress);
            }
            return addresses.get(index).getAddress();
        }
        return accountOrAddress;
    }

    public List<Map<String, Object>> accounts() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<DefaultAddress> addresses = defaultAddressService.getDefaultAddresses();
        for (int i = 0; i < addresses.size(); i++) {
            DefaultAddress address = addresses.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("ref", "account://acc" + i);
            item.put("address", address.getAddress());
            item.put("stake_address", address.getStakeAddress());
            item.put("default_utxo", address.getDefaultUtxoId());
            result.add(item);
        }
        return result;
    }

    public Map<String, Object> stateSummary() {
        ensureContext();
        Map<String, Object> state = new LinkedHashMap<>();
        ClusterInfo info = clusterInfo();
        state.put("cluster", clusterName());
        state.put("node_mode", info.getNodeMode() != null ? info.getNodeMode().getValue() : null);
        state.put("epoch_length", info.getEpochLength());
        Tuple<Long, Point> tip = getAvailableTip(info, msg -> log.debug(msg));
        if (tip != null) {
            state.put("block", tip._1);
            state.put("slot", tip._2.getSlot());
            state.put("hash", tip._2.getHash());
            state.put("epoch", info.getEpochLength() > 0 ? tip._2.getSlot() / info.getEpochLength() : null);
            state.put("tip_available", true);
        } else {
            state.put("tip_available", false);
        }

        if (!canQueryBalances(info)) {
            state.put("balances_available", false);
            return state;
        }

        try {
            List<DefaultAddress> addresses = defaultAddressService.getDefaultAddresses();
            Map<String, Object> balances = new LinkedHashMap<>();
            for (int i = 0; i < addresses.size(); i++) {
                String address = addresses.get(i).getAddress();
                List<Utxo> utxos = accountService.getUtxos(clusterName(), CommandContext.INSTANCE.getEra(), address, msg -> log.debug(msg));
                BigInteger lovelace = BigInteger.ZERO;
                for (Utxo utxo : utxos) {
                    for (Amount amount : utxo.getAmount()) {
                        if (LOVELACE.equals(amount.getUnit())) {
                            lovelace = lovelace.add(amount.getQuantity());
                        }
                    }
                }
                balances.put("account://acc" + i, lovelace);
            }
            state.put("balances_lovelace", balances);
            state.put("balances_available", true);
        } catch (Exception e) {
            log.debug("Could not add account balances to scenario state summary", e);
            state.put("balances_available", false);
        }
        return state;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> protocolParamsAsMap() {
        ProtocolParams params = protocolParams();
        if (params == null) {
            return Map.of();
        }
        return objectMapper.convertValue(params, Map.class);
    }

    public BigInteger assetSupply(String unit) {
        try {
            BackendService backendService = backendServiceProvider.getBackendService(clusterName())
                    .orElseThrow(() -> new IllegalStateException("Backend service is not available"));
            Result<Asset> result = backendService.getAssetService().getAsset(unit);
            if (result.isSuccessful() && result.getValue() != null && result.getValue().getQuantity() != null) {
                return new BigInteger(result.getValue().getQuantity());
            }
            throw new IllegalStateException(result.getResponse());
        } catch (Exception e) {
            throw new IllegalStateException("Could not query asset supply for " + unit + ": " + e.getMessage(), e);
        }
    }

    public JsonNode scriptDatum(String datumHash) {
        try {
            BackendService backendService = backendServiceProvider.getBackendService(clusterName())
                    .orElseThrow(() -> new IllegalStateException("Backend service is not available"));
            Result<ScriptDatum> result = backendService.getScriptService().getScriptDatum(datumHash);
            if (result.isSuccessful() && result.getValue() != null) {
                return result.getValue().getJsonValue();
            }
            throw new IllegalStateException(result.getResponse());
        } catch (Exception e) {
            throw new IllegalStateException("Could not query datum " + datumHash + ": " + e.getMessage(), e);
        }
    }

    private boolean waitSlots(long slots, Consumer<String> writer) {
        ClusterInfo info = clusterInfo();
        Tuple<Long, Point> start = getAvailableTip(info, writer);
        if (start == null) {
            writer.accept("Node tip is not available; cannot wait for slots.");
            return false;
        }
        long startSlot = start._2.getSlot();
        long target = startSlot + slots;
        long waitMs = Math.max(250L, Math.round(info.getSlotLength() * 1000));
        long deadline = System.currentTimeMillis() + Math.max(30_000L, waitMs * slots * 10);
        while (System.currentTimeMillis() < deadline) {
            Tuple<Long, Point> tip = getAvailableTip(info, writer);
            if (tip != null && tip._2.getSlot() >= target) {
                return true;
            }
            sleep(waitMs);
        }
        writer.accept("Timed out waiting for slot " + target);
        return false;
    }

    private boolean advanceUntil(Map<?, ?> until, Consumer<String> writer) {
        ClusterInfo info = clusterInfo();
        Tuple<Long, Point> tip = getAvailableTip(info, writer);
        if (tip == null) {
            writer.accept("Node tip is not available; cannot advance until target.");
            return false;
        }
        long currentSlot = tip._2.getSlot();
        long currentBlock = tip._1;
        if (until.containsKey("slot")) {
            long target = toLong(until.get("slot"));
            return target <= currentSlot || advanceSlots(target - currentSlot, writer);
        }
        if (until.containsKey("epoch")) {
            long target = toLong(until.get("epoch"));
            long current = latestEpoch();
            long epochs = target - current;
            return epochs <= 0 || advanceSlots(epochs * Math.max(1, clusterInfo().getEpochLength()), writer);
        }
        if (until.containsKey("block")) {
            long target = toLong(until.get("block"));
            return target <= currentBlock || waitBlocks(target - currentBlock, writer);
        }
        writer.accept("advance.until supports slot, epoch, or block.");
        return false;
    }

    private int slotEpoch() {
        ClusterInfo info = clusterInfo();
        Tuple<Long, Point> tip = getAvailableTip(info, msg -> log.debug(msg));
        int epochLength = Math.max(1, info.getEpochLength());
        return tip != null ? (int) (tip._2.getSlot() / epochLength) : 0;
    }

    private Tuple<Long, Point> getAvailableTip(ClusterInfo info, Consumer<String> writer) {
        if (info == null) {
            return null;
        }
        if (isYanoDevnet(info) && isTcpListening(info.getYanoHttpPort())) {
            return yanoHttpNodeService.getTip(clusterName());
        }
        if (NodeMode.YANO_ONLY != info.getNodeMode() && isTcpListening(info.getNodePort())) {
            return clusterUtilService.getTip(writer);
        }
        return null;
    }

    private boolean canQueryBalances(ClusterInfo info) {
        if (info == null) {
            return false;
        }
        if (NodeMode.YANO_ONLY == info.getNodeMode()) {
            return isTcpListening(info.getYanoHttpPort());
        }
        return isTcpListening(info.getNodePort());
    }

    private boolean isTcpListening(int port) {
        if (port <= 0) {
            return false;
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 250);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean isYanoDevnet(ClusterInfo info) {
        return info != null && (NodeMode.YANO_ONLY == info.getNodeMode() || NodeMode.YANO_PRIMARY == info.getNodeMode());
    }

    private long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        return new BigDecimal(String.valueOf(value)).longValue();
    }

    private double toDouble(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        return new BigDecimal(String.valueOf(value)).doubleValue();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
