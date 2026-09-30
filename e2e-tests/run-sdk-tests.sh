#!/usr/bin/env bash
# Run the SDK compatibility e2e tests (Evolution SDK, Lucid Evolution, MeshJS, PyCardano,
# cardano-client-lib) against a Yaci DevKit devnet using Yaci Store as the Blockfrost-compatible backend.
#
# Usage: e2e-tests/run-sdk-tests.sh [options]
#   --update              Update every SDK to its latest published version before running
#   --store-jar <path>    Restart DevKit (Yaci Store in java mode) with this yaci-store jar
#   --store-native <path> Restart DevKit (Yaci Store in native mode) with this native binary. It is installed as
#                         yaci-store-all for --node-mode yano-only and as yaci-store-n2c otherwise; the existing
#                         binary is backed up once to <name>.orig
#   --node-mode <mode>    DevKit node mode for --restart: haskell-only (default), companion, yano-only, yano-primary
#   --restart             Restart DevKit with the currently installed Yaci Store (fresh devnet)
#   --ccl-version <ver>   cardano-client-lib version (default: the version in the JBang scripts)
#   --only <suites>       Comma-separated subset: evolution-sdk,lucid-evo,meshjs,pycardano,cardano-client-lib
#
# Environment: YACI_STORE_URL (default http://localhost:8080/api/v1),
#              YACI_ADMIN_URL (default http://localhost:10000/local-cluster/api)
set -uo pipefail

E2E_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CLI_DIR="$E2E_DIR/../applications/cli"
LOG_DIR="${LOG_DIR:-$E2E_DIR/.logs}"
STORE_URL="${YACI_STORE_URL:-http://localhost:8080/api/v1}"
ADMIN_URL="${YACI_ADMIN_URL:-http://localhost:10000/local-cluster/api}"
TEST_TIMEOUT="${TEST_TIMEOUT:-300}"
ALL_SUITES="evolution-sdk,lucid-evo,meshjs,pycardano,cardano-client-lib"

UPDATE=false; RESTART=false; STORE_JAR=""; STORE_NATIVE=""; NODE_MODE=""; CCL_VERSION=""; ONLY="$ALL_SUITES"
while [ $# -gt 0 ]; do
  case "$1" in
    --update) UPDATE=true ;;
    --restart) RESTART=true ;;
    --store-jar) STORE_JAR="$2"; RESTART=true; shift ;;
    --store-native) STORE_NATIVE="$2"; RESTART=true; shift ;;
    --node-mode) NODE_MODE="$2"; shift ;;
    --ccl-version) CCL_VERSION="$2"; shift ;;
    --only) ONLY="$2"; shift ;;
    -h|--help) sed -n '2,19p' "$0"; exit 0 ;;
    *) echo "Unknown option: $1"; exit 2 ;;
  esac
  shift
done

mkdir -p "$LOG_DIR"
RESULTS=()
FAILED=0

log() { printf '\n==> %s\n' "$*"; }
selected() { [[ ",$ONLY," == *",$1,"* ]]; }

latest_npm() { npm view "$1" dist-tags.latest 2>/dev/null; }
latest_pypi() { curl -sf "https://pypi.org/pypi/$1/json" | python3 -c 'import json,sys; print(json.load(sys.stdin)["info"]["version"])'; }
latest_maven() {
  curl -sf "https://repo1.maven.org/maven2/com/bloxbean/cardano/$1/maven-metadata.xml" | sed -n 's:.*<latest>\(.*\)</latest>.*:\1:p'
}
set_npm_version() { # <dir> <package> <version>
  (cd "$E2E_DIR/$1" && python3 - "$2" "$3" <<'EOF'
import json, sys
pkg, ver = sys.argv[1], sys.argv[2]
d = json.load(open("package.json"))
d.setdefault("dependencies", {})[pkg] = ver
open("package.json", "w").write(json.dumps(d, indent=2) + "\n")
EOF
  )
}

