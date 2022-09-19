package com.bloxbean.cardano.yacicli.common;

import java.util.HashMap;
import java.util.Map;

public enum CommandContext {
    INSTANCE;

    public enum Mode {
        REGULAR, LOCAL_CLUSTER
    }

    private Mode currentMode;
    private Map<String, String> props;

    private CommandContext() {
        this.currentMode = Mode.REGULAR;
        this.props = new HashMap<>();
    }

    public void setCurrentMode(Mode mode) {
        this.currentMode = mode;
        this.props.clear();
    }

    public Mode getCurrentMode() {
        return this.currentMode;
    }

    public void setProperty(String key, String value) {
        props.put(key, value);
    }

    public String getProperty(String key) {
        return props.get(key);
    }
}
