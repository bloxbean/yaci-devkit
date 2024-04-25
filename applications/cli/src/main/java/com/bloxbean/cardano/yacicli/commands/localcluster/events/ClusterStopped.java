package com.bloxbean.cardano.yacicli.commands.localcluster.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClusterStopped {
    private String clusterName;
}
