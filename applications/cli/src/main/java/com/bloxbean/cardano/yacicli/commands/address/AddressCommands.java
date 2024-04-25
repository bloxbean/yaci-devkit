package com.bloxbean.cardano.yacicli.commands.address;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.common.model.Networks;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.strLn;

@Component
@Slf4j
public class AddressCommands {

    public void generateNew(boolean isMainnet) {
        Account account = isMainnet ? new Account(Networks.mainnet()): new Account(Networks.testnet());

        StringBuilder sb = new StringBuilder();
        sb.append(strLn("Address   : %s", account.baseAddress()));
        sb.append(strLn("Mnemonics : %s", account.mnemonic()));

        System.out.println(sb.toString());
    }
}
