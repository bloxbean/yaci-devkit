package com.bloxbean.cardano.yacicli.cip30.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignDataRequest {
    @Pattern(regexp = "^[0-9a-fA-F]*$", message = "address must be valid hexadecimal if provided")
    private String address;

    @NotBlank(message = "payload is required")
    @Pattern(regexp = "^[0-9a-fA-F]+$", message = "payload must be valid hexadecimal")
    private String payload;

    @NotBlank(message = "accountId is required")
    private String accountId;
}
