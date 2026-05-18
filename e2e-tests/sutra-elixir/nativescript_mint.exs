alias Sutra.Cardano.Address
alias Sutra.Cardano.Asset
alias Sutra.Crypto.Key
alias Sutra.Cardano.Script
alias Sutra.Cardano.Script.NativeScript


# Setup sutra package & init yaci provider
Code.eval_file("setup.exs")

mnemonic =
  "test test test test test test test test test test test test test test test test test test test test test test test sauce"

{:ok, root_key} = Key.root_key_from_mnemonic(mnemonic)
{:ok, extended_key} = Key.derive_child(root_key, 0, 0)

{:ok, wallet_address} = Key.address(extended_key, :preprod)

script_json = %{
  "type" => "all",
  "scripts" => [
    %{
      "type" => "sig",
      "keyHash" => wallet_address.payment_credential.hash
    }
  ]
}

script = NativeScript.from_json(script_json)

policy_id = NativeScript.to_script(script) |> Script.hash_script()

assets = %{
  Base.encode16("SUTRA-NATIVE-TKN") => 1
}

tx_id =
  Sutra.new_tx()
  |> Sutra.mint_asset(policy_id, assets, script)
  |> Sutra.add_output(wallet_address, %{
    policy_id => assets
  })
  |> Sutra.add_output(
    Address.from_script(policy_id, :testnet),
    Asset.from_lovelace(1000),
    {:datum_hash, "check As Hash"}
  )
  |> Sutra.build_tx!(wallet_address: [wallet_address])
  |> Sutra.sign_tx([extended_key])
  |> Sutra.submit_tx()

IO.puts("Tx Submitted for Mint with Native Script TxId: #{tx_id}")
