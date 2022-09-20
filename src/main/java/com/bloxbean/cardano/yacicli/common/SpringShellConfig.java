package com.bloxbean.cardano.yacicli.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jline.terminal.Terminal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringShellConfig {

    @Bean
    public ShellHelper shellHelper(Terminal terminal) {
        return new ShellHelper(terminal);
    }


    @Bean
    public ObjectMapper getObjectMapper() {
        return new ObjectMapper();
    }

}
