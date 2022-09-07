package com.bloxbean.cardano.yacicli.commands.tail.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Token {
    private String name;
    private BigInteger amount;
}
