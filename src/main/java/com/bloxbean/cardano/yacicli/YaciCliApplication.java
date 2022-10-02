package com.bloxbean.cardano.yacicli;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
@Slf4j
public class YaciCliApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder(YaciCliApplication.class)
				.logStartupInfo(false)
				.run(args);
	}

}
