package com.bloxbean.cardano.yacicli;

import com.bloxbean.cardano.yacicli.localcluster.ClusterInfo;
import com.bloxbean.cardano.yacicli.localcluster.ClusterService;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import lombok.RequiredArgsConstructor;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import static com.bloxbean.cardano.yacicli.localcluster.ClusterConfig.CLUSTER_NAME;

@Component
@RequiredArgsConstructor
public class YaciCliPromptProvider implements PromptProvider {
    private final ClusterService clusterService;

    @Override
    public AttributedString getPrompt() {
        if (CommandContext.INSTANCE.getCurrentMode() == CommandContext.Mode.REGULAR) {
            return new AttributedString("yaci-cli:>",
                    AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN).bold());
        } else {
            String clusterName = CommandContext.INSTANCE.getProperty(CLUSTER_NAME);
            if (clusterName == null)
                clusterName = "";

            String promptName = "devnet";
            try {
                if (StringUtils.hasText(clusterName)) {
                    ClusterInfo clusterInfo = clusterService.getClusterInfo(clusterName);
                    if (clusterInfo != null) {
                        if (clusterInfo.isMasterNode())
                            promptName = "devnet";
                        else {
                            if (clusterInfo.isBlockProducer())
                                promptName = "devnet-peer/bp";
                            else
                                promptName = "devnet-peer/relay";
                        }
                    }
                }
            } catch (Exception e) {
                //ignore
            }

            return new AttributedString(promptName + ":" + clusterName + ">",
                    AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN).bold());
        }
    }
}
