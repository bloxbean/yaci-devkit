package com.bloxbean.cardano.yacicli.commands.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RootLogService {
    private Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

    public void setLogLevel(String logLevel) {
        if (StringUtils.hasLength(logLevel)) {
            switch (logLevel) {
                case "info":
                    root.setLevel(Level.INFO);
                    break;
                case "debug":
                    root.setLevel(Level.DEBUG);
                    break;
                case "trace":
                    root.setLevel(Level.TRACE);
                    break;
                case "error":
                    root.setLevel(Level.ERROR);
                    break;
                case "off":
                    root.setLevel(Level.OFF);
                    break;
                default:
                    break;
            }
        }
    }

    public void setLogLevel(Level level) {
        root.setLevel(level);
    }

    public Level getLogLevel() {
        return root.getLevel();
    }

    public boolean isDebugLevel() {
        return root.isDebugEnabled();
    }

}
