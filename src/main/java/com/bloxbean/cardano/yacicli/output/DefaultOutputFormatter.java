package com.bloxbean.cardano.yacicli.output;

import com.bloxbean.cardano.yacicli.commands.tail.model.CliBlock;
import com.bloxbean.cardano.yacicli.commands.tail.model.CliConnection;
import com.bloxbean.cardano.yacicli.commands.tail.model.Token;
import com.bloxbean.cardano.yacicli.commands.tail.themes.ColorMode;
import com.bloxbean.cardano.yacicli.commands.tail.themes.ColorModeFactory;
import com.bloxbean.cardano.yacicli.common.PromptColor;
import com.bloxbean.cardano.yacicli.common.ShellHelper;

import java.math.BigInteger;
import java.util.List;

import static com.bloxbean.cardano.yacicli.common.AnsiColors.ANSI_RESET;
import static com.bloxbean.cardano.yacicli.util.AdaConversionUtil.lovelaceToAda;
import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.strLn;

public class DefaultOutputFormatter implements OutputFormatter {
    private ShellHelper shellHelper;
    private ColorMode colorMode;

    public DefaultOutputFormatter(ShellHelper shellHelper) {
        this.shellHelper = shellHelper;
        this.colorMode = ColorModeFactory.getColorMode();
    }

    @Override
    public String formatConnection(CliConnection connection) {
        return shellHelper.getColored("=========================", PromptColor.YELLOW) +
                colorMode.getConnectionHeaderColor() + "Connection Info" + ANSI_RESET +
                shellHelper.getColored("=========================", PromptColor.YELLOW) +
                "\n" +
                strLn(colorMode.getConnectionLabelColor() + "Host          : " + colorMode.getConnectionValueColor() + "%s", connection.getHost() + ANSI_RESET) +
                strLn(colorMode.getConnectionLabelColor() + "Port          : " + colorMode.getConnectionValueColor() + "%s", connection.getPort() + ANSI_RESET) +
                strLn(colorMode.getConnectionLabelColor() + "ProtocolMagic : " + colorMode.getConnectionValueColor() + "%s", connection.getProtocolMagic() + ANSI_RESET);
    }

