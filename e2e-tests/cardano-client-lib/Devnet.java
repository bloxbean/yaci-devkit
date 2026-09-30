import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.backend.api.BackendService;
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import com.bloxbean.cardano.client.common.model.Networks;

// Shared settings for the cardano-client-lib e2e tests
public class Devnet {
    public static final String YACI_STORE_URL = System.getenv().getOrDefault("YACI_STORE_URL", "http://localhost:8080/api/v1");

    // Default Yaci DevKit account 0 (pre-funded on devnet creation)
    public static final String MNEMONIC = "test test test test test test test test test test test test test test test test test test test test test test test sauce";

    public static final String RECEIVER = "addr_test1qqm87edtdxc7vu2u34dpf9jzzny4qhk3wqezv6ejpx3vgrwt46dz4zq7vqll88fkaxrm4nac0m5cq50jytzlu0hax5xqwlraql";

    public static BackendService backendService() {
        String url = YACI_STORE_URL.endsWith("/") ? YACI_STORE_URL : YACI_STORE_URL + "/";
        return new BFBackendService(url, "Dummy Key");
    }

    public static Account account() {
        return Account.createFromMnemonic(Networks.testnet(), MNEMONIC);
    }

    public static void check(String step, Result<String> result) {
        if (!result.isSuccessful()) {
            System.out.println("FAIL " + step + ": " + result.getResponse());
            System.exit(1);
        }
        System.out.println("OK   " + step + ": " + result.getValue());
    }
}
