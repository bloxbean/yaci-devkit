package com.bloxbean.cardano.yacicli.localcluster.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Getter
public class ApplicationConfig {
    public final static String YACI_STORE_ENABLED = "yaci.store.enabled";
    private final static String OGMIOS_ENABLED = "ogmios.enabled";
    private final static String KUPO_ENABLED = "kupo.enabled";

    private final Environment environment;

    @Value("${is.docker:false}")
    private boolean isDocker;

    private int adminPort;

    @EventListener
    public void onApplicationEvent(final ServletWebServerInitializedEvent event) {
        adminPort = event.getWebServer().getPort();
    }

    public boolean isYaciStoreEnabled() {
        var enabled = environment.getProperty(YACI_STORE_ENABLED, Boolean.class);
        return enabled != null? enabled: false;
    }

    public void setYaciStoreEnabled(boolean flag) {
        System.setProperty(YACI_STORE_ENABLED, String.valueOf(flag));
    }

    public boolean isOgmiosEnabled() {
        var enabled = environment.getProperty(OGMIOS_ENABLED, Boolean.class);
        return enabled != null? enabled: false;
    }

    public void setOgmiosEnabled(boolean flag) {
        System.setProperty(OGMIOS_ENABLED, String.valueOf(flag));
    }

    public boolean isKupoEnabled() {
        var enabled = environment.getProperty(KUPO_ENABLED, Boolean.class);
        return enabled != null? enabled: false;
    }

    public void setKupoEnabled(boolean flag) {
        System.setProperty(KUPO_ENABLED, String.valueOf(flag));
    }
}
