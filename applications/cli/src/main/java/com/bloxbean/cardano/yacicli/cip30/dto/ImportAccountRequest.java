package com.bloxbean.cardano.yacicli.cip30.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportAccountRequest {
    @NotBlank(message = "mnemonic is required")
    @Size(min = 20, message = "mnemonic must be at least 20 characters")
    private String mnemonic;

    @NotBlank(message = "name is required")
    @Size(min = 1, max = 50, message = "name must be between 1 and 50 characters")
    private String name;
}
