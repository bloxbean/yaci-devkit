package com.bloxbean.cardano.yacicli.localcluster.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClusterCreated {
    private String clusterName;
}
