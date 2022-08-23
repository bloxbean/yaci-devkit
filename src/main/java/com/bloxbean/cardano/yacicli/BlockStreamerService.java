package com.bloxbean.cardano.yacicli;

import com.bloxbean.cardano.yaci.core.common.Constants;
import com.bloxbean.cardano.yaci.core.model.Amount;
import com.bloxbean.cardano.yaci.core.model.TransactionBody;
import com.bloxbean.cardano.yaci.core.model.TransactionInput;
import com.bloxbean.cardano.yaci.core.model.TransactionOutput;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.core.protocol.handshake.messages.VersionTable;
import com.bloxbean.cardano.yaci.core.protocol.handshake.util.N2NVersionTableConstant;
import com.bloxbean.cardano.yaci.core.reactive.BlockStreamer;
import com.bloxbean.cardano.yacicli.common.ConsoleHelper;
import com.bloxbean.cardano.yacicli.common.PromptColor;
import com.bloxbean.cardano.yacicli.common.ShellHelper;
import com.bloxbean.cardano.yacicli.common.Tuple;
import com.bloxbean.cardano.yacicli.rule.RuleService;
import lombok.extern.slf4j.Slf4j;
import org.fusesource.jansi.Ansi;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.core.util.Constants.LOVELACE;
import static com.bloxbean.cardano.yacicli.common.AnsiColors.*;
import static com.bloxbean.cardano.yacicli.util.AdaConversionUtil.lovelaceToAda;
import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.writeLn;
import static java.util.stream.Collectors.toMap;
import static org.fusesource.jansi.Ansi.Color.*;
import static org.fusesource.jansi.Ansi.ansi;

@Component
@Slf4j
public class BlockStreamerService {
    private final Random rand = new Random();
    private final Ansi.Color[] colors = new Ansi.Color[]{BLACK, RED, YELLOW, BLUE, MAGENTA, CYAN, WHITE, DEFAULT};
    private ConsoleHelper consoleHelper = new ConsoleHelper();

    private RuleService ruleService;
    private ShellHelper shellHelper;

    public BlockStreamerService(RuleService ruleService, ShellHelper shellHelper) {
        this.ruleService = ruleService;
        this.shellHelper = shellHelper;
    }

    public void tail(String host, int port, long protocolMagic, long slot, String blockHash,
                     boolean showMint, boolean showInputs, boolean showOutputs, boolean grouping) throws InterruptedException {
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

        writeLn(shellHelper.getColored("=========================", PromptColor.MAGENTA));
        writeLn(BLUE_BOLD + "Connection Info" + ANSI_RESET);
        writeLn(shellHelper.getColored("=========================", PromptColor.MAGENTA));
        writeLn(ANSI_YELLOW + "Host          : %s", host + ANSI_RESET);
        writeLn(ANSI_YELLOW + "Port          : %s", port + ANSI_RESET);
        writeLn(ANSI_YELLOW + "ProtocolMagic : %s", protocolMagic + ANSI_RESET);

        Flux<List<TransactionBody>> stream = BlockStreamer.fromLatest(host, port, versionTable, wellKnownPoint)
                        .map(block -> {
                            System.out.println("");
                            System.out.println("");
                            writeLn(RED_BACKGROUND_BRIGHT + BLACK_BOLD + "Block : %s", block.getHeader().getHeaderBody().getBlockNumber() + ANSI_RESET);
                            writeLn(YELLOW_BOLD + "=================================================================" + ANSI_RESET);
                            return block.getTransactionBodies();
                        });

        Disposable disposable = stream.subscribe(transactionBodies -> {
            if (!grouping) {
                AtomicInteger counter = new AtomicInteger();

                transactionBodies.stream().forEach(transactionBody -> {
                    printTransactionHeader(transactionBody, counter.getAndIncrement());
                    if (showInputs)
                        printInputs(transactionBody.getInputs());

                    if (showMint)
                        printMint(transactionBody.getMint());

                    if (showOutputs) {
                        System.out.println(YELLOW_BACKGROUND_BRIGHT + BLACK_BOLD + "Outputs" + ANSI_RESET);
                        transactionBody.getOutputs().forEach(transactionOutput -> printOutput(transactionOutput));
                    }
                });

            } else {
                List<Amount> amounts = transactionBodies.stream()
                        .flatMap(transactionBody ->
                                transactionBody.getOutputs().stream()
                                        .flatMap(txOutput -> txOutput.getAmounts().stream()))
                        .collect(Collectors.toList());

                if (showOutputs)
                    printAmount(amounts);

                //Print Inputs
                List<TransactionInput> inputs = transactionBodies.stream()
                        .flatMap(transactionBody -> transactionBody.getInputs().stream())
                        .collect(Collectors.toList());

                if (showInputs)
                    printInputs(inputs);
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
                consoleHelper.animate(ANSI_YELLOW + "Waiting for next block..." + ANSI_RESET);
            }
        });
        waitThread.start();
        return waitThread;
    }

