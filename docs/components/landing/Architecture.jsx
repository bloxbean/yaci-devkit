const EMBER = "#ff5a36";
const AMBER = "#fbbf24";
const CYAN = "#22d3ee";
const BLUE = "#3b8bff";
const VIOLET = "#a78bfa";
const MINT = "#34d399";
const PINK = "#f472b6";

const SDKS = [
  { name: "MeshJS", sub: "TypeScript", c: CYAN, mark: "M" },
  { name: "Evolution SDK", sub: "TypeScript", c: EMBER, mark: "Ev" },
  { name: "Lucid Evolution", sub: "TypeScript", c: VIOLET, mark: "L" },
  { name: "Cardano Client Lib", sub: "Java", c: BLUE, mark: "CL" },
  { name: "PyCardano", sub: "Python", c: MINT, mark: "Py" },
  { name: "Sutra", sub: "Elixir", c: PINK, mark: "S" },
];
const SDK_X = 20;
const SDK_W = 220;
const SDK_H = 46;
const sdkY = (i) => 56 + i * 58;

const APIS = [
  { name: "Ogmios", sub: "WebSocket :1337 · optional", y: 240, c: VIOLET },
  { name: "Kupo", sub: "UTxO index :1442 · optional", y: 298, c: VIOLET },
  { name: "Submit API", sub: "tx submission :8090", y: 356, c: MINT },
];
const API_X = 300;
const API_W = 190;

const CORE = { x: 550, y: 44, w: 330, h: 400 };

const TOOLS = [
  { name: "Yaci CLI", sub: "create-node · topup · tip", c: EMBER },
  { name: "Admin API", sub: ":10000 · REST · Swagger", c: AMBER },
  { name: "Yaci Viewer", sub: ":5173 · block explorer", c: CYAN },
  { name: "Yano Wallet", sub: "desktop wallet · Ledger", c: BLUE },
  { name: "CIP-30 wallet", sub: "window.cardano.yacidevkit", c: MINT },
  { name: "MCP server", sub: ":10000/mcp · AI agents", c: PINK },
];
const TOOL_X = 930;
const TOOL_W = 210;
const TOOL_H = 48;
const toolY = (i) => 54 + i * 58;

const curve = (x1, y1, x2, y2) => {
  const mx = (x1 + x2) / 2;
  return `M${x1} ${y1} C${mx} ${y1} ${mx} ${y2} ${x2} ${y2}`;
};

function Wire({ d, color, dur = 2.4, dots = 1, delay = 0, faint }) {
  return (
    <g opacity={faint ? 0.55 : 1}>
      <path className="wire" d={d} />
      <path className={`wire-flow${faint ? " faint" : ""}`} d={d} stroke={color} />
      {Array.from({ length: dots }).map((_, i) => (
        <circle key={i} r="3.4" fill={color} style={{ filter: `drop-shadow(0 0 4px ${color})` }}>
          <animateMotion dur={`${dur}s`} repeatCount="indefinite" path={d} begin={`${delay + (i * dur) / dots}s`} />
        </circle>
      ))}
    </g>
  );
}

function Chip({ x, y, w, h = 28, label, c }) {
  return (
    <g className="chip" style={{ "--c": c }}>
      <rect x={x} y={y} width={w} height={h} rx="8" />
      <text x={x + w / 2} y={y + h / 2 + 3.6} textAnchor="middle">
        {label}
      </text>
    </g>
  );
}

/** The DevKit mark, drawn at (x, y) with the given size. */
function MarkAt({ x, y, size }) {
  const s = size / 64;
  return (
    <g transform={`translate(${x} ${y}) scale(${s})`}>
      <rect x="2" y="2" width="60" height="60" rx="12" fill="#0E0F12" stroke="rgba(255,255,255,0.18)" strokeWidth="2" />
      <path d="M14 12 L24 12 L32 26 L40 12 L50 12 L36 36 L42 36 L24 56 L30 38 L22 38 Z" fill="#FF4E2D" />
    </g>
  );
}

