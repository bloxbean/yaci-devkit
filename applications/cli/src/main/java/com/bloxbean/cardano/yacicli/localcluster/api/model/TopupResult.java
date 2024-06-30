package com.bloxbean.cardano.yacicli.localcluster.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopupResult {
    private String address;
    private double adaAmount;
    private boolean status;
    private String message;
}
