package com.bloxbean.cardano.yacicli.commands.tail;

import com.bloxbean.cardano.yaci.core.common.Constants;
import com.bloxbean.cardano.yaci.core.model.Amount;
import com.bloxbean.cardano.yaci.core.model.TransactionBody;
import com.bloxbean.cardano.yaci.core.model.TransactionInput;
import com.bloxbean.cardano.yaci.core.model.TransactionOutput;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.core.protocol.handshake.messages.VersionTable;
import com.bloxbean.cardano.yaci.core.protocol.handshake.util.N2NVersionTableConstant;
import com.bloxbean.cardano.yaci.core.reactive.BlockStreamer;
import com.bloxbean.cardano.yacicli.commands.tail.model.CliReceiver;
import com.bloxbean.cardano.yacicli.common.ConsoleHelper;
import com.bloxbean.cardano.yacicli.commands.tail.model.CliAmount;
import com.bloxbean.cardano.yacicli.commands.tail.model.CliBlock;
import com.bloxbean.cardano.yacicli.commands.tail.model.CliConnection;
import com.bloxbean.cardano.yacicli.output.OutputFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.core.util.Constants.LOVELACE;
import static com.bloxbean.cardano.yacicli.common.AnsiColors.*;
import static com.bloxbean.cardano.yacicli.util.AdaConversionUtil.lovelaceToAda;
import static java.util.stream.Collectors.toMap;

@Component
@Slf4j
public class BlockStreamerService {

    private final ConsoleHelper consoleHelper = new ConsoleHelper();

    public BlockStreamerService() {
    }

    public void tail(String host, int port, long protocolMagic, long slot, String blockHash,
                     boolean showMint, boolean showInputs, boolean showOutputs, boolean grouping, OutputFormatter outputFormatter) throws InterruptedException {

        if (!StringUtils.hasLength(host)) {
            host = Constants.MAINNET_IOHK_RELAY_ADDR;
        }

        if (port == 0) {
            port = Constants.MAINNET_IOHK_RELAY_PORT;
        }

        if (protocolMagic == 0) {
            protocolMagic = Constants.MAINNET_PROTOCOL_MAGIC;
        }

        Point wellKnownPoint = null;
        if (slot !=0 && StringUtils.hasLength(blockHash))
            wellKnownPoint = new Point(slot, blockHash);
        else
            wellKnownPoint = Constants.WELL_KNOWN_MAINNET_POINT;

        VersionTable versionTable = N2NVersionTableConstant.v4AndAbove(protocolMagic);

        CliConnection connection = CliConnection.builder()
                .host(host)
                .port(port)
                .protocolMagic(protocolMagic)
                .build();

        String formattedConnection = outputFormatter.formatConnection(connection);
        System.out.println(formattedConnection);

        final CliBlock outputBlock = new CliBlock();

        Flux<List<TransactionBody>> stream = BlockStreamer.fromLatest(host, port, versionTable, wellKnownPoint)
                .stream()
                .map(block -> {
                    outputBlock.setBlockNumber(block.getHeader().getHeaderBody().getBlockNumber());
                    return block.getTransactionBodies();
                });

        Disposable disposable = stream.subscribe(transactionBodies -> {
            if (!grouping) {

                transactionBodies.stream().forEach(transactionBody -> {

                    if (showInputs) {
                        outputBlock.setShowInput(true);
                        setInputs(transactionBody.getInputs(), outputBlock);
                    }

                    if (showMint){
                        outputBlock.setShowMint(true);
                        setMint(transactionBody.getMint(), outputBlock);
                    }

                    if (showOutputs) {
                        outputBlock.setShowOutput(true);
                        transactionBody.getOutputs().forEach(transactionOutput -> setOutput(transactionOutput, outputBlock));
                    }
                });

                String formattedBlock = outputFormatter.formatBlock(outputBlock);
                System.out.println();
                System.out.println(formattedBlock);
                outputBlock.getInputs().clear();

            } else {

                outputBlock.setShowGrouping(true);

                List<Amount> amounts = transactionBodies.stream()
                        .flatMap(transactionBody ->
                                transactionBody.getOutputs().stream()
                                        .flatMap(txOutput -> txOutput.getAmounts().stream()))
                        .collect(Collectors.toList());

                if (showOutputs) {
                    outputBlock.setShowOutput(true);
                    setGroupAmount(amounts, outputBlock);
                }

                //Print Inputs
                List<TransactionInput> inputs = transactionBodies.stream()
                        .flatMap(transactionBody -> transactionBody.getInputs().stream())
                        .collect(Collectors.toList());

                if (showInputs) {
                    outputBlock.setShowInput(true);
                    setInputs(inputs, outputBlock);
                }

                String formattedBlock = outputFormatter.formatBlock(outputBlock);
                System.out.println();
                System.out.println(formattedBlock);
                outputBlock.getInputs().clear();
            }
        });



        Thread waitThread = startWaitThread();
        try {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            countDownLatch.await();
        } catch (InterruptedException ex) {
            waitThread.interrupt();
            disposable.dispose();
        } finally {
            if (disposable != null)
                disposable.dispose();
        }
    }

