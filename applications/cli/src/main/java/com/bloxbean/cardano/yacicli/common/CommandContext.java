package com.bloxbean.cardano.yacicli.common;

import com.bloxbean.cardano.yaci.core.protocol.localstate.api.Era;

import java.util.HashMap;
import java.util.Map;

public enum CommandContext {
    INSTANCE;

    public enum Mode {
        REGULAR, LOCAL_CLUSTER
    }

    private Mode currentMode;
    private Map<String, String> props;
    private Era era; //Node era

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

    public Era getEra() {
        return era;
    }

    public void setEra(Era era) {
        this.era = era;
    }
}
