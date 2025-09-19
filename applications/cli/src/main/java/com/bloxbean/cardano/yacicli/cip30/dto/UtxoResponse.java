package com.bloxbean.cardano.yacicli.cip30.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UtxoResponse {
    private String txHash;
    private Integer outputIndex;
    private String address;
    private String amount; // Lovelace amount
    private List<Map<String, Object>> multiAssets; // Other assets
    private String datum;
    private String datumHash;
    private String inlineDatum;
    private String referenceScriptHash;
    private Long blockNumber;
    private Long slot;
}