package com.bloxbean.cardano.yacicli.commands.tail.themes;

public interface ColorMode {
    String getPromptColor();
    //Connection block
    String getConnectionHeaderColor();
    String getConnectionLabelColor();
    String getConnectionValueColor();

    //Block
    String getBlockHeaderBg();
    String getBlockHeaderColor();
    String getReceiverLabelColor();
    String getReceiverValueColor();
    String getAdaTokenLabelColor();
    String getAdaTokenAmountColor();

    String getMintLabelColor();

    String getTokenColor();

    String getInputsHeaderColor();
    String getInputsColor();
}
