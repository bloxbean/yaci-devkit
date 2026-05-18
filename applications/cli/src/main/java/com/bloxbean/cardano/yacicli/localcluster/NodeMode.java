package com.bloxbean.cardano.yacicli.localcluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum NodeMode {
    HASKELL_ONLY("haskell-only"),
    COMPANION("companion"),
    YANO_ONLY("yano-only"),
    YANO_PRIMARY("yano-primary");

    private final String value;

    NodeMode(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static NodeMode fromValue(String value) {
        if (value == null) return HASKELL_ONLY;
        for (NodeMode mode : values()) {
            if (mode.value.equals(value)) return mode;
        }
        return HASKELL_ONLY;
    }
}
