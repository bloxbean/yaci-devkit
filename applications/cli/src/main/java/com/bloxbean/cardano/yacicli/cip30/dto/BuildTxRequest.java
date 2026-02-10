package com.bloxbean.cardano.yacicli.cip30.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildTxRequest {
    private String senderAddress;  // bech32 address (e.g., addr_test1...)
    private String receiverAddress;
    private List<TransferAmount> amounts;
}
