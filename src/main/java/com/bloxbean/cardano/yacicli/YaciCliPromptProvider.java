package com.bloxbean.cardano.yacicli;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.stereotype.Component;

@Component
public class YaciCliPromptProvider implements PromptProvider {
    @Override
    public AttributedString getPrompt() {
        return new AttributedString("yaci-cli:>",
        AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN).bold());
    }
}
