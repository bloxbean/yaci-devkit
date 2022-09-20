package com.bloxbean.cardano.yacicli;

import com.bloxbean.cardano.yacicli.commands.localcluster.ClusterCommands;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.stereotype.Component;

@Component
public class YaciCliPromptProvider implements PromptProvider {
    @Override
    public AttributedString getPrompt() {
        if (CommandContext.INSTANCE.getCurrentMode() == CommandContext.Mode.REGULAR) {
            return new AttributedString("yaci-cli:>",
                    AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN).bold());
        } else {
            String clusterName = CommandContext.INSTANCE.getProperty(ClusterCommands.CUSTER_NAME);
            if (clusterName == null)
                clusterName = "";

            return new AttributedString("local-cluster:" + clusterName + ">",
                    AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN).bold());
        }
    }
}
