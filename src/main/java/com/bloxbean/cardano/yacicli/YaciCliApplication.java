package com.bloxbean.cardano.yacicli;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class YaciCliApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder(YaciCliApplication.class)
				.logStartupInfo(false)
				.run(args);
	}

}
