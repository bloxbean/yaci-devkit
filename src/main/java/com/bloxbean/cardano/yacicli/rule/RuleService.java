package com.bloxbean.cardano.yacicli.rule;

import com.bloxbean.cardano.yaci.core.model.TransactionOutput;
import com.bloxbean.cardano.yacicli.exception.CLIException;
import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rule;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.api.RulesEngine;
import org.jeasy.rules.core.DefaultRulesEngine;
import org.jeasy.rules.mvel.MVELRuleFactory;
import org.jeasy.rules.support.reader.YamlRuleDefinitionReader;
import org.springframework.stereotype.Component;

import java.io.FileReader;

@Component
public class RuleService {

    private Rule rule;

    public void loadRule(String filePath) throws CLIException {
        MVELRuleFactory ruleFactory = new MVELRuleFactory(new YamlRuleDefinitionReader());
        try {
            rule = ruleFactory.createRule(new FileReader(filePath));
        } catch (Exception e) {
            throw new CLIException("Rule creation failed", e);
        }
    }

    public void fireRule(TransactionOutput txOuput, Result result) {
        Facts facts = new Facts();
        facts.put("txout", txOuput);
        facts.put("result", result);

        Rules rules = new Rules();
        rules.register(new AmountRule());

        RulesEngine rulesEngine = new DefaultRulesEngine();
        rulesEngine.fire(rules, facts);
    }
}
