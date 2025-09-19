package com.bloxbean.cardano.yacicli.cip30.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkResponse {
    private Integer networkId; // 0 = testnet, 1 = mainnet
    private String networkName;
    private Integer currentEpoch;
    private Integer currentSlot;
}