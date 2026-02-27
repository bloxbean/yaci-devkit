package com.bloxbean.cardano.yacicli.cip30.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletAccountDto {
    private String id;
    private String name;
    private String address;
    private String stakeAddress;
    private boolean isDefault;
}
