package com.bloxbean.cardano.yacicli.commands.localcluster.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GenesisUtil {

    public static List<GenesisConfig.HeavyDelegation> getHeavyDelegations(Path byronGenesisFile) {
        var objectMapper = new ObjectMapper();
        List<GenesisConfig.HeavyDelegation> heavyDelegations = new ArrayList<>();

        try {

            JsonNode rootNode = objectMapper.readTree(byronGenesisFile.toFile());

            // Access the heavyDelegation node
            JsonNode heavyDelegationNode = rootNode.path("heavyDelegation");

            // Iterate over the fields in the heavyDelegation node
            Iterator<Map.Entry<String, JsonNode>> fields = heavyDelegationNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String bootStakeDelegator = field.getKey();
                JsonNode detailsNode = field.getValue();

                int omega = detailsNode.path("omega").asInt();
                String issuerPk = detailsNode.path("issuerPk").asText();
                String delegatePk = detailsNode.path("delegatePk").asText();
                String cert = detailsNode.path("cert").asText();
                boolean last = !fields.hasNext(); // Determine if this is the last entry

                heavyDelegations.add(new GenesisConfig.HeavyDelegation(bootStakeDelegator, omega, issuerPk, delegatePk, cert, last));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return heavyDelegations;
    }

    public static List<GenesisConfig.GenesisDeleg> getGenesisDelegs(Path shelleyGenesisFile) {
        var objectMapper = new ObjectMapper();
        List<GenesisConfig.GenesisDeleg> genesisDelegs = new ArrayList<>();

        try {
            // Read the JSON file and parse it into a JsonNode
            JsonNode rootNode = objectMapper.readTree(shelleyGenesisFile.toFile());

            // Access the genDelegs node
            JsonNode genDelegsNode = rootNode.path("genDelegs");

            // Iterate over the fields in the genDelegs node
            Iterator<Map.Entry<String, JsonNode>> fields = genDelegsNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String delegator = field.getKey();
                JsonNode detailsNode = field.getValue();

                String delegate = detailsNode.path("delegate").asText();
                String vrf = detailsNode.path("vrf").asText();
                boolean last = !fields.hasNext(); // Determine if this is the last entry

                genesisDelegs.add(new GenesisConfig.GenesisDeleg(delegator, delegate, vrf, last));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return genesisDelegs;
    }

    public static List<GenesisConfig.NonAvvmBalances> getNonAvvmBalances(Path byronGenesisFile) {
        var objectMapper = new ObjectMapper();
        List<GenesisConfig.NonAvvmBalances> nonAvvmBalances = new ArrayList<>();

        try {
            // Read the JSON file and parse it into a JsonNode
            JsonNode rootNode = objectMapper.readTree(byronGenesisFile.toFile());

            // Access the nonAvvmBalances node
            JsonNode nonAvvmBalancesNode = rootNode.path("nonAvvmBalances");

            // Iterate over the fields in the nonAvvmBalances node
            Iterator<Map.Entry<String, JsonNode>> fields = nonAvvmBalancesNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String address = field.getKey();
                String balance = field.getValue().asText();
                boolean last = !fields.hasNext(); // Determine if this is the last entry

                nonAvvmBalances.add(new GenesisConfig.NonAvvmBalances(address, balance, last));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return nonAvvmBalances;
    }
}
