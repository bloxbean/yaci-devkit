import { useEffect, useState } from "react";
import { Check } from "./icons";
import { formatNumber, useInView, useInterval, usePrefersReducedMotion } from "./hooks";

/* ---------------- Block time: 1s down to 100ms ---------------- */

const SPEEDS = [
  { label: "1s", ms: 1000, flag: "1" },
  { label: "200ms", ms: 200, flag: "0.2" },
  { label: "100ms", ms: 100, flag: "0.1" },
];
const SHOWN = 16;

export function BlockSpeed() {
  const [ref, inView] = useInView();
  const reduced = usePrefersReducedMotion();
  const [active, setActive] = useState(0);
  const [paused, setPaused] = useState(false);
  const [blocks, setBlocks] = useState(() => Array.from({ length: SHOWN }, (_, i) => i + 1));
  const speed = SPEEDS[active];

  useInterval(() => setBlocks((b) => [...b.slice(1), b[b.length - 1] + 1]), speed.ms, inView && !reduced);
  useInterval(() => setActive((a) => (a + 1) % SPEEDS.length), 4200, inView && !reduced && !paused);

  return (
    <div ref={ref} className="dk-speed" onMouseEnter={() => setPaused(true)} onMouseLeave={() => setPaused(false)}>
      <div className="dk-seg" role="tablist" aria-label="Block time">
        {SPEEDS.map((s, i) => (
          <button
            key={s.label}
            type="button"
            role="tab"
            aria-selected={i === active}
            className={i === active ? "active" : ""}
            onClick={() => {
              setActive(i);
              setPaused(true);
            }}
          >
            {s.label}
          </button>
        ))}
        <span className="rate">
          <b>{formatNumber(60000 / speed.ms)}</b> blocks / min
        </span>
      </div>
      <div className="dk-speed-row" aria-hidden="true">
        {blocks.map((n, i) => (
          <i key={n} className={i === blocks.length - 1 ? "head" : ""} style={{ "--o": 0.25 + (0.75 * i) / blocks.length }} />
        ))}
      </div>
      <div className="dk-term mini">
        <div className="dk-term-body">
          <span className="p">$ </span>yaci-devkit up <span className="k">--block-time</span> <span className="y">{speed.flag}</span>{" "}
          <span className="k">--slot-length</span> <span className="y">{speed.flag}</span> <span className="k">--epoch-length</span>{" "}
          <span className="y">80</span>
        </div>
      </div>
    </div>
  );
}

/* ---------------- Node modes ---------------- */

const MODES = [
  {
    id: "companion",
    desc: "Yano builds the chain fast, then hands it to cardano-node. You get a real Haskell node and its socket.",
  },
  {
    id: "yano-only",
    desc: "Yano is the only node and Yaci Store syncs from it. The lightest, fastest devnet.",
  },
];

export function NodeModes() {
  const [ref, inView] = useInView();
  const reduced = usePrefersReducedMotion();
  const [active, setActive] = useState(0);
  const [paused, setPaused] = useState(false);
  useInterval(() => setActive((a) => (a + 1) % MODES.length), 4000, inView && !reduced && !paused);
  const mode = MODES[active];

  return (
    <div ref={ref} className="dk-modes" onMouseEnter={() => setPaused(true)} onMouseLeave={() => setPaused(false)}>
      <div className="dk-seg small" role="tablist" aria-label="Node mode">
        {MODES.map((m, i) => (
          <button
            key={m.id}
            type="button"
            role="tab"
            aria-selected={i === active}
            className={i === active ? "active" : ""}
            onClick={() => {
              setActive(i);
              setPaused(true);
            }}
          >
            {m.id}
          </button>
        ))}
      </div>
      <div className={`dk-modes-flow ${mode.id}`} aria-hidden="true">
        <div className="node yano">
          <b>Yano</b>
          <span>Java node</span>
        </div>
        <div className="handover">
          <i />
        </div>
        <div className="node haskell">
          <b>cardano-node</b>
          <span>Haskell</span>
        </div>
      </div>
      <p className="dk-modes-desc dk-fade-swap" key={mode.id}>
        {mode.desc}
      </p>
      <div className="dk-term mini">
        <div className="dk-term-body">
          <span className="c"># node.properties</span>
          {"\n"}
          <span className="k">nodeMode</span>=<span className="y">{mode.id}</span>
        </div>
      </div>
    </div>
  );
}

/* ---------------- Blockfrost-compatible API ---------------- */

