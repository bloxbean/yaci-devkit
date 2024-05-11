package com.bloxbean.cardano.yacicli.commands.tail.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CliAmount {

    double totalAda;
    List<String> tokenList = new ArrayList<>();
    List<Token> tokens = new ArrayList<>();

}
