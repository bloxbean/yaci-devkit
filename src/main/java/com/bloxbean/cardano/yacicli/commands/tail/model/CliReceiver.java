package com.bloxbean.cardano.yacicli.commands.tail.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CliReceiver {

    String receiver;
    CliAmount receiverAmount;
}
