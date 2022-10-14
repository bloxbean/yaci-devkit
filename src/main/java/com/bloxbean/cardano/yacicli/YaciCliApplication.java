package com.bloxbean.cardano.yacicli;

import com.bloxbean.cardano.yacicli.commands.localcluster.ClusterConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
@Slf4j
public class YaciCliApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder(YaciCliApplication.class)
				.logStartupInfo(false)
				.run(args);
	}

	@Component
	public class InitBean{
		ClusterConfig clusterConfig;

		public InitBean(ClusterConfig clusterConfig) {
			this.clusterConfig = clusterConfig;
		}

		@PostConstruct
		public void initialize() throws IOException {
			Path path = Path.of(clusterConfig.getCLIBinFolder());
			if (!Files.exists(path))
				Files.createDirectories(path);
		}
	}

}