    @Override
    public String formatBlock(CliBlock block) {

        StringBuilder blockBuilder = new StringBuilder();
        blockBuilder.append("\n");
        blockBuilder.append(strLn(colorMode.getBlockHeaderBg() + colorMode.getBlockHeaderColor() + "Block : %s", block.getBlockNumber() + ANSI_RESET));

        if (block.isShowGrouping()) {
            blockBuilder.append("\n");
            formatGroupBlockAmount("Outputs", block.getGroupingAmount().getTotalAda(), block.getGroupingAmount().getTokens(), block, blockBuilder);
            blockBuilder.append("\n");
        } else {
            block.getReceivers().forEach(cliReceiver -> {
                blockBuilder.append("\n");
                blockBuilder.append("\n");
                blockBuilder.append(strLn(colorMode.getReceiverLabelColor() + String.format("%-14s", "Receiver ") + ": " + colorMode.getReceiverValueColor() + cliReceiver.getReceiver() + ANSI_RESET));

                formatBlockAmount(null, cliReceiver.getReceiverAmount().getTotalAda(), cliReceiver.getReceiverAmount().getTokens(), blockBuilder);
                blockBuilder.append("\n");
            });

            if (block.isShowMint()) {
                blockBuilder.append("\n");
                if (block.getMintTokens() != null) {
                    formatMintBlock("Mints", block.getMintTokens().getTokens(), blockBuilder);
                }
                blockBuilder.append("\n");
            }
        }

        if (block.isShowInput()) {
            blockBuilder.append("\n");
            blockBuilder.append("\n");

            blockBuilder.append(strLn(colorMode.getInputsHeaderColor() + "Inputs" + ANSI_RESET));
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

    private void formatBlockAmount(String header, double totalAda, List<Token> tokens, StringBuilder blockBuilder) {

        if (header != null)
            blockBuilder.append(strLn(colorMode.getReceiverLabelColor() + header + ANSI_RESET));

        if (totalAda != 0)
            blockBuilder.append(strLn(colorMode.getAdaTokenLabelColor() + String.format("%-14s", "Ada ") + ": " + ANSI_RESET + colorMode.getAdaTokenAmountColor() + "%s", totalAda + ANSI_RESET));

        if (!tokens.isEmpty()) {
            blockBuilder.append("\n\n");
            blockBuilder.append(strLn(colorMode.getAdaTokenLabelColor() + String.format("%-14s", "Tokens ") + ": " + ANSI_RESET));

            tokens.forEach(token -> {
                String fgColor = colorMode.getTokenColor();
                blockBuilder.append(" ");
                blockBuilder.append(fgColor + truncateTokenNameIfRequired(token.getName()) + " : " + colorMode.getAdaTokenAmountColor() + token.getAmount().toString() + ANSI_RESET + ", ");
                blockBuilder.append(ANSI_RESET);
            });
        }
    }

    private void formatGroupBlockAmount(String header, double totalAda, List<Token> tokens, CliBlock block, StringBuilder blockBuilder) {

        if (header != null)
            blockBuilder.append(strLn(colorMode.getInputsHeaderColor() + header + ANSI_RESET));

        blockBuilder.append(strLn("-".repeat(80)));

        if (totalAda != 0)
            blockBuilder.append(strLn(colorMode.getAdaTokenLabelColor() + String.format("%-14s", "Ada ") + ": " + ANSI_RESET + colorMode.getAdaTokenAmountColor() + "%s", totalAda + ANSI_RESET));

        if (block.getTotalFees() != 0)
            blockBuilder.append(strLn(colorMode.getAdaTokenLabelColor() + String.format("%-14s", "Total Fees ") + ": " + ANSI_RESET + colorMode.getAdaTokenAmountColor() + "%s",
                    lovelaceToAda(BigInteger.valueOf(block.getTotalFees())) + ANSI_RESET));

        blockBuilder.append(strLn(colorMode.getAdaTokenLabelColor() + String.format("%-14s", "Transactions") + ": " + ANSI_RESET + colorMode.getAdaTokenAmountColor() + "%s", block.getTotalTxs() + ANSI_RESET));
        blockBuilder.append(strLn(colorMode.getAdaTokenLabelColor() + String.format("%-14s", "Block Size") + ": " + ANSI_RESET + colorMode.getAdaTokenAmountColor() + "%s", block.getBlockSize() / 1000 + " KB" + ANSI_RESET));

        if (block.getNoPlutusV1Scripts() != 0)
            blockBuilder.append(strLn(colorMode.getAdaTokenLabelColor() + String.format("%-14s", "# of PlutusV1") + ": " + ANSI_RESET + colorMode.getAdaTokenAmountColor() + "%s", block.getNoPlutusV1Scripts() + ANSI_RESET));

        if (block.getNoPlutusV2Scripts() != 0)
            blockBuilder.append(strLn(colorMode.getAdaTokenLabelColor() + String.format("%-14s", "# of PlutusV2") + ": " + ANSI_RESET + colorMode.getAdaTokenAmountColor() + "%s", block.getNoPlutusV2Scripts() + ANSI_RESET));

        if (!tokens.isEmpty()) {
            blockBuilder.append("\n\n");
            blockBuilder.append(strLn(colorMode.getAdaTokenLabelColor() + String.format("%-14s", "Tokens ") + ": " + ANSI_RESET));

            tokens.forEach(token -> {
                String fgColor = colorMode.getTokenColor();
                blockBuilder.append(" ");
                blockBuilder.append(fgColor + truncateTokenNameIfRequired(token.getName()) + " : " + colorMode.getAdaTokenAmountColor() + token.getAmount().toString() + ANSI_RESET + ", ");
                blockBuilder.append(ANSI_RESET);
            });
        }

        if (block.isShowMint() && block.getMintTokens() != null) {
            blockBuilder.append("\n\n");
            formatMintBlock("Mints", block.getMintTokens().getTokens(), blockBuilder);
        }

        if (block.isShowMetadata() && block.getMetadataList() != null && !block.getMetadataList().isEmpty()) {
            blockBuilder.append("\n\n");
            blockBuilder.append(strLn(colorMode.getAdaTokenLabelColor() + String.format("%-14s", "Metadata ") + ANSI_RESET));

            block.getMetadataList().forEach(metadata -> {
                blockBuilder.append(strLn(colorMode.getInputsColor() + metadata + ANSI_RESET));
            });
        }

        if (block.isShowInlineDatums() && block.getInlineDatums() != null && !block.getInlineDatums().isEmpty()) {
            blockBuilder.append("\n\n");
            blockBuilder.append(strLn(colorMode.getAdaTokenLabelColor() + String.format("%-14s", "InlineDatums ") + ANSI_RESET));

            block.getInlineDatums().forEach(inlineDatum -> {
                blockBuilder.append(strLn(colorMode.getInputsColor() + inlineDatum + ANSI_RESET));
            });
        }

        if (block.isShowDatumhases() && block.getDatumHashes() != null && !block.getDatumHashes().isEmpty()) {
            blockBuilder.append("\n\n");
            blockBuilder.append(strLn(colorMode.getAdaTokenLabelColor() + String.format("%-14s", "DatumHash(s) ") + ANSI_RESET));

            block.getDatumHashes().forEach(datumHash -> {
                blockBuilder.append(strLn(colorMode.getInputsColor() + datumHash + ANSI_RESET));
            });
        }
    }

    private void formatMintBlock(String header, List<Token> tokens, StringBuilder blockBuilder) {
        blockBuilder.append(strLn(colorMode.getMintLabelColor() + header + ANSI_RESET));
        if (!tokens.isEmpty()) {
            tokens.forEach(token -> {
                String symbol = "\uD83D\uDD25";
                if (BigInteger.ZERO.compareTo(token.getAmount()) == 1)
                    symbol = "\uD83D\uDD25"; //burn
                else
                    symbol = "⛏️";
                String fgColor = colorMode.getTokenColor();
                blockBuilder.append(" ");
                blockBuilder.append(fgColor + truncateTokenNameIfRequired(token.getName()) + " : " + colorMode.getAdaTokenAmountColor() + token.getAmount().toString() + ANSI_RESET + symbol + ", ");
                blockBuilder.append(ANSI_RESET);
            });
        }
    }


    private String truncateTokenNameIfRequired(String token) {
        String tokenName = null;
        if (token.length() > 13) {
            tokenName = org.apache.commons.lang3.StringUtils.abbreviateMiddle(token, "..", 18);
        } else
            tokenName = token;
        return tokenName;
    }
}
