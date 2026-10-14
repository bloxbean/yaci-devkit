#!/bin/bash
# Homebrew formula for bloxbean/homebrew-tap, shared by release-cli.yml (publish) and dev-release-cli.yml (dry run).
#
#   homebrew-formula.sh name <version>
#       Prints the formula a version is published as, following the npm dist-tags in release-cli.yml:
#       a final release -> yaci-devkit, *-beta* -> yaci-devkit-beta, *-preview* -> nothing (not published).
#
#   homebrew-formula.sh render <formula> <version> <macos-arm64-url> <macos-arm64-sha256> <linux-x64-url> <linux-x64-sha256>
#       Prints the formula. Both formulas install the same `yaci-devkit` command, so they conflict with each other.
set -euo pipefail

formula_name() {
  case "$1" in
    *-preview*) echo "" ;;
    *-beta*) echo "yaci-devkit-beta" ;;
    *) echo "yaci-devkit" ;;
  esac
}

render() {
  local formula=$1 version=$2 mac_url=$3 mac_sha=$4 linux_url=$5 linux_sha=$6
  local class other desc
  case "$formula" in
    yaci-devkit)
      class=YaciDevkit; other=yaci-devkit-beta
      desc="Local Cardano devnet with Yaci Store, Yano, Ogmios and Kupo" ;;
    yaci-devkit-beta)
      class=YaciDevkitBeta; other=yaci-devkit
      desc="Local Cardano devnet with Yaci Store, Yano, Ogmios and Kupo (beta)" ;;
    *) echo "Unknown formula: $formula" >&2; exit 1 ;;
  esac

  # Quoted heredoc: Ruby's #{...} and the wrapper's ${HOME} / "$@" stay literal; @...@ placeholders are filled below.
  cat <<'FORMULA' | sed \
      -e "s|@CLASS@|$class|" -e "s|@DESC@|$desc|" -e "s|@VERSION@|$version|" -e "s|@OTHER@|$other|" \
      -e "s|@MAC_URL@|$mac_url|" -e "s|@MAC_SHA@|$mac_sha|" -e "s|@LINUX_URL@|$linux_url|" -e "s|@LINUX_SHA@|$linux_sha|"
class @CLASS@ < Formula
  desc "@DESC@"
  homepage "https://devkit.yaci.xyz/"
  version "@VERSION@"
  license "MIT"

  on_macos do
    # Built on macOS 15: the binary needs macOS 15 or later
    depends_on macos: :sequoia
    if Hardware::CPU.arm?
      url "@MAC_URL@"
      sha256 "@MAC_SHA@"
    end
  end

  on_linux do
    if Hardware::CPU.intel?
      url "@LINUX_URL@"
      sha256 "@LINUX_SHA@"
    end
  end

  # Not conflicts_with: it makes Homebrew load the other formula, which Homebrew refuses for a tap formula the
  # user has not installed by name ("untrusted tap"), so the install would fail. Linking already stops both
  # formulas from installing the command at once.
  def caveats
    <<~EOS
      This installs the `yaci-devkit` command, as @OTHER@ does: install only one of them.
      Devnets, downloaded components and ~/.yaci-cli/devkit.properties are shared between them.
    EOS
  end

  def install
    libexec.install "yaci-cli", "config"
    cfg = libexec/"config"
    # yaci-cli reads its config relative to the working directory, so point it at the installed config.
    # User overrides in ~/.yaci-cli/devkit.properties are imported last, so they win over the bundled config.
    # yaci.cli.bundled-config-dir is only the default for yaci.cli.plutus-costmodels-path (see application.properties),
    # so devkit.properties and environment variables can still override that path.
    (bin/"yaci-devkit").write <<~EOS
      #!/bin/bash
      exec "#{libexec}/yaci-cli" \\
        "-Dspring.config.import=optional:file:#{cfg}/application.properties,optional:file:#{cfg}/download.properties,optional:file:#{cfg}/node.properties,optional:file:${HOME}/.yaci-cli/devkit.properties" \\
        "-Dyaci.cli.bundled-config-dir=#{cfg}" "$@"
    EOS
  end

  test do
    assert_predicate libexec/"yaci-cli", :executable?
    assert_predicate libexec/"config/application.properties", :exist?
  end
end
FORMULA
}

case "${1:-}" in
  name) formula_name "$2" ;;
  render) shift; render "$@" ;;
  *) echo "Usage: $0 name <version> | render <formula> <version> <mac-url> <mac-sha> <linux-url> <linux-sha>" >&2; exit 1 ;;
esac
