package com.bloxbean.cardano.yacicli.output;

import com.bloxbean.cardano.yacicli.common.PromptColor;
import com.bloxbean.cardano.yacicli.common.ShellHelper;
import com.bloxbean.cardano.yacicli.commands.tail.model.CliBlock;
import com.bloxbean.cardano.yacicli.commands.tail.model.CliConnection;

import java.util.List;
import java.util.Random;

import static com.bloxbean.cardano.yacicli.common.AnsiColors.*;
import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.strLn;

public class DefaultOutputFormatter implements OutputFormatter{

    private final Random rand = new Random();
    private final String[] colors = new String[]{
            BLACK_BOLD, RED_BOLD, YELLOW_BOLD, BLUE_BOLD, CYAN_BOLD, WHITE_BOLD
    };
    private ShellHelper shellHelper;

    public DefaultOutputFormatter(ShellHelper shellHelper){
        this.shellHelper = shellHelper;
    }

    @Override
    public String formatConnection(CliConnection connection) {

        return shellHelper.getColored("=========================", PromptColor.YELLOW) +
                YELLOW_BOLD_BRIGHT + "Connection Info" + ANSI_RESET +
                shellHelper.getColored("=========================", PromptColor.YELLOW) +
                "\n" +
                strLn(CYAN_BOLD_BRIGHT + "Host          : " + GREEN_BOLD_BRIGHT + "%s", connection.getHost() + ANSI_RESET) +
                strLn(CYAN_BOLD_BRIGHT + "Port          : " + GREEN_BOLD_BRIGHT + "%s", connection.getPort() + ANSI_RESET) +
                strLn(CYAN_BOLD_BRIGHT + "ProtocolMagic : " + GREEN_BOLD_BRIGHT + "%s", connection.getProtocolMagic() + ANSI_RESET);
    }

    @Override
    public String formatBlock(CliBlock block) {

        StringBuilder blockBuilder = new StringBuilder();
        blockBuilder.append("\n");

        blockBuilder.append(strLn(RED_BACKGROUND_BRIGHT + BLACK_BOLD + "Block : %s", block.getBlockNumber() + ANSI_RESET));
        blockBuilder.append(strLn(YELLOW_BOLD + "=================================================================" + ANSI_RESET));

        if (block.isShowGrouping()) {
            blockBuilder.append("\n");
            formatBlockAmount("Output", block.getGroupingAmount().getTotalAda(), block.getGroupingAmount().getTokens(), blockBuilder);
            blockBuilder.append("\n");
        }  else {
            if ( block.isShowOutput() ){
                block.getReceivers().forEach( cliReceiver -> {
                    blockBuilder.append("\n");
                    blockBuilder.append("\n");
                    String header = "Receiver: " + cliReceiver.getReceiver();
                    blockBuilder.append(strLn(RED_BOLD_BRIGHT + "=".repeat(header.length()) + ANSI_RESET));
                    formatBlockAmount(header, cliReceiver.getReceiverAmount().getTotalAda(), cliReceiver.getReceiverAmount().getTokens(), blockBuilder);
                    blockBuilder.append("\n");
                    blockBuilder.append(strLn(RED_BOLD_BRIGHT + "=".repeat(header.length()) + ANSI_RESET));
                });
            }
            if ( block.isShowMint() ){
                blockBuilder.append("\n");
                if ( block.getMintTokens() != null ) {
                    formatBlockAmount("Mints", block.getMintTokens().getTotalAda(), block.getMintTokens().getTokens(), blockBuilder);
                }
                blockBuilder.append("\n");
            }
        }

        if (block.isShowInput()) {
            blockBuilder.append("\n");
            blockBuilder.append("\n");

            blockBuilder.append(strLn(YELLOW_BRIGHT + "Inputs" + ANSI_RESET));
            blockBuilder.append("\n");
            block.getInputs().forEach(input -> {
                blockBuilder.append(input);
                blockBuilder.append("\n");
            });
            blockBuilder.append(ANSI_RESET);
            blockBuilder.append("\n");
        }

        return blockBuilder.toString();
    }

    private void formatBlockAmount(String header, double totalAda, List<String> tokens, StringBuilder blockBuilder){

        blockBuilder.append(strLn(YELLOW_BRIGHT + header + ANSI_RESET ));

        blockBuilder.append(strLn(GREEN_BOLD_BRIGHT + "Ada: " + ANSI_RESET + CYAN_BOLD_BRIGHT + "%s",  totalAda + ANSI_RESET));
        blockBuilder.append("\n\n");
        blockBuilder.append(strLn(GREEN_BOLD_BRIGHT + "Tokens    : " + ANSI_RESET));

        tokens.forEach(token -> {
            String fgColor = getRandomColor();
            blockBuilder.append(" ");
            blockBuilder.append(fgColor + token + ANSI_RESET);
            blockBuilder.append(ANSI_RESET);
        });

    }

    private String getRandomColor() {
        int index = rand.nextInt(colors.length - 1);
        return colors[index];
    }
}
