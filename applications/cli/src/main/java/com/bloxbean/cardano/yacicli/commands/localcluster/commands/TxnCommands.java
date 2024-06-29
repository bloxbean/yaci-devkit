package com.bloxbean.cardano.yacicli.commands.localcluster.commands;

import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.plutus.spec.PlutusData;
import com.bloxbean.cardano.client.plutus.spec.serializers.PlutusDataJsonConverter;
import com.bloxbean.cardano.yaci.core.protocol.localstate.api.Era;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yacicli.commands.common.Groups;
import com.bloxbean.cardano.yacicli.commands.localcluster.service.AccountService;
import com.bloxbean.cardano.yacicli.commands.localcluster.service.ClusterUtilService;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.*;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.bloxbean.cardano.yacicli.commands.localcluster.ClusterConfig.CLUSTER_NAME;
import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

@ShellComponent
@ShellCommandGroup(Groups.TXN_CMD_GROUP)
@RequiredArgsConstructor
@Slf4j
public class TxnCommands {
    private final AccountService accountService;
    private final ClusterUtilService clusterUtilService;

    @ShellMethod(value = "Topup account", key = "topup")
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void mintToken(@ShellOption(value = {"-a", "--address"}, help = "Receiver address") String address,
                          @ShellOption(value = {"-v", "--value"}, help = "Ada value") double adaValue) {
        String clusterName = CommandContext.INSTANCE.getProperty(CLUSTER_NAME);
        Era era = CommandContext.INSTANCE.getEra();

        boolean topupStatus = accountService.topup(clusterName, era, address, adaValue, msg -> writeLn(msg));
        if (!topupStatus)
            return;

        boolean status = clusterUtilService.waitForNextBlocks(1, msg -> writeLn(msg));

        if (status) {
            writeLn(info("Available utxos") + "\n");
            getUtxos(address, false);
        }
    }

    @ShellMethod(value = "Get utxos at an address", key = "utxos")
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void getUtxos(@ShellOption(value = {"-a", "--address"}, help = "Address") String address,
                         @ShellOption(value = {"--pretty-print-inline-datum"}, defaultValue = "false") boolean prettyPrintInlineDatum) {
        String clusterName = CommandContext.INSTANCE.getProperty(CLUSTER_NAME);
        Era era = CommandContext.INSTANCE.getEra();

        List<Utxo> utxos = accountService.getUtxos(clusterName, era, address, msg -> writeLn(msg));

        AtomicInteger index = new AtomicInteger(0);
        utxos.forEach(utxo -> {
            writeLn(index.incrementAndGet() + ". " + utxo.getTxHash() + "#" + utxo.getOutputIndex() + " : " + utxo.getAmount());
            if (utxo.getInlineDatum() != null) {
                if (prettyPrintInlineDatum) {
                    try {
                        writeLn("InlineDatum : \n" +
                                PlutusDataJsonConverter.toJson(PlutusData.deserialize(HexUtil.decodeHexString(utxo.getInlineDatum()))));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else
                    writeLn("InlineDatum: " + utxo.getInlineDatum());
            }
            if (utxo.getDataHash() != null)
                writeLn("DatumHash: " + utxo.getDataHash());

            if (utxo.getReferenceScriptHash() != null)
                writeLn("ReferenceScriptHash: " + utxo.getReferenceScriptHash());
            writeLn("--------------------------------------------------------------------------------------");
        });
    }

    @ShellMethod(value = "Mint tokens using faucet account with default policy", key = "mint")
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void mintToken(@ShellOption(value = {"-n", "--asset-name"}, help = "Asset Name") String assetName,
                          @ShellOption(value = {"-q", "--quantity"}, help = "Quantity") BigInteger quantity,
                          @ShellOption(value = {"-r", "--receiver"}, help = "Reciever Address") String receiver
    ) {
        String clusterName = CommandContext.INSTANCE.getProperty(CLUSTER_NAME);
        Era era = CommandContext.INSTANCE.getEra();

        boolean mintStatus = accountService.mint(clusterName, era, assetName, quantity, receiver, msg -> writeLn(msg));
        if (!mintStatus)
            return;

        writeLn(info("Available utxos") + "\n");
        getUtxos(receiver, false);
    }

    public Availability localClusterCmdAvailability() {
        return CommandContext.INSTANCE.getCurrentMode() == CommandContext.Mode.LOCAL_CLUSTER
                ? Availability.available()
                : Availability.unavailable("you are not in local-cluster modes");
    }
}
