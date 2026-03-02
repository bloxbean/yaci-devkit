package com.bloxbean.cardano.yacicli.cip30.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssembleTxResponse {
    private boolean success;
    private String signedTxCbor;
    private String errorCode;
    private String message;

    public static AssembleTxResponse success(String signedTxCbor) {
        return AssembleTxResponse.builder()
                .success(true)
                .signedTxCbor(signedTxCbor)
                .build();
    }

    public static AssembleTxResponse error(String errorCode, String message) {
        return AssembleTxResponse.builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .build();
    }
}
