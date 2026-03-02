package com.bloxbean.cardano.yacicli.cip30.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignTxRequest {
    @NotBlank(message = "txCbor is required")
    @Pattern(regexp = "^[0-9a-fA-F]+$", message = "txCbor must be valid hexadecimal")
    private String txCbor;

    @NotBlank(message = "accountId is required")
    private String accountId;

    private boolean partialSign;
}
