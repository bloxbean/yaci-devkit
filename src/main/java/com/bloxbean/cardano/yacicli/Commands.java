package com.bloxbean.cardano.yacicli;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.bloxbean.cardano.yacicli.commands.tail.BlockStreamerService;
import com.bloxbean.cardano.yacicli.common.ShellHelper;
import com.bloxbean.cardano.yacicli.output.BoxOutputFormatter;
import com.bloxbean.cardano.yacicli.output.DefaultOutputFormatter;
import com.bloxbean.cardano.yacicli.output.OutputFormatter;
import org.slf4j.LoggerFactory;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.util.StringUtils;

@ShellComponent(value = "Commands")
public class Commands {
    private Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

    private BlockStreamerService blockStreamerService;
    private ShellHelper shellHelper;
    private OutputFormatter outputFormatter;
    
    public Commands(BlockStreamerService blockStreamerService, ShellHelper shellHelper) {
        this.blockStreamerService = blockStreamerService;
        this.shellHelper = shellHelper;
        this.outputFormatter = new DefaultOutputFormatter(shellHelper);
    }

    @ShellMethod(value = "Stream recent blocks from Cardano node", key = "tail")
    public void tail(
                     @ShellOption(help = "Cardano Node host",defaultValue = ShellOption.NULL) String host,
                     @ShellOption(help = "Cardano Node Port", defaultValue = "0") int port,
                     @ShellOption(value = {"--protocol-magic"}, help = "Protocol Magic", defaultValue = "0") long protocolMagic,
                     @ShellOption(value = {"--known-slot"}, help="Well known slot", defaultValue = "0") long slot,
                     @ShellOption(value = {"--known-blockhash"}, help="Well known block hash", defaultValue = ShellOption.NULL) String blockHash,
                     @ShellOption(value = {"-m", "--show-mint" }, defaultValue = "true", help = "Show mint outputs") boolean showMint,
                     @ShellOption(value = {"-i", "--show-inputs"}, defaultValue = "true", help = "Show inputs") boolean showInputs,
                     @ShellOption(value = {"-o", "--show-outputs"}, defaultValue = "true", help = "Show outputs") boolean showOutputs,
                     @ShellOption(value = {"--grouping"}, defaultValue = "false", help="Enable grouping") boolean grouping,
                     @ShellOption(value = {"--log-level"}, defaultValue = ShellOption.NULL, help = "Log level") String logLevel,
                     @ShellOption(value = {"--output-format"}, defaultValue = "Default", help = "Output Format (Default, Box") String outputFormat
    ) throws InterruptedException {
        String output = shellHelper.getSuccessMessage("Starting tail ...");
        if ( outputFormat.equals("Box")){
            outputFormatter = new BoxOutputFormatter();
        } else {
            outputFormatter = new DefaultOutputFormatter(shellHelper);
        }
        blockStreamerService.tail(host, port, protocolMagic, slot, blockHash, showMint, showInputs, showOutputs, grouping, outputFormatter);
    }

    @ShellMethod(value = "Set log level", key = "logging")
    public void logLevel(@ShellOption(value = {"--set-level"}, defaultValue = ShellOption.NULL, help = "Log level") String logLevel) {
        setLogLevel(logLevel);
    }

    private void setLogLevel(String logLevel) {
        if (StringUtils.hasLength(logLevel)) {
            switch (logLevel) {
                case "info":
                    root.setLevel(Level.INFO);
                    break;
                case "debug":
                    root.setLevel(Level.DEBUG);
                    break;
                case "trace":
                    root.setLevel(Level.TRACE);
                    break;
                case "error":
                    root.setLevel(Level.ERROR);
                    break;
                default:
                    break;
            }
        }
    }
}
