package com.bloxbean.cardano.yacicli.commands.tail.model;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CliConnection {

    public static final String HEADER = "CONNECTION INFO";

    private String host;
    private int port;
    private long protocolMagic;
    private Point wellKnownPoint;

}
