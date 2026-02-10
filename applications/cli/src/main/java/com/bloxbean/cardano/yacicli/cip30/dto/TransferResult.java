package com.bloxbean.cardano.yacicli.cip30.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResult {
    private boolean success;
    private String txHash;
    private String message;
    private String errorCode;

    public static TransferResult success(String txHash) {
        return TransferResult.builder()
                .success(true)
                .txHash(txHash)
                .message("Transaction submitted successfully")
                .build();
    }

    public static TransferResult error(String errorCode, String message) {
        return TransferResult.builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .build();
    }
}
