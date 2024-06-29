package com.bloxbean.cardano.yacicli.commands.localcluster.service.model;

import com.bloxbean.cardano.client.crypto.cip1852.DerivationPath;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DefaultAddress {
    private DerivationPath derivationPath;
    private String address;
    private String addressHex;
    private String stakeAddress;
    private String paymentKey;
    private String stakingKey;
    private String defaultUtxoId;
    private BigDecimal defaultUtxoAmount;
}
