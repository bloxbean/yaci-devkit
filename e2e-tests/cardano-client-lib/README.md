# cardano-client-lib Compatibility Tests

[JBang](https://www.jbang.dev) scripts using `BFBackendService` pointed at Yaci Store. The cardano-client-lib
version defaults to the one in the `//DEPS` lines and can be overridden with `-Dccl.version`.

```shell
jbang Payment.java                              # simple ADA payment
jbang MintToken.java                            # mint a native token and query it by asset
jbang PlutusV3.java                             # lock at an always-succeeds PlutusV3 script and spend it back
jbang -Dccl.version=0.8.0-preview1 Payment.java # another cardano-client-lib version
```
