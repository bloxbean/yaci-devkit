package com.bloxbean.cardano.yacicli.localcluster.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Getter
public class ApplicationConfig {

    @Value("${is.docker:false}")
    private boolean isDocker;

    private int adminPort;

    @EventListener
    public void onApplicationEvent(final ServletWebServerInitializedEvent event) {
        adminPort = event.getWebServer().getPort();
    }
}
