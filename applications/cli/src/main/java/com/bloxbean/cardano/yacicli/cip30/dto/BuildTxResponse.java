package com.bloxbean.cardano.yacicli.cip30.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildTxResponse {
    private boolean success;
    private String txCbor;
    private String txHash;
    private String errorCode;
    private String message;

    public static BuildTxResponse success(String txCbor, String txHash) {
        return BuildTxResponse.builder()
                .success(true)
                .txCbor(txCbor)
                .txHash(txHash)
                .build();
    }

    public static BuildTxResponse error(String errorCode, String message) {
        return BuildTxResponse.builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .build();
    }
}
