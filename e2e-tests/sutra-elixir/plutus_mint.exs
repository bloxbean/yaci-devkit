defmodule AlwaysSucceedMint do
  @moduledoc false

  alias Sutra.Cardano.Address
  alias Sutra.Cardano.Asset
  alias Sutra.Cardano.Script
  alias Sutra.Crypto.Key
  alias Sutra.Data

  @compiled_code "589a010100323232323223225333004323232323232533300a3370e9000000899251375c601a60186ea800854ccc028cdc3a4004002264664464a66601c66e1d2000300f37540042a66601c66e1cdd6980898081baa00200114a22c2c6eb4018c038004c038c03c004c030dd50010b18051baa001300b300c003300a002300900230090013006375400229309b2b1bae0015734aae7555cf2ba15745"

  def run() do

    mnemonic =
      "test test test test test test test test test test test test test test test test test test test test test test test sauce"

    {:ok, root_key} = Key.root_key_from_mnemonic(mnemonic)

    # deriving two account to have utxo to cover collateral
    {:ok, extended_key_acct1} = Key.derive_child(root_key, 0, 0)
    {:ok, wallet_address_1} = Key.address(extended_key_acct1, :preprod)


    {:ok, extended_key_acct2} = Key.derive_child(root_key, 1, 0)
    {:ok, wallet_address_2} = Key.address(extended_key_acct2, :preprod)

    simple_mint_script =
      @compiled_code
      |> Script.apply_params([Base.encode16("some-params", case: :lower)])
      |> Script.new(:plutus_v3)

    mint_script_address = Address.from_script(simple_mint_script, :preprod)

    out_token_name = Base.encode16("YACI-DEVKIT-TEST", case: :lower)
    policy_id = Script.hash_script(simple_mint_script)

    out_value =
      Asset.zero()
      |> Asset.add(policy_id, out_token_name, 100)

    tx_id =
      Sutra.new_tx()
      |> Sutra.mint_asset(policy_id, %{out_token_name => 100}, simple_mint_script, Data.void())
      |> Sutra.add_output(mint_script_address, out_value, {:inline_datum, 58})
      |> Sutra.build_tx!(wallet_address: [wallet_address_1, wallet_address_2])
      |> Sutra.sign_tx([extended_key_acct1, extended_key_acct2])
      |> Sutra.submit_tx()

    IO.puts(" Tx submitted with : #{tx_id}")
  end
end

# Setup sutra package & init yaci provider
Code.eval_file("setup.exs")

AlwaysSucceedMint.run()
