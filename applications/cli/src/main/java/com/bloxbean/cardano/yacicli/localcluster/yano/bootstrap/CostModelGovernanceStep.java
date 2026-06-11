package com.bloxbean.cardano.yacicli.localcluster.yano.bootstrap;

import com.bloxbean.cardano.yacicli.localcluster.yano.YanoGovernanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * Built-in bootstrap step: submit the PV11 Plutus cost-model governance action
 * (register stake+DRep, ParameterChangeAction, vote YES) so the full cost models are
 * enacted within the shifted past epochs before catch-up to wall-clock.
 */
@Component
@RequiredArgsConstructor
public class CostModelGovernanceStep implements ChainBootstrapStep {

    private final YanoGovernanceService yanoGovernanceService;

    @Override
    public String name() {
        return "cost-model-governance";
    }

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    public boolean apply(ChainBootstrapContext context, Consumer<String> writer) {
        return yanoGovernanceService.submitCostModelGovernance(
                context.clusterInfo(), context.clusterFolder(), writer);
    }
}
