package com.bloxbean.cardano.yacicli.commands.localcluster.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopupRequest {
    private String address;
    private double adaAmount;
}
