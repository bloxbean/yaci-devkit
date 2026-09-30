# PyCardano Compatibility Tests

Uses Yaci Store as the Blockfrost backend (`BlockFrostChainContext`).

```shell
python3 -m venv .venv && .venv/bin/pip install -r requirements.txt
.venv/bin/python payment.py      # simple ADA payment
.venv/bin/python plutus_v3.py    # lock at an always-succeeds PlutusV3 script and spend it back
```
