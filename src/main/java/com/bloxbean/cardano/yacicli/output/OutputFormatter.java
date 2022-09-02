package com.bloxbean.cardano.yacicli.output;

import com.bloxbean.cardano.yacicli.commands.tail.model.CliBlock;
import com.bloxbean.cardano.yacicli.commands.tail.model.CliConnection;

public interface OutputFormatter {

    public String formatConnection(CliConnection connection);

    public String formatBlock(CliBlock block);

}
