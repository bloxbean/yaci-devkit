package com.bloxbean.cardano.yacicli.commands.tail.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CliConnection {

    public static final String HEADER = "CONNECTION INFO";

    String host;
    int port;
    long protocolMagic;

}
