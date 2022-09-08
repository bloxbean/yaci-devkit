package com.bloxbean.cardano.yacicli.rule;

import com.bloxbean.cardano.yaci.core.model.AuxData;
import com.bloxbean.cardano.yaci.core.model.Block;
import com.bloxbean.cardano.yaci.core.model.TransactionBody;
import com.bloxbean.cardano.yaci.core.model.TransactionOutput;
import com.bloxbean.cardano.yacicli.exception.CLIException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.api.RulesEngine;
import org.jeasy.rules.core.DefaultRulesEngine;
import org.jeasy.rules.mvel.MVELRuleFactory;
import org.jeasy.rules.support.reader.YamlRuleDefinitionReader;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Slf4j
public class RuleService {
    private static final String BLOCK_RULES_FILE = "rules/block_rules.yml";
    private static final String TX_RULES_FILE = "rules/tx_rules.yml";
    private static final String TX_OUT_RULES_FILE = "rules/txout_rules.yml";
    private static final String METADATA_RULES_FILE = "rules/metadata_rules.yml";
    private final ObjectMapper mapper = new ObjectMapper();

    private Rules blockRules;
    private Rules txRules;
    private Rules txOutputRules;
    private Rules metadataRules;

    public RuleService() throws CLIException {
        if (Files.exists(Path.of(BLOCK_RULES_FILE))) {
            log.info("Loading block rules !!!");
            blockRules = loadRules(BLOCK_RULES_FILE);
        }

        if (Files.exists(Path.of(TX_RULES_FILE))) {
            log.info("Loading tx rules !!!");
            txRules = loadRules(TX_RULES_FILE);
        }

        if (Files.exists(Path.of(TX_OUT_RULES_FILE))) {
            log.info("Loading txout rules !!!");
            txOutputRules = loadRules(TX_OUT_RULES_FILE);
        }

        if (Files.exists(Path.of(METADATA_RULES_FILE))) {
            log.info("Loading metadata rules !!!");
            metadataRules = loadRules(METADATA_RULES_FILE);
        }
    }

    public Rules loadRules(String ruleYmlFile) throws CLIException {
        MVELRuleFactory ruleFactory = new MVELRuleFactory(new YamlRuleDefinitionReader());
        try {
            return ruleFactory.createRules(new FileReader(ruleYmlFile));
        } catch (Exception e) {
            throw new CLIException("Rule creation failed : " + ruleYmlFile, e);
        }
    }

    public void fireBlockLevelRules(Block block, Result result) {
        if (blockRules == null) return;
        Facts facts = new Facts();
        facts.put("block", block.getHeader().getHeaderBody().getBlockNumber());
        facts.put("block", block);
        facts.put("result", result);

        RulesEngine rulesEngine = new DefaultRulesEngine();
        rulesEngine.fire(blockRules, facts);
    }

    public void fireTxLevelRule(TransactionBody txBody, Result result) {
        if (txRules == null) return;
        Facts facts = new Facts();
        facts.put("txHash", txBody.getTxHash());
        facts.put("txBody", txBody);
        facts.put("result", result);

        RulesEngine rulesEngine = new DefaultRulesEngine();
        rulesEngine.fire(txRules, facts);
    }

    public void fireTxOutputRules(String txHash, TransactionOutput txOuput, Result result) {
        if (txOutputRules == null) return;
        Facts facts = new Facts();
        facts.put("txHash", txHash);
        facts.put("txOut", txOuput);
        facts.put("result", result);

        RulesEngine rulesEngine = new DefaultRulesEngine();
        rulesEngine.fire(txOutputRules, facts);
    }

    public void fireMetadataRules(String txHash, AuxData auxData, Result result) {
        if (metadataRules == null) return;
        Facts facts = new Facts();
        try {
            if (auxData.getMetadataJson() == null || auxData.getMetadataJson().isEmpty())
                return;

            JsonNode jsonNode = mapper.readTree(auxData.getMetadataJson());

            facts.put("txHash", txHash);
            facts.put("metadata", jsonNode);
            facts.put("result", result);

            RulesEngine rulesEngine = new DefaultRulesEngine();
            rulesEngine.fire(metadataRules, facts);
        } catch (JsonProcessingException e) {
            log.error("Metadata cannot be parsed");
        }
    }

    public void executeRules(Block block, Result ruleResult) {
        //Fire block rules
        fireBlockLevelRules(block, ruleResult);

        for (int i=0; i < block.getTransactionBodies().size(); i++) {
            TransactionBody txBody = block.getTransactionBodies().get(i);
            //Fire tx rules
            fireTxLevelRule(txBody, ruleResult);

            for (TransactionOutput txOutput: txBody.getOutputs()) {
                //Fire txoutput rules
                fireTxOutputRules(txBody.getTxHash(), txOutput, ruleResult);
            }

            AuxData auxData = block.getAuxiliaryDataMap().get(Integer.valueOf(i));
            if (auxData != null) {
                fireMetadataRules(txBody.getTxHash(), auxData, ruleResult);
            }
        }
    }
}
