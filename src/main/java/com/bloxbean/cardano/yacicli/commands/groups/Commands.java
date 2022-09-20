package com.bloxbean.cardano.yacicli.commands.groups;

import com.bloxbean.cardano.yacicli.commands.address.AddressCommands;
import com.bloxbean.cardano.yacicli.commands.common.RootLogService;
import com.bloxbean.cardano.yacicli.commands.tail.BlockStreamerService;
import com.bloxbean.cardano.yacicli.commands.tail.themes.ColorModeFactory;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import com.bloxbean.cardano.yacicli.common.ShellHelper;
import com.bloxbean.cardano.yacicli.output.BoxOutputFormatter;
import com.bloxbean.cardano.yacicli.output.DefaultOutputFormatter;
import com.bloxbean.cardano.yacicli.output.OutputFormatter;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.*;

@ShellComponent
@ShellCommandGroup(Groups.GENERAL_CMD_GROUP)
public class Commands {
    private final BlockStreamerService blockStreamerService;
    private final AddressCommands addressCommands;
    private final RootLogService rootLogService;
    private ShellHelper shellHelper;
    private OutputFormatter outputFormatter;

    public Commands(BlockStreamerService blockStreamerService, AddressCommands addressCommands, RootLogService rootLogService, ShellHelper shellHelper) {
        this.blockStreamerService = blockStreamerService;
        this.addressCommands = addressCommands;
        this.rootLogService = rootLogService;
        this.shellHelper = shellHelper;
        this.outputFormatter = new DefaultOutputFormatter(shellHelper);
    }

    @ShellMethod(value = "Stream recent blocks from Cardano node", key = "tail")
    @ShellMethodAvailability("generalCmdAvailability")
    public void tail(
            @ShellOption(help = "Cardano Node host", defaultValue = ShellOption.NULL) String host,
            @ShellOption(help = "Cardano Node Port", defaultValue = "0") int port,
            @ShellOption(defaultValue = ShellOption.NULL, help = "Provide a known network (mainnet, legacy_testnet, prepod, preview)") String network,
            @ShellOption(value = {"--protocol-magic"}, help = "Protocol Magic", defaultValue = "0") long protocolMagic,
            @ShellOption(value = {"--known-slot"}, help = "Well known slot", defaultValue = "0") long slot,
            @ShellOption(value = {"--known-blockhash"}, help = "Well known block hash", defaultValue = ShellOption.NULL) String blockHash,
            @ShellOption(value = {"-c", "--show-mint"}, defaultValue = "true", help = "Show mint outputs") boolean showMint,
            @ShellOption(value = {"-i", "--show-inputs"}, defaultValue = "false", help = "Show inputs") boolean showInputs,
//                     @ShellOption(value = {"-o", "--show-outputs"}, defaultValue = "true", help = "Show outputs") boolean showOutputs,
            @ShellOption(value = {"-m", "--show-metadata"}, defaultValue = "true", help = "Show Metadata") boolean showMetadata,
            @ShellOption(value = {"-d", "--show-datumhash"}, defaultValue = "true", help = "Show DatumHash") boolean showDatumhash,
            @ShellOption(value = {"-l", "--show-inlinedatum"}, defaultValue = "true", help = "Show InlineDatum") boolean showInlineDatum,
            @ShellOption(value = {"--grouping"}, defaultValue = "true", help = "Enable/Disable grouping") boolean grouping,
            @ShellOption(value = {"--log-level"}, defaultValue = ShellOption.NULL, help = "Log level") String logLevel,
//                     @ShellOption(value = {"--output-format"}, defaultValue = "Default", help = "Output Format (Default, Box") String outputFormat,
            @ShellOption(value = {"--color-mode"}, defaultValue = "dark", help = "Color mode (dark, light") String colorMode
    ) throws InterruptedException {
        String output = shellHelper.getSuccessMessage("Starting tail ...");
        ColorModeFactory.setColorMode(colorMode);

        String outputFormat = "default";
        if (outputFormat.equals("Box")) {
            outputFormatter = new BoxOutputFormatter();
        } else {
            outputFormatter = new DefaultOutputFormatter(shellHelper);
        }

        blockStreamerService.tail(host, port, network, protocolMagic, slot, blockHash, showMint, showInputs, showMetadata, showDatumhash, showInlineDatum, grouping, outputFormatter);
    }

    @ShellMethod(value = "Set log level", key = "logging")
    public void logLevel(@ShellOption(value = {"--set-level"}, defaultValue = ShellOption.NULL, help = "Log level") String logLevel) {
        rootLogService.setLogLevel(logLevel);
    }

    @ShellMethod(value = "Generate Address", key = "gen-address")
    public void generateAddress(@ShellOption(defaultValue = "true", help = "true for mainnet, false for testnet") boolean mainnet) {
        addressCommands.generateNew(mainnet);
    }

    public Availability generalCmdAvailability() {
        return CommandContext.INSTANCE.getCurrentMode() == CommandContext.Mode.REGULAR
                ? Availability.available()
                : Availability.unavailable("you are not in general command mode");
    }
}
