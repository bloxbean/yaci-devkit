# Simple ADA payment with PyCardano using Yaci Store as the Blockfrost backend
from pycardano import Address, TransactionBuilder, TransactionOutput

from devnet import RECEIVER, chain_context, wait_for_tx, wallet

context = chain_context()
payment_skey, address = wallet()
print("Sender:", address)

builder = TransactionBuilder(context)
builder.add_input_address(address)
builder.add_output(TransactionOutput(Address.from_primitive(RECEIVER), 5_000_000))
signed_tx = builder.build_and_sign([payment_skey], change_address=address)

tx_hash = str(context.submit_tx(signed_tx))
print("Submitted:", tx_hash)
wait_for_tx(context, tx_hash)
print("Confirmed:", tx_hash)