const BF_CMD = "curl localhost:8080/api/v1/blocks/latest";
const BF_FIELDS = [
  ["hash", '"9c41…e07a"'],
  ["height", "1284"],
  ["slot", "1284"],
  ["epoch", "2"],
  ["tx_count", "3"],
  ["block_vrf", '"vrf_vk1…"'],
];

export function BlockfrostTerm() {
  const [ref, inView] = useInView();
  const reduced = usePrefersReducedMotion();
  const [typed, setTyped] = useState(0);
  const [shown, setShown] = useState(0);

  useEffect(() => {
    if (reduced) {
      setTyped(BF_CMD.length);
      setShown(BF_FIELDS.length + 2);
    }
  }, [reduced]);

  useInterval(
    () => {
      if (typed < BF_CMD.length) setTyped((t) => Math.min(BF_CMD.length, t + 2));
      else if (shown < BF_FIELDS.length + 36) setShown((s) => s + 1);
      else {
        setTyped(0);
        setShown(0);
      }
    },
    typed < BF_CMD.length ? 45 : 110,
    inView && !reduced
  );

  const done = typed >= BF_CMD.length;
  const rows = Math.min(shown, BF_FIELDS.length + 2);

  return (
    <div className="dk-term" ref={ref}>
      <div className="dk-term-bar">
        <div className="dk-dots" aria-hidden="true">
          <i />
          <i />
          <i />
        </div>
        GET /blocks/latest
      </div>
      <div className="dk-term-body" style={{ minHeight: 210 }}>
        <span className="p">$ </span>
        {BF_CMD.slice(0, typed)}
        {!done && <span className="dk-caret" />}
        {"\n"}
        {rows > 0 && <div className="dk-json-line">{"{"}</div>}
        {BF_FIELDS.slice(0, Math.max(0, rows - 1)).map(([k, v], i) => (
          <div key={k} className="dk-json-line">
            {"  "}
            <span className="k">&quot;{k}&quot;</span>: <span className={v.startsWith('"') ? "s" : "n"}>{v}</span>
            {i < BF_FIELDS.length - 1 ? "," : ""}
          </div>
        ))}
        {rows === BF_FIELDS.length + 2 && <div className="dk-json-line">{"}"}</div>}
      </div>
    </div>
  );
}

/* ---------------- MCP: an AI agent drives the devnet ---------------- */

const TOOLS = ["devnet_status", "devnet_topup", "devnet_utxos"];

export function McpChat() {
  const [ref, inView] = useInView();
  const reduced = usePrefersReducedMotion();
  // 0 = question, 1..3 = tool i-1 running, 4 = typing, 5 = answer, then hold
  const [step, setStep] = useState(0);
  useEffect(() => {
    if (reduced) setStep(5);
  }, [reduced]);
  useInterval(() => setStep((s) => (s >= 10 ? 0 : s + 1)), 1000, inView && !reduced);

  const s = Math.min(step, 5);
  const toolState = (i) => (s > i + 1 ? "done" : s === i + 1 ? "running" : "");

  return (
    <div className="dk-chat" ref={ref}>
      <div className="dk-msg user">Fund my test wallet with 10,000 ADA and show its UTxOs.</div>
      {TOOLS.map((t, i) => (
        <div key={t} className={`dk-tool ${toolState(i)}`}>
          <span className="st">
            <Check />
          </span>
          {t}
        </div>
      ))}
      <div className="dk-msg agent" style={{ minHeight: 44 }}>
        {s < 4 ? (
          <span style={{ color: "var(--faint)" }}>Calling devnet tools…</span>
        ) : s === 4 ? (
          <span className="dk-typing" aria-label="typing">
            <i />
            <i />
            <i />
          </span>
        ) : (
          <span className="dk-fade-swap">
            Done: 10,000 ₳ landed in block 1,284. The address now holds <b>1 UTxO</b>.
          </span>
        )}
      </div>
    </div>
  );
}

/* ---------------- CIP-30 wallet ---------------- */

const WALLET_STEPS = ["request", "signing", "signed"];

