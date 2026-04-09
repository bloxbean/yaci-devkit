package com.bloxbean.cardano.yacicli.localcluster.common;

import com.bloxbean.cardano.client.crypto.SecretKey;
import com.bloxbean.cardano.client.crypto.VerificationKey;
import com.bloxbean.cardano.yacicli.common.Tuple;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared utility methods for genesis UTXO keys and cost model loading,
 * used by both LocalNodeService and YanoGovernanceService.
 */
public class GenesisUtil {
    private static final String UTXO_KEYS_FOLDER = "utxo-keys";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static List<Tuple<VerificationKey, SecretKey>> loadUtxoKeys(Path clusterFolder) throws IOException {
        List<Tuple<VerificationKey, SecretKey>> keys = new ArrayList<>();
        Path utxoFolder = clusterFolder.resolve(UTXO_KEYS_FOLDER);

        for (int i = 1; i <= 3; i++) {
            Path skeyPath = utxoFolder.resolve("utxo" + i + ".skey");
            SecretKey skey = objectMapper.readValue(skeyPath.toFile(), SecretKey.class);

            Path vkeyPath = utxoFolder.resolve("utxo" + i + ".vkey");
            VerificationKey vkey = objectMapper.readValue(vkeyPath.toFile(), VerificationKey.class);
            keys.add(new Tuple<>(vkey, skey));
        }
        return keys;
    }

    public static Map<String, long[]> loadCostModels(Path costModelsFile) throws IOException {
        Map<String, List<Long>> raw = objectMapper.readValue(costModelsFile.toFile(),
                new TypeReference<Map<String, List<Long>>>() {});
        Map<String, long[]> result = new HashMap<>();
        for (Map.Entry<String, List<Long>> entry : raw.entrySet()) {
            result.put(entry.getKey(), entry.getValue().stream().mapToLong(Long::longValue).toArray());
        }
        return result;
    }

    public static Path resolveCostModelsFile(Path basePath, int protocolMajorVer) {
        String fileName = protocolMajorVer >= 11
                ? "plutus-costmodels-v11.json"
                : "plutus-costmodels-v10.json";
        return basePath.resolve(fileName);
    }
}
