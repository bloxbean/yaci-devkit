import { useEffect, useRef, useState } from "react";
import { Check } from "./icons";
import { formatNumber, hex, useInView, useInterval, usePrefersReducedMotion } from "./hooks";

const CMD = "yaci-devkit up --enable-yaci-store";

const STEPS = [
  ["Components ready", "cardano-node · Yano · Yaci Store"],
  ["Genesis bootstrapped by Yano", "PV11 · companion mode"],
  ["cardano-node took over", "producing a block every second"],
  ["Yaci Store indexing", "Blockfrost API on :8080"],
  ["Admin API, wallet & MCP", "on :10000"],
];

const EPOCH_LENGTH = 600;
const CHAIN = 7;
const LOG = 5;

// Illustrative devnet activity, with the colour of the line's tag.
const EVENTS = [
  { tag: "topup", c: "#34d399", text: (s) => `addr_test1qz${hex(s, 4)}…${hex(s + 7, 5)}  +10,000 ₳` },
  { tag: "tx", c: "#22d3ee", text: (s) => `${hex(s, 8)}…  2 inputs → 3 outputs` },
  { tag: "mint", c: "#fbbf24", text: (s) => `NFT  policy ${hex(s + 3, 8)}…` },
  { tag: "plutus", c: "#a78bfa", text: () => `V3 script validated · scalus` },
  { tag: "cip-30", c: "#ff7a45", text: () => `signTx approved · yacidevkit wallet` },
  { tag: "mcp", c: "#f472b6", text: () => `devnet_utxos → 3 UTxOs` },
];

function initialChain() {
  return Array.from({ length: CHAIN }, (_, i) => ({ n: i + 1, txs: [1, 0, 2, 1, 3, 0, 2][i] }));
}

export default function HeroConsole() {
  const reduced = usePrefersReducedMotion();
  const [ref, inView] = useInView();
  const [typed, setTyped] = useState(0);
  const [steps, setSteps] = useState(0);
  const [chain, setChain] = useState(initialChain);
  const [log, setLog] = useState([]);
  const seq = useRef(0);

  const booted = steps >= STEPS.length;
  const tip = chain[chain.length - 1].n;

  // Reduced motion: show the finished boot and a still chain.
  useEffect(() => {
    if (!reduced) return;
    setTyped(CMD.length);
    setSteps(STEPS.length);
    setLog(EVENTS.slice(0, LOG).map((e, i) => ({ id: i, slot: 3 + i, ...e, line: e.text(i + 1) })));
  }, [reduced]);

  useInterval(() => setTyped((t) => Math.min(CMD.length, t + 1)), 42, inView && !reduced && typed < CMD.length);
  useInterval(() => setSteps((s) => s + 1), 520, inView && !reduced && typed >= CMD.length && !booted);

  useInterval(
    () => {
      const n = tip + 1;
      const txs = Math.floor(Math.random() * 4);
      setChain((c) => [...c.slice(1), { n, txs }]);
      if (Math.random() < 0.8) {
        const e = EVENTS[Math.floor(Math.random() * EVENTS.length)];
        const id = ++seq.current;
        setLog((l) => [...l.slice(-(LOG - 1)), { id, slot: n, ...e, line: e.text(n * 31 + id) }]);
      }
    },
    1000,
    inView && !reduced && booted
  );

  const slotInEpoch = tip % EPOCH_LENGTH;

  return (
    <div ref={ref}>
      <div className="dk-console-wrap">
        <div className="dk-console" role="img" aria-label="Illustration of yaci-devkit up starting a local Cardano devnet and producing blocks">
          <div className="dk-console-bar">
            <div className="dk-dots" aria-hidden="true">
              <i />
              <i />
              <i />
            </div>
            <span className="dk-console-title">yaci-devkit · devnet:default</span>
            <span className={`dk-live${booted ? "" : " boot"}`}>
              <i />
              {booted ? "LIVE" : "STARTING"}
            </span>
          </div>

          <div className="dk-boot">
            <div className="dk-cmd">
              <span className="p">$ </span>
              {CMD.slice(0, typed)}
              {typed < CMD.length && <span className="dk-caret" />}
            </div>
            {STEPS.map(([title, sub], i) => {
              const state = i < steps ? "done" : i === steps && typed >= CMD.length ? "run" : "";
              return (
                <div key={title} className={`dk-step ${state}`}>
                  <span className="st">{state === "done" && <Check />}</span>
                  <b>{title}</b>
                  <span className="sub">{sub}</span>
                </div>
              );
            })}
          </div>

          <div className={`dk-live-panel${booted ? " on" : ""}`}>
            <div className="dk-tip">
              <div>
                <span className="label">Block</span>
                <span className="value">{booted ? formatNumber(tip) : "—"}</span>
              </div>
              <div>
                <span className="label">Slot</span>
                <span className="value grad">{booted ? formatNumber(tip) : "—"}</span>
              </div>
              <div className="dk-progress">
                <div className="row">
                  <span>
                    Epoch 0 <b>{booted ? `${((slotInEpoch / EPOCH_LENGTH) * 100).toFixed(1)}%` : ""}</b>
                  </span>
                  <span>1s blocks</span>
                </div>
                <div className="track">
                  <div className="fill" style={{ width: booted ? `${(slotInEpoch / EPOCH_LENGTH) * 100}%` : "0%" }} />
                </div>
              </div>
            </div>

            <div className="dk-chain" aria-hidden="true">
              {chain.map((b, i) => (
                <div key={b.n} className={`dk-blk${i === chain.length - 1 ? " head" : ""}`}>
                  <span className="no">#{b.n}</span>
                  <span className="txs">
                    {Array.from({ length: 3 }).map((_, k) => (
                      <i key={k} className={k < b.txs ? "on" : ""} />
                    ))}
                  </span>
                </div>
              ))}
            </div>

            <div className="dk-log" aria-hidden="true">
              <div className="dk-log-inner">
                {log.map((l) => (
                  <div key={l.id} className="dk-log-line">
                    <span className="slot">{formatNumber(l.slot)}</span>
                    <span className="tag" style={{ "--c": l.c }}>
                      {l.tag}
                    </span>
                    <span className="evt">{l.line}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
      <p className="dk-console-note">Illustrative. Run the command and watch your own devnet come up.</p>
    </div>
  );
}
