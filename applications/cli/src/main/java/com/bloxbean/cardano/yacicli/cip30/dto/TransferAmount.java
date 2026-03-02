package com.bloxbean.cardano.yacicli.cip30.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferAmount {
    @NotBlank(message = "unit is required")
    private String unit;      // "lovelace" or "{policyId}{assetNameHex}"

    @NotBlank(message = "quantity is required")
    @Pattern(regexp = "^[0-9]+$", message = "quantity must be a positive number")
    private String quantity;  // Amount as string
}
