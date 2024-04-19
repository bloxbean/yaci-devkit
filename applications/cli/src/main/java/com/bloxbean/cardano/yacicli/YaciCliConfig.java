package com.bloxbean.cardano.yacicli;

import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class YaciCliConfig {
    public final static String YACI_CLI_HOME = System.getProperty("user.home") + File.separator + ".yaci-cli";
}
