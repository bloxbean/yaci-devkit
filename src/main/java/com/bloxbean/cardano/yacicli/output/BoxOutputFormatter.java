package com.bloxbean.cardano.yacicli.output;

import com.bloxbean.cardano.yacicli.commands.tail.model.CliAmount;
import com.bloxbean.cardano.yacicli.commands.tail.model.CliBlock;
import com.bloxbean.cardano.yacicli.commands.tail.model.CliConnection;
import com.bloxbean.cardano.yacicli.output.asciitable.AsciiArtContentGroup;
import com.bloxbean.cardano.yacicli.output.asciitable.AsciiArtTable;

import java.util.ArrayList;
import java.util.List;

import static com.bloxbean.cardano.yacicli.common.AnsiColors.*;

public class BoxOutputFormatter implements OutputFormatter {

    @Override
    public String formatConnection(CliConnection connection) {

        AsciiArtTable asciiArtTable = new AsciiArtTable();
        asciiArtTable.setNoHeaderColumns(2);

        AsciiArtContentGroup connectionContentGroup = new AsciiArtContentGroup(YELLOW_BOLD_BRIGHT + CliConnection.HEADER + ANSI_RESET);
        connectionContentGroup.setNoHeaderColumns(2);

        connectionContentGroup.add(GREEN_BOLD_BRIGHT + "Host:" + ANSI_RESET, CYAN_BOLD_BRIGHT + connection.getHost() + ANSI_RESET);
        connectionContentGroup.add(GREEN_BOLD_BRIGHT + "Port:" + ANSI_RESET, CYAN_BOLD_BRIGHT + connection.getPort() + ANSI_RESET);
        connectionContentGroup.add(GREEN_BOLD_BRIGHT + "Protocol Magic:" + ANSI_RESET, CYAN_BOLD_BRIGHT + connection.getProtocolMagic() + ANSI_RESET);

        asciiArtTable.addContentGroup(connectionContentGroup);

        return asciiArtTable.getOutput();
    }

    @Override
    public String formatBlock(CliBlock block) {
        int numColumns = 6;
        AsciiArtTable asciiArtTable = new AsciiArtTable();
        asciiArtTable.setNoHeaderColumns(numColumns);

        AsciiArtContentGroup blockContentGroup = new AsciiArtContentGroup(YELLOW_BOLD_BRIGHT + CliBlock.HEADER + ANSI_RESET);
        blockContentGroup.setNoHeaderColumns(2);
        blockContentGroup.add(GREEN_BOLD_BRIGHT + "Block:     " + ANSI_RESET, CYAN_BOLD_BRIGHT + block.getBlockNumber() + ANSI_RESET);
        asciiArtTable.addContentGroup(blockContentGroup);

        if (block.isShowGrouping()) {
            formatBlockAmount(numColumns, "Outputs", block.getGroupingAmount().getTotalAda(), block.getGroupingAmount().getTokenList(), asciiArtTable);
        } else {
            block.getReceivers().forEach(cliReceiver -> {
                String header = "Receiver: " + cliReceiver.getReceiver();
                formatBlockAmount(numColumns, header, cliReceiver.getReceiverAmount().getTotalAda(), cliReceiver.getReceiverAmount().getTokenList(), asciiArtTable);
            });

            if (block.isShowMint()) {
                formatBlockAmount(numColumns, "Mints", block.getMintTokens().getTotalAda(), block.getMintTokens().getTokenList(), asciiArtTable);
            }
        }

        if (block.isShowInput()) {
            AsciiArtContentGroup inputContentGroup = new AsciiArtContentGroup(YELLOW_BOLD_BRIGHT + "Input" + ANSI_RESET);
            inputContentGroup.setNoHeaderColumns(1);
            asciiArtTable.addContentGroup(inputContentGroup);

            AsciiArtContentGroup transactionContentGroup = new AsciiArtContentGroup(GREEN_BOLD_BRIGHT + "Transactions:" + ANSI_RESET);
            transactionContentGroup.setNoHeaderColumns(1);
            transactionContentGroup.addList(new ArrayList<>(block.getInputs()));
            asciiArtTable.addContentGroup(transactionContentGroup);
        }

        return asciiArtTable.getOutput();
    }

    private void formatBlockAmount(int numColumns, String header, double totalAda, List<String> tokens, AsciiArtTable asciiArtTable) {

        AsciiArtContentGroup amountContentGroup = new AsciiArtContentGroup(YELLOW_BOLD_BRIGHT + header + ANSI_RESET);
        amountContentGroup.setNoHeaderColumns(2);
        amountContentGroup.add(GREEN_BOLD_BRIGHT + "Ada:       " + ANSI_RESET, CYAN_BOLD_BRIGHT + totalAda + ANSI_RESET);

        asciiArtTable.addContentGroup(amountContentGroup);

        AsciiArtContentGroup tokenContentGroup = new AsciiArtContentGroup(GREEN_BOLD_BRIGHT + "Tokens:" + ANSI_RESET);
        tokenContentGroup.setNoHeaderColumns(numColumns);
        tokenContentGroup.setMaxColumnWidth(15);

        tokenContentGroup.addList(new ArrayList<>(tokens));

        asciiArtTable.addContentGroup(tokenContentGroup);

    }
}
