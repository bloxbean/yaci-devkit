package com.bloxbean.cardano.yacicli.cip30.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/wallet-demo")
@ConditionalOnProperty(prefix = "yaci.devkit.wallet", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WalletDemoController {
    
    @GetMapping("/")
    public String demoIndex() {
        log.info("Serving wallet demo page");
        return "redirect:/wallet-demo/index.html";
    }
}