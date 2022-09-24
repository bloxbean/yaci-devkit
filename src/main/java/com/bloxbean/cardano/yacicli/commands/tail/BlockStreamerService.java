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
import com.bloxbean.cardano.yacicli.commands.tail.model.*;
import com.bloxbean.cardano.yacicli.common.ConsoleHelper;
import com.bloxbean.cardano.yacicli.output.OutputFormatter;
import com.bloxbean.cardano.yacicli.rule.Result;
import com.bloxbean.cardano.yacicli.rule.RuleService;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.core.util.Constants.LOVELACE;
import static com.bloxbean.cardano.yacicli.common.AnsiColors.ANSI_RESET;
import static com.bloxbean.cardano.yacicli.common.AnsiColors.YELLOW_BOLD;
import static com.bloxbean.cardano.yacicli.util.AdaConversionUtil.lovelaceToAda;
import static java.util.stream.Collectors.toMap;

@Component
@Slf4j
public class BlockStreamerService {

    private final ConsoleHelper consoleHelper = new ConsoleHelper();
    private RuleService ruleService;

    public BlockStreamerService(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    public void tail(String host, int port, long protocolMagic, boolean showMint, boolean showInputs, boolean showMetadata,
                     boolean showDatumhash, boolean showInlineDatum, boolean grouping, OutputFormatter outputFormatter) throws InterruptedException {
        CliConnection cliConnection = CliConnection.builder()
                .host(host)
                .port(port)
                .protocolMagic(protocolMagic)
                .wellKnownPoint(Point.ORIGIN)
                .build();

        tail(cliConnection, showMint, showInputs, showMetadata, showDatumhash, showInlineDatum, grouping, outputFormatter);
    }

    public void tail(String host, int port, String network, long protocolMagic, long slot, String blockHash,
                     boolean showMint, boolean showInputs, boolean showMetadata, boolean showDatumhash, boolean showInlineDatum, boolean grouping, OutputFormatter outputFormatter) throws InterruptedException {

        CliConnection connection = getConnectionInfo(host, port, network, protocolMagic, slot, blockHash);
        if (connection == null) return;

        tail(connection, showMint, showInputs, showMetadata, showDatumhash, showInlineDatum, grouping, outputFormatter);
    }

    private void tail(CliConnection connection, boolean showMint, boolean showInputs, boolean showMetadata, boolean showDatumhash, boolean showInlineDatum, boolean grouping, OutputFormatter outputFormatter) {
        VersionTable versionTable = N2NVersionTableConstant.v4AndAbove(connection.getProtocolMagic());

        String formattedConnection = outputFormatter.formatConnection(connection);
        System.out.println(formattedConnection);

        final CliBlock outputBlock = new CliBlock();
        outputBlock.setShowMetadata(showMetadata);
        outputBlock.setShowDatumhases(showDatumhash);
        outputBlock.setShowInlineDatums(showInlineDatum);
        outputBlock.setShowInput(showInputs);
        outputBlock.setShowMint(showMint);
        outputBlock.setShowGrouping(grouping);

        Result ruleResult = new Result();

        Flux<List<TransactionBody>> stream = BlockStreamer.fromLatest(connection.getHost(), connection.getPort(), connection.getWellKnownPoint(), versionTable)
                .stream()
                .map(block -> {
                    //Initialize ruleResult
                    ruleResult.clear();
                    ruleService.executeRules(block, ruleResult);

                    outputBlock.setBlockNumber(block.getHeader().getHeaderBody().getBlockNumber());
                    outputBlock.setBlockSize(block.getHeader().getHeaderBody().getBlockBodySize());

                    List<String> metadataList = block.getAuxiliaryDataMap().values().stream()
                            .map(auxData -> auxData.getMetadataJson())
                            .collect(Collectors.toList());
                    outputBlock.setMetadataList(metadataList);

                    long nV1scripts = block.getTransactionWitness()
                            .stream().map(witnesses -> witnesses.getPlutusV1Scripts())
                            .filter(plutusScripts -> plutusScripts != null)
                            .flatMap(plutusScripts -> plutusScripts.stream())
                            .collect(Collectors.toList())
                            .size();

                    long nV2scripts = block.getTransactionWitness()
                            .stream().map(witnesses -> witnesses.getPlutusV2Scripts())
                            .filter(plutusScripts -> plutusScripts != null)
                            .flatMap(plutusScripts -> plutusScripts.stream())
                            .collect(Collectors.toList())
                            .size();

                    outputBlock.setNoPlutusV1Scripts(nV1scripts);
                    outputBlock.setNoPlutusV2Scripts(nV2scripts);

                    return block.getTransactionBodies();
                });

        Disposable disposable = stream.subscribe(transactionBodies -> {
            if (!grouping) {
                transactionBodies.stream().forEach(transactionBody -> {
                    if (showInputs) {
                        // outputBlock.setShowInput(true);
                        setInputs(transactionBody.getInputs(), outputBlock);
                    }

                    if (showMint) {
                        //outputBlock.setShowMint(true);
                        setMint(transactionBody.getMint(), outputBlock);
                    }

                    transactionBody.getOutputs().forEach(transactionOutput -> setOutput(transactionOutput, outputBlock));

                });
                String formattedBlock = outputFormatter.formatBlock(outputBlock);
                System.out.println();
                System.out.println(formattedBlock);
                outputBlock.getInputs().clear();

            } else {
                if (transactionBodies != null)
                    outputBlock.setTotalTxs(transactionBodies.size());

                AtomicLong totalFees = new AtomicLong(0);
                transactionBodies.forEach(transactionBody -> totalFees.addAndGet(transactionBody.getFee().longValue()));
                outputBlock.setTotalFees(totalFees.longValue());

                for (TransactionBody body : transactionBodies) {
                    List<String> inlineDatum = body.getOutputs().stream()
                            .map(transactionOutput -> transactionOutput.getInlineDatum())
                            .filter(iDatum -> iDatum != null)
                            .collect(Collectors.toList());
                    outputBlock.setInlineDatums(inlineDatum);

                    List<String> datumHash = body.getOutputs().stream().map(transactionOutput -> transactionOutput.getDatumHash())
                            .filter(dHash -> dHash != null)
                            .collect(Collectors.toList());
                    outputBlock.setDatumHashes(datumHash);
                }

                outputBlock.setShowGrouping(true);
                List<Amount> amounts = transactionBodies.stream()
                        .flatMap(transactionBody ->
                                transactionBody.getOutputs().stream()
                                        .flatMap(txOutput -> txOutput.getAmounts().stream()))
                        .collect(Collectors.toList());

                List<Amount> mintAmounts = transactionBodies.stream()
                        .flatMap(transactionBody -> transactionBody.getMint().stream())
                        .collect(Collectors.toList());

                setGroupAmount(amounts, outputBlock);

                if (showMint) {
                    //outputBlock.setShowMint(true);
                    setMint(mintAmounts, outputBlock);
                }

                //Print Inputs
                List<TransactionInput> inputs = transactionBodies.stream()
                        .flatMap(transactionBody -> transactionBody.getInputs().stream())
                        .collect(Collectors.toList());

                if (showInputs) {
                    // outputBlock.setShowInput(true);
                    setInputs(inputs, outputBlock);
                }

                String formattedBlock = outputFormatter.formatBlock(outputBlock);
                System.out.println();
                System.out.println(formattedBlock);
                outputBlock.getInputs().clear();
            }

            outputBlock.clear();
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

    private CliConnection getConnectionInfo(String host, int port, String network, long protocolMagic, long slot, String blockHash) {
        Point wellKnownPoint = null;
        if (StringUtils.hasLength(network)) {
            if ("mainnet".equals(network)) {
                if (!StringUtils.hasLength(host)) {
                    host = Constants.MAINNET_IOHK_RELAY_ADDR;
                }

                if (port == 0) {
                    port = Constants.MAINNET_IOHK_RELAY_PORT;
                }

                if (protocolMagic == 0) {
                    protocolMagic = Constants.MAINNET_PROTOCOL_MAGIC;
                }

                if (slot != 0 && StringUtils.hasLength(blockHash))
                    wellKnownPoint = new Point(slot, blockHash);
                else
                    wellKnownPoint = Constants.WELL_KNOWN_MAINNET_POINT;
            } else if ("legacy_testnet".equals(network)) {
                if (!StringUtils.hasLength(host)) {
                    host = Constants.TESTNET_IOHK_RELAY_ADDR;
                }

                if (port == 0) {
                    port = Constants.TESTNET_IOHK_RELAY_PORT;
                }

                if (protocolMagic == 0) {
                    protocolMagic = Constants.LEGACY_TESTNET_PROTOCOL_MAGIC;
                }

                if (slot != 0 && StringUtils.hasLength(blockHash))
                    wellKnownPoint = new Point(slot, blockHash);
                else
                    wellKnownPoint = Constants.WELL_KNOWN_TESTNET_POINT;
            } else if ("prepod".equals(network)) {
                if (!StringUtils.hasLength(host)) {
                    host = Constants.PREPOD_IOHK_RELAY_ADDR;
                }

                if (port == 0) {
                    port = Constants.PREPOD_IOHK_RELAY_PORT;
                }

                if (protocolMagic == 0) {
                    protocolMagic = Constants.PREPOD_PROTOCOL_MAGIC;
                }

                if (slot != 0 && StringUtils.hasLength(blockHash))
                    wellKnownPoint = new Point(slot, blockHash);
                else
                    wellKnownPoint = Constants.WELL_KNOWN_PREPOD_POINT;
            } else if ("preview".equals(network)) {
                if (!StringUtils.hasLength(host)) {
                    host = Constants.PREVIEW_IOHK_RELAY_ADDR;
                }

                if (port == 0) {
                    port = Constants.PREVIEW_IOHK_RELAY_PORT;
                }

                if (protocolMagic == 0) {
                    protocolMagic = Constants.PREVIEW_PROTOCOL_MAGIC;
                }

                if (slot != 0 && StringUtils.hasLength(blockHash))
                    wellKnownPoint = new Point(slot, blockHash);
                else
                    wellKnownPoint = Constants.WELL_KNOWN_PREVIEW_POINT;
            } else {
                System.out.println("[Error] Invalid Network");
                return null;
            }
        } else {
            if (!StringUtils.hasLength(host)) {
                host = Constants.MAINNET_IOHK_RELAY_ADDR;
            }

            if (port == 0) {
                port = Constants.MAINNET_IOHK_RELAY_PORT;
            }

            if (protocolMagic == 0) {
                protocolMagic = Constants.MAINNET_PROTOCOL_MAGIC;
            }

            if (slot != 0 && StringUtils.hasLength(blockHash))
                wellKnownPoint = new Point(slot, blockHash);
            else {
                if (protocolMagic == Constants.MAINNET_PROTOCOL_MAGIC)
                    wellKnownPoint = Constants.WELL_KNOWN_MAINNET_POINT;
                else
                    wellKnownPoint = Point.ORIGIN;
            }
        }


        CliConnection connection = CliConnection.builder()
                .host(host)
                .port(port)
                .protocolMagic(protocolMagic)
                .wellKnownPoint(wellKnownPoint)
                .build();
        return connection;
    }

    private Thread startWaitThread() {
        Thread waitThread = new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return;
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
        if (lovelaceAmt != BigInteger.ZERO) {
            cliAmount.setTotalAda(lovelaceToAda(lovelaceAmt).doubleValue());
        }

        assetAmountsMap.remove("lovelace"); //Already printed above
        assetAmountsMap.forEach(
                (token, qty) -> {
                    cliAmount.getTokenList().add(token + " : " + qty);
                    cliAmount.getTokens().add(new Token(token, qty));
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
                    cliAmount.getTokenList().add(token + ": " + qty);
                    cliAmount.getTokens().add(new Token(token, qty));
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