export default function Architecture() {
  const { x: cx, y: cy, w: cw, h: ch } = CORE;
  const nodeY = 172;
  const chainY = 268;

  return (
    <div className="dk-arch">
      <div className="dk-arch-scroll">
        <svg viewBox="0 0 1160 460" role="img" aria-labelledby="dk-arch-title dk-arch-desc">
          <title id="dk-arch-title">How Yaci DevKit fits together</title>
          <desc id="dk-arch-desc">
            Off-chain SDKs such as MeshJS, Evolution SDK, Lucid Evolution, Cardano Client Lib, PyCardano and Sutra
            connect to the devnet through Yaci Store's Blockfrost-compatible API, Ogmios, Kupo and the Submit API. The
            local devnet runs Yano and cardano-node, in companion or yano-only mode. The Yaci CLI, admin API, Yaci
            Viewer, Yano Wallet, the CIP-30 wallet and the MCP server let you operate it.
          </desc>
          <defs>
            <linearGradient id="dk-core-stroke" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0" stopColor={EMBER} />
              <stop offset="0.55" stopColor={AMBER} />
              <stop offset="1" stopColor={VIOLET} />
            </linearGradient>
            <radialGradient id="dk-core-glow" cx="0.5" cy="0.3" r="0.75">
              <stop offset="0" stopColor={EMBER} stopOpacity="0.2" />
              <stop offset="1" stopColor={EMBER} stopOpacity="0" />
            </radialGradient>
            <linearGradient id="dk-blk-fill" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0" stopColor={EMBER} stopOpacity="0.95" />
              <stop offset="1" stopColor={AMBER} stopOpacity="0.75" />
            </linearGradient>
          </defs>

          <text className="col-label" x={SDK_X} y="30">01 · BUILD</text>
          <text className="col-label" x={API_X} y="30">02 · CONNECT</text>
          <text className="col-label" x={cx} y="30">03 · RUN</text>
          <text className="col-label" x={TOOL_X} y="30">04 · OPERATE</text>

          {/* Wires first, so the boxes sit on top */}
          {SDKS.map((s, i) => (
            <Wire
              key={s.name}
              d={curve(SDK_X + SDK_W, sdkY(i) + SDK_H / 2, API_X, 84 + i * 22)}
              color={s.c}
              dur={2.2}
              delay={i * 0.25}
            />
          ))}
          <Wire d={curve(SDK_X + SDK_W, sdkY(1) + SDK_H / 2, API_X, 263)} color={EMBER} dur={2.6} delay={0.4} faint />
          <Wire d={curve(SDK_X + SDK_W, sdkY(2) + SDK_H / 2, API_X, 321)} color={VIOLET} dur={2.6} delay={0.9} faint />

          <Wire d={curve(cx, 206, API_X + API_W, 140)} color={CYAN} dur={1.8} dots={2} />
          <Wire d={curve(cx, 246, API_X + API_W, 263)} color={VIOLET} dur={2} />
          <Wire d={curve(cx, 286, API_X + API_W, 321)} color={VIOLET} dur={2} delay={0.5} />
          <Wire d={curve(API_X + API_W, 379, cx, 330)} color={MINT} dur={2} delay={0.3} />

          {TOOLS.map((t, i) => (
            <Wire key={t.name} d={curve(TOOL_X, toolY(i) + TOOL_H / 2, cx + cw, 112 + i * 56)} color={t.c} dur={2.3} delay={i * 0.3} />
          ))}

          {/* 01 · SDKs */}
          {SDKS.map((s, i) => {
            const y = sdkY(i);
            return (
              <g key={s.name} className="sdk" style={{ "--c": s.c, "--i": i }}>
                <rect className="node-bg" x={SDK_X} y={y} width={SDK_W} height={SDK_H} rx="12" />
                <rect x={SDK_X + 10} y={y + 9} width="28" height="28" rx="8" fill={`${s.c}24`} stroke={`${s.c}88`} />
                <text x={SDK_X + 24} y={y + 27} textAnchor="middle" className="sdk-mark" fill={s.c}>
                  {s.mark}
                </text>
                <text className="node-title sm" x={SDK_X + 48} y={y + 21}>
                  {s.name}
                </text>
                <text className="node-sub" x={SDK_X + 48} y={y + 36}>
                  {s.sub}
                </text>
              </g>
            );
          })}
          <text className="node-sub faint" x={SDK_X + SDK_W / 2} y="426" textAnchor="middle">
            + any Blockfrost-compatible client
          </text>

          {/* 02 · Provider APIs */}
          <g>
            <rect className="node-bg" x={API_X} y="56" width={API_W} height="168" rx="16" stroke={`${CYAN}88`} />
            <g transform={`translate(${API_X + 18} 74)`} stroke={CYAN} fill="none" strokeWidth="1.6">
              <ellipse cx="10" cy="4" rx="10" ry="3.6" />
              <path d="M0 4v14c0 2 4.5 3.6 10 3.6s10-1.6 10-3.6V4" />
              <path d="M0 11c0 2 4.5 3.6 10 3.6s10-1.6 10-3.6" />
            </g>
            <text className="node-title" x={API_X + 48} y="88">Yaci Store</text>
            <text className="node-sub" x={API_X + 48} y="104">Blockfrost-compatible</text>
            {["GET  /blocks/latest", "GET  /addresses/…/utxos", "POST /tx/submit"].map((l, i) => (
              <text key={l} className="node-sub code" x={API_X + 18} y={132 + i * 18}>
                {l}
              </text>
            ))}
            <Chip x={API_X + 18} y={186} w={70} h={24} label=":8080" c={CYAN} />
            <Chip x={API_X + 96} y={186} w={78} h={24} label="scalus eval" c={MINT} />
          </g>
          {APIS.map((a) => (
            <g key={a.name}>
              <rect className="node-bg" x={API_X} y={a.y} width={API_W} height="46" rx="12" />
              <rect x={API_X} y={a.y + 13} width="3" height="20" rx="1.5" fill={a.c} />
              <text className="node-title sm" x={API_X + 16} y={a.y + 20}>
                {a.name}
              </text>
              <text className="node-sub" x={API_X + 16} y={a.y + 36}>
                {a.sub}
              </text>
            </g>
          ))}

          {/* 03 · Local devnet */}
          <g>
            <rect x={cx} y={cy} width={cw} height={ch} rx="24" fill="url(#dk-core-glow)" />
            <rect className="node-bg" x={cx} y={cy} width={cw} height={ch} rx="24" stroke="url(#dk-core-stroke)" strokeWidth="1.6" />
            <circle className="core-ring" cx={cx + 40} cy={cy + 40} r="24" stroke={EMBER} />
            <MarkAt x={cx + 24} y={cy + 24} size={32} />
            <text className="node-title" x={cx + 76} y={cy + 36} style={{ fontSize: 18 }}>
              Local devnet
            </text>
            <text className="node-sub" x={cx + 76} y={cy + 55}>
              one command · seconds to first block
            </text>

            {/* Node mode switch: companion, then yano-only */}
            <rect x={cx + 22} y={cy + 76} width={cw - 44} height="30" rx="15" className="mode-track" />
            <rect y={cy + 79} width={(cw - 50) / 2} height="24" rx="12" fill={`${EMBER}2e`} stroke={`${EMBER}aa`}>
              <animate
                attributeName="x"
                values={`${cx + 25};${cx + 25};${cx + 25 + (cw - 50) / 2};${cx + 25 + (cw - 50) / 2};${cx + 25}`}
                keyTimes="0;0.45;0.5;0.95;1"
                dur="9s"
                repeatCount="indefinite"
              />
            </rect>
            <text className="mode-label" x={cx + 25 + (cw - 50) / 4} y={cy + 95} textAnchor="middle">companion</text>
            <text className="mode-label" x={cx + 25 + (3 * (cw - 50)) / 4} y={cy + 95} textAnchor="middle">yano-only</text>

            {/* Yano and cardano-node */}
            <g>
              <rect className="node-inner" x={cx + 22} y={nodeY} width="128" height="68" rx="14" stroke={`${AMBER}99`} />
              <text className="node-title sm" x={cx + 38} y={nodeY + 28}>Yano</text>
              <text className="node-sub" x={cx + 38} y={nodeY + 46}>fast bootstrap</text>
              <circle cx={cx + 132} cy={nodeY + 22} r="4" fill={AMBER}>
                <animate attributeName="opacity" values="1;0.3;1" dur="1.2s" repeatCount="indefinite" />
              </circle>
            </g>
            <g>
              <animate attributeName="opacity" values="1;1;0.28;0.28;1" keyTimes="0;0.45;0.5;0.95;1" dur="9s" repeatCount="indefinite" />
              <rect className="node-inner" x={cx + cw - 150} y={nodeY} width="128" height="68" rx="14" stroke={`${EMBER}99`} />
              <text className="node-title sm" x={cx + cw - 134} y={nodeY + 28}>cardano-node</text>
              <text className="node-sub" x={cx + cw - 134} y={nodeY + 46}>Haskell node</text>
              <path d={`M${cx + 150} ${nodeY + 34} L${cx + cw - 150} ${nodeY + 34}`} className="wire" />
              <path d={`M${cx + 150} ${nodeY + 34} L${cx + cw - 150} ${nodeY + 34}`} className="wire-flow" stroke={EMBER} />
              <text className="node-sub tiny" x={cx + cw / 2} y={nodeY - 8} textAnchor="middle">handover</text>
            </g>

            {/* Blocks being produced */}
            {Array.from({ length: 6 }).map((_, i) => {
              const bx = cx + 26 + i * 48;
              return (
                <g key={i}>
                  {i > 0 && <path d={`M${bx - 14} ${chainY + 17} L${bx} ${chainY + 17}`} className="link" />}
                  <rect x={bx} y={chainY} width="34" height="34" rx="9" className="blk" />
                  <rect x={bx} y={chainY} width="34" height="34" rx="9" fill="url(#dk-blk-fill)" opacity="0">
                    <animate
                      attributeName="opacity"
                      values="0;0;1;1;0"
                      keyTimes={`0;${(i / 7).toFixed(3)};${((i + 0.6) / 7).toFixed(3)};0.94;1`}
                      dur="6s"
                      repeatCount="indefinite"
                    />
                  </rect>
                </g>
              );
            })}
            <text className="node-sub" x={cx + 22} y={chainY + 56}>
              block time 1s · down to 100ms
            </text>

            <Chip x={cx + 22} y={cy + 300} w={139} label="PV11 protocol" c={AMBER} />
            <Chip x={cx + 169} y={cy + 300} w={139} label="20 funded accounts" c={MINT} />
            <Chip x={cx + 22} y={cy + 336} w={139} label="multi-node · forks" c={VIOLET} />
            <Chip x={cx + 169} y={cy + 336} w={139} label="catch-up after pauses" c={PINK} />
            <text className="node-sub faint" x={cx + cw / 2} y={cy + ch - 18} textAnchor="middle">
              Docker · npm · Homebrew · Zip
            </text>
          </g>

          {/* 04 · Tools */}
          {TOOLS.map((t, i) => {
            const y = toolY(i);
            return (
              <g key={t.name}>
                <rect className="node-bg" x={TOOL_X} y={y} width={TOOL_W} height={TOOL_H} rx="12" />
                <rect x={TOOL_X} y={y + 13} width="3" height="22" rx="1.5" fill={t.c} />
                <text className="node-title sm" x={TOOL_X + 18} y={y + 21}>
                  {t.name}
                </text>
                <text className="node-sub" x={TOOL_X + 18} y={y + 37}>
                  {t.sub}
                </text>
              </g>
            );
          })}
          <text className="node-sub faint" x={TOOL_X + TOOL_W / 2} y="426" textAnchor="middle">
            terminal · browser · desktop · AI agent
          </text>
        </svg>
      </div>
      <div className="dk-legend">
        <span style={{ "--c": CYAN }}>
          <i />
          Off-chain SDKs
        </span>
        <span style={{ "--c": VIOLET }}>
          <i />
          Optional Ogmios / Kupo
        </span>
        <span style={{ "--c": EMBER }}>
          <i />
          Devnet
        </span>
        <span style={{ "--c": PINK }}>
          <i />
          Tools &amp; AI agents
        </span>
      </div>
    </div>
  );
}
