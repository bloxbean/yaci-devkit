# End-to-End Tests

This folder contains end-to-end tests for Yaci DevKit. The SDK compatibility tests build, sign and submit
transactions through Yaci Store's Blockfrost-compatible API using each SDK.

## Test Categories

1. **api-tests**: This folder contains HTTP API tests, primarily for admin endpoints and essential endpoints from Yaci Store.
2. **lucid-evo**: Compatibility tests for Lucid Evolution.
3. **meshjs**: Compatibility tests for MeshJs
4. **evolution-sdk**: Compatibility tests for Evolution SDK
5. **pycardano**: Compatibility tests for PyCardano
6. **cardano-client-lib**: Compatibility tests for cardano-client-lib (JBang scripts)

## Run all SDK tests

Requires `bun`, `python3`, `jbang`, Java 21 and `timeout` (GNU coreutils), plus a built CLI jar
(`cd applications/cli && ./gradlew clean build -x test`).

```shell
# Against a running DevKit (Yaci Store on 8080, admin API on 10000)
./run-sdk-tests.sh

# Update every SDK to its latest version, recreate the devnet and run
./run-sdk-tests.sh --update --restart

# Test a Yaci Store build (runs Store in java mode from this jar; recreates the devnet)
./run-sdk-tests.sh --store-jar /path/to/yaci-store-<version>.jar

# A subset, or a specific cardano-client-lib version
./run-sdk-tests.sh --only pycardano,cardano-client-lib --ccl-version 0.8.0-pre5
```

`--restart` and `--store-jar` stop the running DevKit and recreate the devnet (`create-node -o`), which wipes
its data. They match processes by command line, so they also stop any other `cardano-node` or
`cardano-submit-api` on the machine (e.g. a preprod or mainnet node); without those flags nothing is stopped.
Per-test logs are written to `.logs/`.
