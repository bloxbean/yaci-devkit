package com.bloxbean.cardano.yacicli.output;

import com.bloxbean.cardano.yacicli.commands.tail.model.CliBlock;
import com.bloxbean.cardano.yacicli.commands.tail.model.CliConnection;

public interface OutputFormatter {
    String formatConnection(CliConnection connection);
    String formatBlock(CliBlock block);
}
