package com.bloxbean.cardano.yacicli;

import com.bloxbean.cardano.yacicli.localcluster.config.GenesisConfig;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(GenesisConfig.class)
@Slf4j
public class YaciCliApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder(YaciCliApplication.class)
				.logStartupInfo(false)
				.run(args);
	}

    @PreDestroy
    public void onShutDown() {
    }
}
