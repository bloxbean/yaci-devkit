import os
import time

from blockfrost import ApiError
from pycardano import (Address, BlockFrostChainContext, ExtendedSigningKey, HDWallet, Network)

YACI_STORE_URL = os.environ.get("YACI_STORE_URL", "http://localhost:8080/api/v1")

# Default Yaci DevKit account 0 (pre-funded on devnet creation)
MNEMONIC = "test test test test test test test test test test test test test test test test test test test test test test test sauce"

RECEIVER = "addr_test1qqm87edtdxc7vu2u34dpf9jzzny4qhk3wqezv6ejpx3vgrwt46dz4zq7vqll88fkaxrm4nac0m5cq50jytzlu0hax5xqwlraql"


def chain_context() -> BlockFrostChainContext:
    # blockfrost-python appends the API version (default "v0") to the base url, so split
    # "http://localhost:8080/api/v1" into base url "http://localhost:8080/api" and version "v1"
    base_url, api_version = YACI_STORE_URL.rstrip("/").rsplit("/", 1)
    os.environ["BLOCKFROST_API_VERSION"] = api_version
    return BlockFrostChainContext(project_id="dummy", base_url=base_url)


def wallet():
    hdwallet = HDWallet.from_mnemonic(MNEMONIC)
    payment_skey = ExtendedSigningKey.from_hdwallet(hdwallet.derive_from_path("m/1852'/1815'/0'/0/0"))
    stake_skey = ExtendedSigningKey.from_hdwallet(hdwallet.derive_from_path("m/1852'/1815'/0'/2/0"))
    address = Address(payment_skey.to_verification_key().hash(), stake_skey.to_verification_key().hash(),
                      network=Network.TESTNET)
    return payment_skey, address


def wait_for_tx(context: BlockFrostChainContext, tx_hash: str, timeout: int = 60) -> None:
    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            context.api.transaction(tx_hash)
            return
        except ApiError:
            time.sleep(1)
    raise TimeoutError(f"Transaction {tx_hash} was not confirmed within {timeout}s")
