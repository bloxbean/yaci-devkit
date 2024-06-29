package com.bloxbean.cardano.yacicli;

import com.bloxbean.cardano.yacicli.commands.localcluster.ClusterConfig;
import com.bloxbean.cardano.yacicli.commands.localcluster.config.GenesisConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.shell.jline.NonInteractiveShellRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@SpringBootApplication
@EnableConfigurationProperties(GenesisConfig.class)
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
        NonInteractiveShellRunner nonInteractiveShellRunner;

		public InitBean(ClusterConfig clusterConfig, NonInteractiveShellRunner nonInteractiveShellRunner) {
			this.clusterConfig = clusterConfig;
            this.nonInteractiveShellRunner = nonInteractiveShellRunner;
		}

		@PostConstruct
		public void initialize() throws IOException {
			Path path = Path.of(clusterConfig.getCLIBinFolder());
			if (!Files.exists(path))
				Files.createDirectories(path);

            nonInteractiveShellRunner.setCommandsFromInputArgs(commandsFromInputArgs);
		}
	}

    //Used to parse multiple comma separate commands for NonInteractiveShellRunner
    private Function<ApplicationArguments, List<String>> commandsFromInputArgs = args -> {
        if (args.getSourceArgs().length == 0) {
            return Collections.emptyList();
        }

        String raw = Arrays.stream(args.getSourceArgs())
                .collect(Collectors.joining(" "));
        String[] commands = raw.split(",");
        for (String cmd: commands) {
            if (log.isDebugEnabled())
                log.debug("Command: " +  cmd);
        }
        return Arrays.asList(commands);
    };

    @PreDestroy
    public void onShutDown() {
    }
}
