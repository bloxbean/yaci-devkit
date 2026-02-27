package com.bloxbean.cardano.yacicli.cip30.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {
    @NotBlank(message = "accountId is required")
    private String accountId;           // "0"-"19" or "custom-N"

    @NotBlank(message = "receiverAddress is required")
    @Pattern(regexp = "^addr[_a-z0-9]+$", message = "receiverAddress must be a valid Cardano address")
    private String receiverAddress;     // Bech32 address

    @NotEmpty(message = "amounts list cannot be empty")
    @Valid
    private List<TransferAmount> amounts;
}
