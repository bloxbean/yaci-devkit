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

    public static void main(String[] args) {
        BoxOutputFormatter boxOutputFormatter = new BoxOutputFormatter();
        CliConnection connection = CliConnection.builder()
                .host("relays-new.cardano-mainnet.iohk.io")
                .port(3001)
                .protocolMagic(764824073)
                .build();

        //String formattedConnection = boxOutputFormatter.formatConnection(connection);
        //System.out.println(formattedConnection);

        CliBlock block = CliBlock
                .builder()
                .blockNumber(7669983)
                .groupingAmount(CliAmount.builder()
                        .totalAda(718.360769)
                        .tokenList(List.of("d�C2p^�ȁ!�8I�8�Z�wW�ޒ1�6�* : 1", "ADAPunks5916 : 1", "MetAliensVRclub01838 : 1", "ADAPunks5914 : 1", "YellowGang090 : 1"))
                        .build())
                .inputs(List.of("TxIn: 80c963f461d3aaf62809cf478c996aa36ce968fa98d73169d3181a37867dd874#1", "TxIn: 5495a4011ad01e61d02becdf50ff3fbf5106119f0ba098e89c66735755545922#1"))
                .build();

        block.setShowGrouping(true);
        block.setShowInput(true);
        String formattedBlock = boxOutputFormatter.formatBlock(block);

        System.out.println(formattedBlock);
    }
}
