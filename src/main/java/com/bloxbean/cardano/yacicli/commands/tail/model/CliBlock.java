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

    private boolean showGrouping = false;
    private boolean showInput = false;
    private boolean showMint = false;
    private boolean showMetadata = false;
    private boolean showInlineDatums = false;
    private boolean showDatumhases = false;

    private long blockNumber;
    private int totalTxs;
    private long totalFees;
    private long blockSize;

    private CliAmount groupingAmount;
    private List<String> inputs = new ArrayList<>();
    private List<CliReceiver> receivers = new ArrayList<>();
    private CliAmount mintTokens;

    //Optional group fields
    private List<String> metadataList;
    private long noPlutusV1Scripts;
    private long noPlutusV2Scripts;
    private List<String> inlineDatums;
    private List<String> datumHashes;

    public void clear() {
        blockNumber = 0;
        totalTxs = 0;
        totalFees = 0;
        blockSize = 0;

        groupingAmount = null;

        inputs = new ArrayList<>();

        receivers = new ArrayList<>();

        mintTokens = null;

        //Optional group fields
        metadataList = null;
        noPlutusV1Scripts = 0;
        noPlutusV2Scripts = 0;
        inlineDatums = null;
        datumHashes = null;
    }
}
