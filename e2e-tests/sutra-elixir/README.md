# Sutra Elixir Scripts

This README explains how to install Elixir and run the example Elixir scripts in this folder.

## 1. Install Elixir

On Debian/Ubuntu you can install Elixir with:

```bash
sudo apt-get update
sudo apt-get install -y elixir
```

For other platforms or the latest release, see the official instructions: https://elixir-lang.org/install.html

## 2. Run Tests

- Native Script Minting:

```bash
elixir nativescript_mint.exs
```

- Plutus Script Minting:

```bash
elixir plutus_mint.exs
```

- Register stake credential:

```bash
elixir register_stake.exs
```

- Delegate vote to DRep:

```bash
elixir delegate_vote.exs
```

- Withdraw stake:

```bash
elixir withdraw_stake.exs
```

> **Warning:**
>
> You must run `register_stake.exs` at least once before delegating to a DRep or withdrawing stake.

> **Note:**
>
> If your Yaci Devkit is running on a custom endpoint, export these environment variables before running the scripts:
>
> ```bash
> export YACI_GENERAL_API_URL="http://your-host:8080"
> export YACI_ADMIN_API_URL="http://your-host:10000"
> ```
