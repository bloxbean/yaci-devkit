package com.bloxbean.cardano.yacicli.cip30.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletAccountsResponse {
    private List<WalletAccountDto> accounts;
}
