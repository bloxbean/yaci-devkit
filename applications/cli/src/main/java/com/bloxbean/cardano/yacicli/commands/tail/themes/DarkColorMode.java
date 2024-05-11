package com.bloxbean.cardano.yacicli.commands.tail.themes;

import static com.bloxbean.cardano.yacicli.common.AnsiColors.*;

public class DarkColorMode implements ColorMode {
    @Override
    public String getPromptColor() {
        return WHITE_BOLD_BRIGHT;
    }

    @Override
    public String getConnectionHeaderColor() {
        return YELLOW_BOLD_BRIGHT;
    }

    @Override
    public String getConnectionLabelColor() {
        return CYAN_BOLD_BRIGHT;
    }

    @Override
    public String getConnectionValueColor() {
        return GREEN_BOLD_BRIGHT;
    }

    @Override
    public String getBlockHeaderBg() {
        return WHITE_BACKGROUND;
    }

    @Override
    public String getBlockHeaderColor() {
        return BLACK_BOLD;
    }

    @Override
    public String getReceiverLabelColor() {
        return CYAN_BOLD_BRIGHT;
    }

    @Override
    public String getReceiverValueColor() {
        return YELLOW_BRIGHT;
    }

    @Override
    public String getAdaTokenLabelColor() {
        return CYAN_BOLD_BRIGHT;
    }

    @Override
    public String getAdaTokenAmountColor() {
        return GREEN_BRIGHT;
    }

    @Override
    public String getMintLabelColor() {
        return CYAN_BOLD_BRIGHT;
    }

    @Override
    public String getTokenColor() {
        return WHITE_BRIGHT;
    }


    @Override
    public String getInputsHeaderColor() {
        return YELLOW_BOLD_BRIGHT;
    }

    @Override
    public String getInputsColor() {
        return WHITE_BRIGHT;
    }
}
