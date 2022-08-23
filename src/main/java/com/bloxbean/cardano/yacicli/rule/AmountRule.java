package com.bloxbean.cardano.yacicli.rule;

import com.bloxbean.cardano.yaci.core.model.TransactionOutput;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;

@Rule(name = "Hosky rule", description = "Hosky token")
public class AmountRule {

    @Condition
    public boolean when(@Fact("txout")TransactionOutput txOutput) {
        return txOutput.getAmounts().stream().filter(amount -> amount.getAssetName().startsWith("Naru"))
                .findAny().isPresent();
    }

    @Action
    public void then(@Fact("result") Result result) {
        System.out.println("then");
        result.setSelected(true);
    }
}