    private void printTransactionHeader(TransactionBody transactionBody, int index) {
        System.out.println("\n");
        writeLn(GREEN_BACKGROUND_BRIGHT + BLACK_BOLD + index + "> Transaction #" + transactionBody.getTxHash() + ANSI_RESET);
    }

    private void printMint(List<Amount> mint) {
        if (mint != null && mint.size() > 0) {
            System.out.println(YELLOW_BACKGROUND_BRIGHT + BLACK_BOLD + "Mint tokens" + ANSI_RESET);
            printNonGroupAmount(mint);
        }
    }

    private void printOutput(TransactionOutput output) {
        //Print Address
        System.out.println("\n");
        System.out.println(BLACK_BOLD + "Receiver : " + ANSI_RESET + output.getAddress());
        printNonGroupAmount(output.getAmounts());
    }

    private void printAmount(List<Amount> amounts) {
        Map<String, BigInteger> assetAmountsMap = amounts.stream()
                .collect(toMap(
                        amount -> amount.getAssetName(),
                        amount -> amount.getQuantity(),
                        (qty1, qty2) -> qty1.add(qty2)
                ));

        BigInteger lovelaceAmt = assetAmountsMap.get(LOVELACE);
        if (lovelaceAmt == null) lovelaceAmt = BigInteger.ZERO;
        if ( lovelaceAmt != BigInteger.ZERO) {
            System.out.print(BLACK_BOLD + "Ada      : " + ANSI_RESET);
            System.out.print(RED_BOLD + lovelaceToAda(lovelaceAmt).toString() + ANSI_RESET);
        }
       // writeLn(ANSI_YELLOW + "\n===================================" + ANSI_RESET);
        writeLn("");
        writeLn(YELLOW_BACKGROUND_BRIGHT + BLACK_BOLD + "Tokens    : " + ANSI_RESET);
        writeLn("");
       // writeLn(ANSI_YELLOW + "====================================" + ANSI_RESET);

        assetAmountsMap.remove("lovelace"); //Already printed above
        assetAmountsMap.forEach(
                (token, qty) -> {
                    Tuple<Ansi.Color, Ansi.Color> colorTuple = getRandomColor();
                    System.out.print(" ");
                    System.out.print(ansi().fg(colorTuple._2).bg(colorTuple._1).render(token).reset().toString());
                    System.out.print( " : " + qty + " || ");
                    System.out.print(ANSI_RESET);
                }
        );

        writeLn(ANSI_RESET);
    }

    private void printNonGroupAmount(List<Amount> amounts) {
        Map<String, BigInteger> assetAmountsMap = amounts.stream()
                .collect(toMap(
                        amount -> amount.getAssetName(),
                        amount -> amount.getQuantity(),
                        (qty1, qty2) -> qty1.add(qty2)
                ));

        BigInteger lovelaceAmt = assetAmountsMap.get(LOVELACE);
        if (lovelaceAmt == null) lovelaceAmt = BigInteger.ZERO;
        if ( lovelaceAmt != BigInteger.ZERO) {
            System.out.print(BLACK_BOLD + "Ada      : " + ANSI_RESET);
            System.out.print(RED_BOLD + lovelaceToAda(lovelaceAmt).toString() + ANSI_RESET);
        }
        System.out.println("");
        assetAmountsMap.remove("lovelace"); //Already printed above
        System.out.print(BLACK_BOLD + "Tokens   :" + ANSI_RESET);
        assetAmountsMap.forEach(
                (token, qty) -> {
                    Tuple<Ansi.Color, Ansi.Color> colorTuple = getRandomColor();
                    System.out.print(" ");
                    System.out.print(ansi().fg(colorTuple._2).bg(colorTuple._1).render(token).reset().toString());
                    System.out.print( " : " + qty + ", ");
                    System.out.print(ANSI_RESET);
                }
        );
    }

    private void printInputs(Collection<TransactionInput> inputs) {
        writeLn("");
        writeLn(YELLOW_BACKGROUND_BRIGHT + BLACK_BOLD + "Inputs" + ANSI_RESET);
        writeLn("");
        inputs.forEach(input -> {
            System.out.println(ANSI_GREEN_BACKGROUND + "TxIn" + ANSI_RESET + " " + input.getTransactionId() + "#" + input.getIndex());
        });
        writeLn(ANSI_RESET);
    }

    private Tuple<Ansi.Color, Ansi.Color> getRandomColor() {
        int index = rand.nextInt(colors.length-1 - 0) + 0;

        Ansi.Color fgColor = BLACK;
        if (index == 0 || index == 1 || index == 3 || index == 4)
            fgColor = WHITE;

        return new Tuple(colors[index], fgColor);
    }
}
