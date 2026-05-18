Mix.install([:sutra_cardano])

alias Sutra.Provider.Yaci

# Setup Yaci Devkit provider
Application.put_env(:sutra, :provider, Yaci)

Application.put_env(
  :sutra,
  :yaci_general_api_url,
  System.get_env("YACI_GENERAL_API_URL", "http://localhost:8080")
)

Application.put_env(
  :sutra,
  :yaci_admin_api_url,
  System.get_env("YACI_ADMIN_API_URL", "http://localhost:10000")
)
