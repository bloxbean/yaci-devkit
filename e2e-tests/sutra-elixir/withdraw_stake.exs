alias Sutra.Cardano.Script.NativeScript
alias Sutra.Data
alias Sutra.Cardano.Script
alias Sutra.Crypto.Key

Code.eval_file("setup.exs")

mnemonic =
  "test test test test test test test test test test test test test test test test test test test test test test test sauce"

{:ok, root_key} = Key.root_key_from_mnemonic(mnemonic)

{:ok, extended_key} = Key.derive_child(root_key, 0, 0)
{:ok, wallet_address} = Key.address(extended_key, :preprod)

{:ok, extended_key_2} = Key.derive_child(root_key, 0, 0)
{:ok, wallet_address_2} = Key.address(extended_key, :preprod)

always_true_script = "58da010100229800aba2aba1aab9eaab9dab9a9bae002488888966002646465300130063754003300800398040012444b30013370e9000001c4c9289bae300b300a375400915980099b874800800e2646644944c030004c030c034004c028dd5002456600266e1d2004003899251300b300a375400915980099b874801800e2646644944dd698060009806180680098051baa0048acc004cdc3a40100071324a2601660146ea80122646644944dd698060009806180680098051baa00440208041008201040203007300800130070013004375400f149a26cac80101"

script =
  always_true_script
  |> Script.apply_params([Base.encode16("sutra-yaci-devkit-register-stake", case: :lower)])
  |> Script.new(:plutus_v3)

script_json = %{
  "type" => "all",
  "scripts" => [
    %{
      "type" => "sig",
      "keyHash" => wallet_address.payment_credential.hash
    }
  ]
}

tx_id =
  Sutra.new_tx()
  |> Sutra.withdraw_stake(script, Data.void(), 0)
  |> Sutra.withdraw_stake(NativeScript.from_json(script_json), 0)
  |> Sutra.withdraw_stake(wallet_address, 0)
  |> Sutra.build_tx!(wallet_address: [wallet_address, wallet_address_2])
  |> Sutra.sign_tx([extended_key, extended_key_2])
  |> Sutra.sign_tx_with_raw_extended_key(extended_key.stake_key)
  |> Sutra.submit_tx()

IO.inspect(tx_id)
