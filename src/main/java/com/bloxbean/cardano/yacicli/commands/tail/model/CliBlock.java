package com.bloxbean.cardano.yacicli.commands.tail.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CliBlock {

    public static final String HEADER = "BLOCK INFO";

    boolean showGrouping = false;
    boolean showInput = false;
    boolean showOutput = false;
    boolean showMint = false;

    long blockNumber;

    CliAmount groupingAmount;

    List<String> inputs = new ArrayList<>();

    List<CliReceiver> receivers = new ArrayList<>();

    CliAmount mintTokens;

}
