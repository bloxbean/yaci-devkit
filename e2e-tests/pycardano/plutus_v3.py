# Lock ADA at an always-succeeds PlutusV3 script and spend it back, using Yaci Store as the
# Blockfrost backend (UTxO queries, script evaluation and submission)
from dataclasses import dataclass

from pycardano import (Address, PlutusData, PlutusV3Script, Redeemer, TransactionBuilder, TransactionOutput,
                       plutus_script_hash)

from devnet import chain_context, wait_for_tx, wallet

# Same always-succeeds validator as lucid-evo/plutus_v3.ts (CBOR-wrapped compiled code)
COMPILED_CODE = "5857010000323232323225333002323232323253330073370e900118041baa00113232324a26018601a004601600260126ea800458c024c028008c020004c020008c018004c010dd50008a4c26cacae6955ceaab9e5742ae89"

@dataclass
class OwnerDatum(PlutusData):
    CONSTR_ID = 0
    owner: bytes


@dataclass
class MessageRedeemer(PlutusData):
    CONSTR_ID = 0
    message: bytes


context = chain_context()
payment_skey, address = wallet()
owner = address.payment_part.payload

script = PlutusV3Script(bytes.fromhex(COMPILED_CODE))
script_address = Address(plutus_script_hash(script), network=address.network)
print("Script address:", script_address)

# Lock
builder = TransactionBuilder(context)
builder.add_input_address(address)
builder.add_output(TransactionOutput(script_address, 10_000_000, datum=OwnerDatum(owner)))
lock_tx = builder.build_and_sign([payment_skey], change_address=address)
lock_hash = str(context.submit_tx(lock_tx))
print("Lock submitted:", lock_hash)
wait_for_tx(context, lock_hash)

# Spend the UTxO created by the lock tx
script_utxo = next(u for u in context.utxos(script_address) if str(u.input.transaction_id) == lock_hash)

builder = TransactionBuilder(context)
builder.add_input_address(address)
builder.add_script_input(script_utxo, script=script, redeemer=Redeemer(MessageRedeemer(b"Hello, World!")))
# PyCardano's default ttl (current slot + 10000) is beyond a young devnet's forecast horizon (PastHorizon)
builder.validity_start = context.last_block_slot
builder.ttl = context.last_block_slot + 50
builder.add_output(TransactionOutput(address, 5_000_000))
spend_tx = builder.build_and_sign([payment_skey], change_address=address)
spend_hash = str(context.submit_tx(spend_tx))
print("Spend submitted:", spend_hash)
wait_for_tx(context, spend_hash)
print("Confirmed:", spend_hash)
