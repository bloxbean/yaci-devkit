package com.bloxbean.cardano.yacicli.commands.localcluster.service;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.crypto.Bech32;
import com.bloxbean.cardano.client.crypto.Blake2bUtil;
import com.bloxbean.cardano.client.crypto.cip1852.DerivationPath;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yacicli.commands.localcluster.service.model.DefaultAddress;
import com.bloxbean.cardano.yacicli.common.AnsiColors;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.header;
import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.writeLn;

@Component
public class DefaultAddressService {
    public static final String ED_25519_E_SK = "ed25519e_sk";
    private final String mnemonic = "test test test test test test test test test test test test test test test test test test test test test test test sauce";
    private final int noOfAccounts = 20;
    private final BigDecimal defaultAdaAmount = new BigDecimal(10000);

    private List<DefaultAddress> defaultAddresses = new ArrayList<>();

    public List<DefaultAddress> getDefaultAddresses() {
        if (defaultAddresses.size() > 0)
            return defaultAddresses;

        for (int i = 0; i < noOfAccounts; i++) {
            DerivationPath derivationPath = DerivationPath.createExternalAddressDerivationPathForAccount(i);
            var account = new Account(Networks.testnet(), mnemonic, derivationPath);

            var defaultAddress = new DefaultAddress();
            defaultAddress.setDerivationPath(derivationPath);
            defaultAddress.setAddress(account.baseAddress());
            defaultAddress.setAddressHex(HexUtil.encodeHexString(new Address(account.baseAddress()).getBytes()));
            defaultAddress.setStakeAddress(account.stakeAddress());
            defaultAddress.setPaymentKey(Bech32.encode(account.privateKeyBytes(), ED_25519_E_SK));
            defaultAddress.setStakingKey(Bech32.encode(account.stakeHdKeyPair().getPrivateKey().getKeyData(), ED_25519_E_SK));

            String txHash = HexUtil.encodeHexString(Blake2bUtil.blake2bHash256(new Address(account.baseAddress()).getBytes()));
            defaultAddress.setDefaultUtxoId(txHash + "#" + 0);
            defaultAddress.setDefaultUtxoAmount(defaultAdaAmount);
            defaultAddresses.add(defaultAddress);
        }

        return defaultAddresses;
    }

    public String getDefaultMnemonic() {
        return mnemonic;
    }

    public void printDefaultAddresses(boolean showDefaultUtxos) {
        writeLn("\n#################################");
        writeLn(header(AnsiColors.CYAN_BOLD, "Default Addresses"));
        writeLn("#################################\n");
        writeLn(header(AnsiColors.BLUE_BOLD, "Mnemonic: ") + getDefaultMnemonic());
        writeLn("");

        var defaultAddresses = getDefaultAddresses();

        for (int i = 0; i < defaultAddresses.size(); i++) {
            var defaultAddress = defaultAddresses.get(i);
            writeLn("%-30s : %s", header(AnsiColors.CYAN_BOLD, "Address" + " #" + i), defaultAddress.getAddress());
            writeLn("%-30s : %s", header(AnsiColors.CYAN_BOLD, "Derivation Path"), derivationPathToString(defaultAddress.getDerivationPath()));
            writeLn("%-30s : %s", header(AnsiColors.CYAN_BOLD, "Stake Address"), defaultAddress.getStakeAddress());
            writeLn("%-30s : %s", header(AnsiColors.CYAN_BOLD, "Payment Key"), defaultAddress.getPaymentKey());
            writeLn("%-30s : %s", header(AnsiColors.CYAN_BOLD, "Staking Key"), defaultAddress.getStakingKey());

            if (showDefaultUtxos) {
                writeLn("%-30s : %s - %s Ada", header(AnsiColors.CYAN_BOLD, "Utxo"), defaultAddress.getDefaultUtxoId(), defaultAddress.getDefaultUtxoAmount());
            }
            writeLn("");
        }
    }

    private String derivationPathToString(DerivationPath derivationPath) {
        var sb = new StringBuilder();
        sb.append("m")
                .append("/")
                .append(derivationPath.getPurpose().getValue() + "'")
                .append("/")
                .append(derivationPath.getCoinType().getValue() + "'")
                .append("/")
                .append(derivationPath.getAccount().getValue() + "'")
                .append("/")
                .append(derivationPath.getRole().getValue())
                .append("/")
                .append(derivationPath.getIndex().getValue());

        return sb.toString();

    }
}
