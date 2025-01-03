package com.bloxbean.cardano.yacicli;

import com.bloxbean.cardano.yacicli.localcluster.config.GenesisConfig;
import com.bloxbean.cardano.yacicli.util.ProcessUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(GenesisConfig.class)
@Slf4j
public class YaciCliApplication {
    @Autowired
    private ProcessUtil processUtil;

	public static void main(String[] args) {
		new SpringApplicationBuilder(YaciCliApplication.class)
				.logStartupInfo(false)
				.run(args);
	}

    @PostConstruct
    public void stopRunningProcesses() {
        processUtil.killRunningProcesses();
    }

    @PreDestroy
    public void onShutDown() {
    }
}
