package com.bloxbean.cardano.yacicli.localcluster.config;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/*
 * This class holds the custom overloaded values for the GenesisConfig. Ideally, the map in this class remains empty.
 * However, if the GenesisConfig is updated through the admin create endpoint, these values will be merged with the default GenesisConfig values during creation of node.
 */
@Component
public class CustomGenesisConfig {
    private Map<String, String> map;

    public CustomGenesisConfig() {
        this.map = new HashMap<>();
    }

    public void populate(Map<String, String> updatedMap) {
        this.map.clear();
        this.map.putAll(updatedMap);
    }

    public Map<String, String> getMap() {
        return map;
    }
}
