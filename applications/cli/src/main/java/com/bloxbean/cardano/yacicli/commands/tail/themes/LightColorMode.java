package com.bloxbean.cardano.yacicli.commands.tail.themes;

import static com.bloxbean.cardano.yacicli.common.AnsiColors.*;

public class LightColorMode implements ColorMode {
    @Override
    public String getPromptColor() {
        return WHITE_BOLD_BRIGHT;
    }

    @Override
    public String getConnectionHeaderColor() {
        return BLACK_BOLD_BRIGHT;
    }

    @Override
    public String getConnectionLabelColor() {
        return PURPLE_BOLD_BRIGHT;
    }

    @Override
    public String getConnectionValueColor() {
        return GREEN_BRIGHT;
    }

    @Override
    public String getBlockHeaderBg() {
        return BLACK_BACKGROUND;
    }

    @Override
    public String getBlockHeaderColor() {
        return WHITE_BOLD_BRIGHT;
    }

    @Override
    public String getReceiverLabelColor() {
        return BLUE_BOLD_BRIGHT;
    }

    @Override
    public String getReceiverValueColor() {
        return BLACK;
    }

    @Override
    public String getAdaTokenLabelColor() {
        return BLUE_BOLD_BRIGHT;
    }

    @Override
    public String getAdaTokenAmountColor() {
        return RED_BRIGHT;
    }

    @Override
    public String getMintLabelColor() {
        return BLUE_BOLD_BRIGHT;
    }

    @Override
    public String getTokenColor() {
        return BLACK_BRIGHT;
    }


    @Override
    public String getInputsHeaderColor() {
        return BLACK_BOLD;
    }

    @Override
    public String getInputsColor() {
        return BLACK_BRIGHT;
    }
}
