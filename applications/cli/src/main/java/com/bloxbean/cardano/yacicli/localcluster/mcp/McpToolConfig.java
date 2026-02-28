package com.bloxbean.cardano.yacicli.localcluster.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpToolConfig {

    @Bean
    public ToolCallbackProvider devnetTools(DevnetMcpTools devnetMcpTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(devnetMcpTools)
                .build();
    }
}