update_versions() {
  log "Updating SDKs to latest versions"
  local v
  v=$(latest_npm @evolution-sdk/evolution) && set_npm_version evolution-sdk @evolution-sdk/evolution "$v" && echo "evolution-sdk      @evolution-sdk/evolution $v"
  v=$(latest_npm @lucid-evolution/lucid) && set_npm_version lucid-evo @lucid-evolution/lucid "$v" && echo "lucid-evo          @lucid-evolution/lucid $v"
  for p in @meshsdk/core @meshsdk/core-cst @meshsdk/common; do
    v=$(latest_npm "$p") && set_npm_version meshjs "$p" "$v" && echo "meshjs             $p $v"
  done
  v=$(latest_pypi pycardano) && sed -i.bak -E "s/^pycardano==.*/pycardano==$v/" "$E2E_DIR/pycardano/requirements.txt" \
    && rm -f "$E2E_DIR/pycardano/requirements.txt.bak" && echo "pycardano          pycardano $v"
  v=$(latest_maven cardano-client-lib) && [ -n "$v" ] && {
    for f in "$E2E_DIR"/cardano-client-lib/*.java; do
      sed -i.bak -E "s/(\\\$\{ccl\.version:)[^}]+\}/\1$v}/g" "$f" && rm -f "$f.bak"
    done
    echo "cardano-client-lib cardano-client-lib $v"
  }
}

devkit_ready() { curl -sf "$STORE_URL/epochs/latest/parameters" 2>/dev/null | grep -q min_fee; }

restart_devkit() {
  log "Restarting DevKit (fresh devnet)"
  local pattern="yaci-cli.jar|components/store/yaci-store|store/yaci-store-(n2c|all)|components/yano/|cardano-node run|cardano-submit-api"
  local pids
  pids=$(ps -eo pid,command | grep -E "$pattern" | grep -v grep | awk '{print $1}')
  [ -n "$pids" ] && kill -TERM $pids 2>/dev/null
  for _ in $(seq 1 30); do
    ps -eo command | grep -E "$pattern" | grep -vq grep || break
    sleep 1
  done
  local store_dir="$HOME/.yaci-cli/components/store" store_mode=java
  if [ -n "$STORE_JAR" ]; then
    cp "$STORE_JAR" "$store_dir/yaci-store.jar" || { echo "Cannot copy $STORE_JAR"; exit 1; }
    echo "Installed Yaci Store jar: $STORE_JAR"
  fi
  if [ -n "$STORE_NATIVE" ]; then
    local bin=yaci-store-n2c
    [ "$NODE_MODE" = "yano-only" ] && bin=yaci-store-all
    [ -f "$store_dir/$bin" ] && [ ! -f "$store_dir/$bin.orig" ] && cp -p "$store_dir/$bin" "$store_dir/$bin.orig"
    cp "$STORE_NATIVE" "$store_dir/$bin" && chmod +x "$store_dir/$bin" || { echo "Cannot copy $STORE_NATIVE"; exit 1; }
    store_mode=native
    echo "Installed Yaci Store native binary as $bin: $STORE_NATIVE (original kept as $bin.orig)"
  fi
  [ -f "$CLI_DIR/build/libs/yaci-cli.jar" ] || { echo "Build the CLI first: cd applications/cli && ./gradlew clean build -x test"; exit 1; }
  (cd "$CLI_DIR" && nohup java -Dyaci.store.enabled=true -Dyaci.store.mode=$store_mode ${NODE_MODE:+-DnodeMode=$NODE_MODE} -jar build/libs/yaci-cli.jar \
      create-node -o --start > "$LOG_DIR/devkit.log" 2>&1 &)
  for _ in $(seq 1 150); do devkit_ready && { echo "DevKit ready"; sleep 5; return 0; }; sleep 2; done
  echo "DevKit did not become ready; see $LOG_DIR/devkit.log"; exit 1
}

run_test() { # <suite> <name> <command...>
  local suite="$1" name="$2"; shift 2
  local logf="$LOG_DIR/$suite-$name.log" status
  (cd "$E2E_DIR/$suite" && timeout "$TEST_TIMEOUT" "$@") > "$logf" 2>&1
  status=$?
  if [ $status -eq 0 ]; then
    RESULTS+=("PASS|$suite|$name|")
  else
    FAILED=$((FAILED + 1))
    local reason
    reason=$(grep -vE '^\s+at |^\s*$|^\s*[0-9]+ \|' "$logf" | grep -iE 'error|fail|exception' | tail -1 | cut -c1-110)
    RESULTS+=("FAIL|$suite|$name|${reason:-exit $status}")
  fi
  printf '%-4s %-20s %s\n' "$([ $status -eq 0 ] && echo PASS || echo FAIL)" "$suite" "$name"
  sleep 3 # let the next block include the tx so the following test sees fresh UTxOs
}

$UPDATE && update_versions
$RESTART && restart_devkit
devkit_ready || { echo "Yaci Store is not reachable at $STORE_URL. Start DevKit or pass --restart."; exit 1; }
curl -sf "$ADMIN_URL/admin/devnet" > /dev/null || { echo "DevKit admin API is not reachable at $ADMIN_URL"; exit 1; }

export YACI_STORE_URL="$STORE_URL" YACI_ADMIN_URL="$ADMIN_URL"

if selected evolution-sdk; then
  log "Evolution SDK"; (cd "$E2E_DIR/evolution-sdk" && bun install --silent)
  run_test evolution-sdk protocol_params bun protocol_params.ts
  run_test evolution-sdk payment bun payment.ts
  run_test evolution-sdk plutus_v3 bun plutus_v3.ts
fi

if selected lucid-evo; then
  log "Lucid Evolution"; (cd "$E2E_DIR/lucid-evo" && bun install --silent)
  run_test lucid-evo payment bun payment.ts
  run_test lucid-evo plutus_v2 bun plutus_v2.ts
  run_test lucid-evo plutus_v3 bun plutus_v3.ts
fi

if selected meshjs; then
  log "MeshJS"; (cd "$E2E_DIR/meshjs" && bun install --silent)
  run_test meshjs payment bun payment.ts
  run_test meshjs payment_splitter_plutusV3 bun payment_splitter_plutusV3.ts
fi

if selected pycardano; then
  log "PyCardano"
  (cd "$E2E_DIR/pycardano" && { [ -d .venv ] || python3 -m venv .venv; } && .venv/bin/pip install -q -r requirements.txt)
  run_test pycardano payment .venv/bin/python payment.py
  run_test pycardano plutus_v3 .venv/bin/python plutus_v3.py
fi

if selected cardano-client-lib; then
  log "cardano-client-lib${CCL_VERSION:+ $CCL_VERSION}"
  JBANG_ARGS=(--quiet); [ -n "$CCL_VERSION" ] && JBANG_ARGS+=(-Dccl.version="$CCL_VERSION")
  run_test cardano-client-lib Payment jbang "${JBANG_ARGS[@]}" Payment.java
  run_test cardano-client-lib MintToken jbang "${JBANG_ARGS[@]}" MintToken.java
  run_test cardano-client-lib PlutusV3 jbang "${JBANG_ARGS[@]}" PlutusV3.java
fi

log "Summary (logs in $LOG_DIR)"
printf '%-6s %-20s %-28s %s\n' RESULT SUITE TEST DETAIL
for r in "${RESULTS[@]}"; do
  IFS='|' read -r res suite name reason <<< "$r"
  printf '%-6s %-20s %-28s %s\n' "$res" "$suite" "$name" "$reason"
done
[ $FAILED -eq 0 ] && echo "All tests passed." || echo "$FAILED test(s) failed."
exit $FAILED