export function WalletArt() {
  const [ref, inView] = useInView();
  const reduced = usePrefersReducedMotion();
  const [t, setT] = useState(0);
  useEffect(() => {
    if (reduced) setT(5);
  }, [reduced]);
  useInterval(() => setT((x) => (x >= 7 ? 0 : x + 1)), 900, inView && !reduced);
  const state = t < 3 ? WALLET_STEPS[0] : t < 5 ? WALLET_STEPS[1] : WALLET_STEPS[2];
  const balance = state === "signed" ? "9,994.83" : "10,000.00";

  return (
    <div ref={ref} className="dk-wallet">
      <div className="head">
        <span className="dot" />
        <span>window.cardano.yacidevkit</span>
        <span className="net">devnet</span>
      </div>
      <div className="bal">
        <span className="lbl">Account 0 · addr_test1qz…9xn5f</span>
        <b key={balance} className="dk-fade-swap">
          {balance} <em>₳</em>
        </b>
      </div>
      <div className={`req ${state}`}>
        <div className="req-top">
          <span className="app">localhost:3000</span>
          <span className="what">wants you to sign a transaction</span>
        </div>
        <div className="req-row">
          <span>Send</span>
          <b>5 ₳ + 1 NFT</b>
        </div>
        <div className="req-btn">
          {state === "request" && <span>Approve</span>}
          {state === "signing" && (
            <span className="dk-typing" aria-label="signing">
              <i />
              <i />
              <i />
            </span>
          )}
          {state === "signed" && (
            <span className="ok">
              <Check /> Signed and submitted
            </span>
          )}
        </div>
      </div>
    </div>
  );
}

/* ---------------- Rollback testing: forks ---------------- */

export function ForkArt() {
  return (
    <div className="dk-fork" aria-hidden="true">
      <svg viewBox="0 0 320 132">
        <defs>
          <linearGradient id="dk-fork-a" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0" stopColor="#ff6a3d" />
            <stop offset="1" stopColor="#fbbf24" />
          </linearGradient>
        </defs>
        {/* common prefix */}
        {[14, 54, 94].map((x, i) => (
          <g key={x}>
            {i > 0 && <path className="link" d={`M${x - 14} 66 L${x} 66`} />}
            <rect className="b common" x={x} y="52" width="28" height="28" rx="7" />
          </g>
        ))}
        {/* branch A: survives */}
        <path className="link branch-a" d="M122 66 C136 66 132 30 148 30" />
        {[148, 188, 228].map((x, i) => (
          <g key={x} className="grow" style={{ "--d": `${0.4 + i * 0.5}s` }}>
            {i > 0 && <path className="link" d={`M${x - 12} 30 L${x} 30`} />}
            <rect className="b a" x={x} y="16" width="28" height="28" rx="7" />
          </g>
        ))}
        {/* branch B: rolled back */}
        <g className="loser">
          <path className="link" d="M122 66 C136 66 132 102 148 102" />
          {[148, 188].map((x, i) => (
            <g key={x} className="grow" style={{ "--d": `${0.6 + i * 0.6}s` }}>
              {i > 0 && <path className="link" d={`M${x - 12} 102 L${x} 102`} />}
              <rect className="b bb" x={x} y="88" width="28" height="28" rx="7" />
            </g>
          ))}
          <text className="rb" x="228" y="107">
            ↺ rolled back
          </text>
        </g>
        <g className="joined">
          <path className="link" d="M256 30 L268 30" />
          <rect className="b a" x="268" y="16" width="28" height="28" rx="7" />
        </g>
      </svg>
      <div className="dk-fork-cmds">
        <span className="c1">create-forks</span>
        <span className="arrow">→</span>
        <span className="c2">join-forks</span>
      </div>
    </div>
  );
}

/* ---------------- Catch-up after a long pause ---------------- */

export function CatchUpArt() {
  const [ref, inView] = useInView();
  const reduced = usePrefersReducedMotion();
  const [t, setT] = useState(0);
  useEffect(() => {
    if (reduced) setT(8);
  }, [reduced]);
  useInterval(() => setT((x) => (x >= 11 ? 0 : x + 1)), 700, inView && !reduced);

  const phase = t < 2 ? 0 : t < 7 ? 1 : 2;
  const progress = phase === 0 ? 0 : phase === 1 ? Math.min(100, ((t - 1) / 5) * 100) : 100;

  return (
    <div ref={ref} className="dk-catch">
      <div className="row">
        <span className="when">Fri 18:04</span>
        <span className="evt">devnet stopped, laptop lid closed</span>
      </div>
      <div className="gap">
        <i />
        <span>63 hours later</span>
        <i />
      </div>
      <div className={`row${phase >= 1 ? " on" : ""}`}>
        <span className="when">Mon 09:12</span>
        <span className="evt">start · backfilling with Yano</span>
      </div>
      <div className="bar">
        <i style={{ width: `${progress}%` }} />
      </div>
      <div className={`row ok${phase === 2 ? " on" : ""}`}>
        <span className="when">
          <Check />
        </span>
        <span className="evt">producing blocks again, same chain</span>
      </div>
    </div>
  );
}