    private Thread startWaitThread() {
        Thread waitThread = new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                   break;
                }
                consoleHelper.animate(YELLOW_BOLD + "Waiting for next block..." + ANSI_RESET);
            }
        });
        waitThread.start();
        return waitThread;
    }

    private void setMint(List<Amount> mint, CliBlock block) {
        if (mint != null && mint.size() > 0) {
            block.setMintTokens(getAmount(mint));
        }
    }

    private void setOutput(TransactionOutput output, CliBlock block) {
        CliReceiver cliReceiver = new CliReceiver();
        cliReceiver.setReceiver(output.getAddress());
        cliReceiver.setReceiverAmount(getAmount(output.getAmounts()));
        block.getReceivers().add(cliReceiver);
    }

    private void setGroupAmount(List<Amount> amounts, CliBlock outputBlock) {
        Map<String, BigInteger> assetAmountsMap = amounts.stream()
                .collect(toMap(
                        amount -> amount.getAssetName(),
                        amount -> amount.getQuantity(),
                        (qty1, qty2) -> qty1.add(qty2)
                ));

        BigInteger lovelaceAmt = assetAmountsMap.get(LOVELACE);
        if (lovelaceAmt == null) lovelaceAmt = BigInteger.ZERO;
        CliAmount cliAmount = new CliAmount();
        if ( lovelaceAmt != BigInteger.ZERO) {
            cliAmount.setTotalAda(lovelaceToAda(lovelaceAmt).doubleValue());
        }

        assetAmountsMap.remove("lovelace"); //Already printed above
        assetAmountsMap.forEach(
                (token, qty) -> {
                    cliAmount.getTokens().add(token + " : " + qty);
                }
        );

        outputBlock.setGroupingAmount(cliAmount);
    }

    private CliAmount getAmount(List<Amount> amounts) {

        CliAmount cliAmount = new CliAmount();

        Map<String, BigInteger> assetAmountsMap = amounts.stream()
                .collect(toMap(
                        amount -> amount.getAssetName(),
                        amount -> amount.getQuantity(),
                        (qty1, qty2) -> qty1.add(qty2)
                ));

        BigInteger lovelaceAmt = assetAmountsMap.get(LOVELACE);
        if (lovelaceAmt == null) lovelaceAmt = BigInteger.ZERO;
        cliAmount.setTotalAda(lovelaceToAda(lovelaceAmt).doubleValue());

        assetAmountsMap.remove("lovelace"); //Already printed above
        assetAmountsMap.forEach(
                (token, qty) -> {
                    cliAmount.getTokens().add(token + " : " + qty);
                }
        );

        return cliAmount;
    }

    private void setInputs(Collection<TransactionInput> inputs, CliBlock outputBlock) {
        inputs.forEach(input -> {
            outputBlock.getInputs().add("TxIn: " + input.getTransactionId() + "#" + input.getIndex());
        });
    }

}

