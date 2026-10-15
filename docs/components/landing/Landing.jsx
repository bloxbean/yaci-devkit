import { useState } from "react";
import Link from "next/link";
import { Inter, JetBrains_Mono, Space_Grotesk } from "next/font/google";
import Architecture from "./Architecture";
import HeroConsole from "./HeroConsole";
import YanoWalletArt from "./YanoWalletArt";
import { BlockfrostTerm, BlockSpeed, CatchUpArt, ForkArt, McpChat, NodeModes, WalletArt } from "./FeatureArt";
import { useGithubStars, useReveal, useScrolled, useTheme } from "./hooks";
import {
  Archive,
  ArrowRight,
  Beer,
  Book,
  Box,
  Braces,
  Check,
  Clock,
  Copy,
  Discord,
  Flask,
  GitFork,
  GitHub,
  Globe,
  Mark,
  Moon,
  Package,
  Settings,
  Shuffle,
  Sparkles,
  Sun,
  Terminal,
  Wallet,
  XLogo,
  Zap,
} from "./icons";

const display = Space_Grotesk({ subsets: ["latin"], weight: ["500", "600", "700"], variable: "--font-dk-display", display: "swap" });
const body = Inter({ subsets: ["latin"], variable: "--font-dk-body", display: "swap" });
const mono = JetBrains_Mono({ subsets: ["latin"], weight: ["400", "500", "600", "800"], variable: "--font-dk-mono", display: "swap" });

const VERSION = "v0.12.0";
const GITHUB_REPO = "bloxbean/yaci-devkit";
const GITHUB_URL = `https://github.com/${GITHUB_REPO}`;
const RELEASE_URL = `${GITHUB_URL}/releases/tag/${VERSION}`;
const DISCORD_URL = "https://discord.gg/JtQ54MSw6p";
const X_URL = "https://x.com/bloxbean";
const YANO_WALLET_URL = "https://github.com/bloxbean/yano-wallet";

const formatStars = (n) => (n >= 1000 ? `${(n / 1000).toFixed(1).replace(/\.0$/, "")}k` : String(n));

/** Moves the radial spotlight on a card to follow the pointer. */
function spotlight(e) {
  const card = e.target.closest?.(".dk-card");
  if (!card) return;
  const r = card.getBoundingClientRect();
  card.style.setProperty("--mx", `${e.clientX - r.left}px`);
  card.style.setProperty("--my", `${e.clientY - r.top}px`);
}

function useCopy() {
  const [copied, setCopied] = useState(false);
  const copy = async (text) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 1600);
    } catch {}
  };
  return [copied, copy];
}

/* ------------------------------------------------------------------ */

function Brand({ size = 26 }) {
  return (
    <span className="dk-brand">
      <Mark size={size} />
      <span className="word">
        <b>Yaci</b>
        <i />
        DevKit
      </span>
    </span>
  );
}

function Nav({ theme, onToggleTheme, stars }) {
  const scrolled = useScrolled();
  return (
    <header className={`dk-nav${scrolled ? " scrolled" : ""}`}>
      <div className="dk-container dk-nav-inner">
        <Link href="/" aria-label="Yaci DevKit home">
          <Brand />
        </Link>
        <nav className="dk-nav-links" aria-label="Primary">
          <a href="#how-it-works">How it works</a>
          <a href="#features">Features</a>
          <a href="#yano-wallet">Yano Wallet</a>
          <a href="#get-started">Get started</a>
          <a href="#whats-new">What&apos;s new</a>
          <Link href="/introduction">Docs</Link>
        </nav>
        <div className="dk-nav-actions">
          <a className="dk-icon-btn" href={GITHUB_URL} target="_blank" rel="noopener noreferrer" aria-label="Yaci DevKit on GitHub">
            <GitHub />
            {stars !== null && <span className="count hide-sm">★ {formatStars(stars)}</span>}
          </a>
          <a className="dk-icon-btn hide-sm" href={DISCORD_URL} target="_blank" rel="noopener noreferrer" aria-label="Join the Discord">
            <Discord />
          </a>
          <button
            type="button"
            className="dk-icon-btn"
            onClick={onToggleTheme}
            aria-label={theme === "dark" ? "Switch to light theme" : "Switch to dark theme"}
          >
            {theme === "dark" ? <Sun /> : <Moon />}
          </button>
          <Link href="/introduction" className="dk-btn dk-btn-primary dk-btn-sm hide-xs">
            Docs
          </Link>
        </div>
      </div>
    </header>
  );
}

const QUICK = [
  { id: "brew", label: "Homebrew", cmd: "brew install bloxbean/tap/yaci-devkit" },
  { id: "npm", label: "npm", cmd: "npm install -g @bloxbean/yaci-devkit" },
  { id: "docker", label: "Docker", cmd: "curl -LsSf https://devkit.yaci.xyz/install.sh | bash" },
];

function QuickInstall() {
  const [active, setActive] = useState(0);
  const [copied, copy] = useCopy();
  const q = QUICK[active];
  return (
    <div className="dk-quick">
      <div className="dk-quick-tabs" role="tablist" aria-label="Install with">
        {QUICK.map((o, i) => (
          <button
            key={o.id}
            type="button"
            role="tab"
            aria-selected={i === active}
            className={i === active ? "active" : ""}
            onClick={() => setActive(i)}
          >
            {o.label}
          </button>
        ))}
      </div>
      <div className="dk-quick-cmd">
        <code key={q.id} className="dk-fade-swap">
          <span className="p">$</span> {q.cmd}
        </code>
        <button type="button" className="dk-copy" onClick={() => copy(q.cmd)} aria-label="Copy install command">
          {copied ? <Check /> : <Copy />}
        </button>
      </div>
    </div>
  );
}

function Hero() {
  return (
    <section className="dk-hero">
      <div className="dk-container dk-hero-grid">
        <div className="dk-hero-copy">
          <a href={RELEASE_URL} target="_blank" rel="noopener noreferrer" className="dk-announce dk-reveal">
            <span className="tag">{VERSION}</span>
            <span className="text">Now on Homebrew · devnets that survive a long pause</span>
            <ArrowRight width={15} height={15} />
          </a>
          <h1 className="dk-hero-title dk-reveal" style={{ "--delay": "0.05s" }}>
            Your own Cardano devnet,
            <br />
            <span className="dk-gradient-text">in seconds.</span>
          </h1>
          <p className="dk-hero-lead dk-reveal" style={{ "--delay": "0.12s" }}>
            Yaci DevKit runs a complete local Cardano network on your laptop or in CI: a block-producing node, a
            Blockfrost-compatible indexer, a CIP-30 wallet, an explorer and an MCP server for AI agents. One command
            to create it, with blocks as fast as every 100ms.
          </p>
          <div className="dk-hero-ctas dk-reveal" style={{ "--delay": "0.18s" }}>
            <a href="#get-started" className="dk-btn dk-btn-primary">
              Get started <ArrowRight />
            </a>
            <a href={GITHUB_URL} target="_blank" rel="noopener noreferrer" className="dk-btn dk-btn-ghost">
              <GitHub /> View on GitHub
            </a>
          </div>
          <div className="dk-reveal" style={{ "--delay": "0.22s" }}>
            <QuickInstall />
          </div>
          <div className="dk-hero-meta dk-reveal" style={{ "--delay": "0.26s" }}>
            <span>
              <Globe /> Linux · macOS · Docker
            </span>
            <span>
              <Zap /> PV11 ready
            </span>
            <span>
              <Check /> MIT licensed
            </span>
          </div>
        </div>
        <div className="dk-reveal" style={{ "--delay": "0.15s" }}>
          <HeroConsole />
        </div>
      </div>
    </section>
  );
}

/* ------------------------------------------------------------------ */

const STACK = [
  { name: "MeshJS", mark: "M", colors: ["#0891b2", "#22d3ee"] },
  { name: "Evolution SDK", mark: "Ev", colors: ["#ff4e2d", "#ff9a3d"] },
  { name: "Lucid Evolution", mark: "L", colors: ["#7c3aed", "#a78bfa"] },
  { name: "Cardano Client Lib", mark: "CL", colors: ["#1d4ed8", "#3b8bff"] },
  { name: "PyCardano", mark: "Py", colors: ["#059669", "#34d399"] },
  { name: "Sutra", mark: "S", colors: ["#db2777", "#f472b6"] },
  { name: "Blockfrost clients", mark: "BF", colors: ["#0f766e", "#2dd4bf"] },
  { name: "Ogmios & Kupo clients", mark: "OK", colors: ["#6d28d9", "#c084fc"] },
];

function Stack() {
  const loop = [...STACK, ...STACK];
  return (
    <section className="dk-stack" aria-label="Works with your SDK">
      <div className="dk-container">
        <p className="dk-stack-label dk-reveal">Works with the SDKs you already use</p>
      </div>
      <div className="dk-marquee dk-reveal">
        <div className="dk-marquee-track">
          {loop.map((s, i) => (
            <span key={i} className="dk-marquee-item" aria-hidden={i >= STACK.length}>
              <span className="dk-mono-mark" style={{ "--c1": s.colors[0], "--c2": s.colors[1] }} aria-hidden="true">
                {s.mark}
              </span>
              {s.name}
            </span>
          ))}
        </div>
      </div>
    </section>
  );
}

function SectionHead({ eyebrow, title, children }) {
  return (
    <div className="dk-section-head dk-reveal">
      <div className="dk-eyebrow">{eyebrow}</div>
      <h2 className="dk-h2">{title}</h2>
      {children && <p className="dk-sub">{children}</p>}
    </div>
  );
}

function HowItWorks() {
  return (
    <section id="how-it-works" className="dk-section">
      <div className="dk-container">
        <SectionHead
          eyebrow="How it works"
          title={
            <>
              Your whole stack, <span className="dk-gradient-text">on localhost</span>
            </>
          }
        >
          Your off-chain code talks to the same APIs it uses on mainnet. DevKit runs the chain underneath, indexes
          every block, and gives you a CLI, an admin API, wallets and an explorer to drive it.
        </SectionHead>
        <div className="dk-arch-card dk-reveal">
          <Architecture />
        </div>
      </div>
    </section>
  );
}

function FeatureCard({ span, accent, icon, badge, title, text, link, children, delay }) {
  return (
    <div className={`dk-card span-${span} dk-reveal`} style={{ "--accent": accent, "--delay": delay }}>
      <div className="dk-card-body">
        <div className="dk-card-top">
          <span className="dk-card-icon">{icon}</span>
          {badge && <span className="dk-badge">{badge}</span>}
        </div>
        <h3 className="dk-card-title">{title}</h3>
        <p className="dk-card-text">{text}</p>
        {link && (
          <Link href={link.href} className="dk-card-link">
            {link.label} <ArrowRight />
          </Link>
        )}
      </div>
      <div className="dk-card-art">{children}</div>
    </div>
  );
}

/* ------------------------------------------------------------------ */

const YW_POINTS = [
  "Create or restore wallets, with multiple accounts",
  "A CIP-30 browser connector: window.cardano.yano",
  "Simulate before you sign: see what a transaction does first",
  "Native assets, staking and governance votes",
];

/* Step 1 depends on how DevKit was installed. Yaci Viewer only ships in Docker; elsewhere it's an optional npm package. */
const YW_START = [
  { id: "native", label: "Homebrew · npm", lines: [["$", "yaci-devkit up --enable-yaci-store"]] },
  { id: "zip", label: "Zip", lines: [["$", "./yaci-cli up --enable-yaci-store"]] },
  {
    id: "docker",
    label: "Docker",
    lines: [
      ["$", "devkit start"],
      ["yaci-cli:>", "create-node -o --start"],
    ],
    docker: true,
  },
];

const YW_STEPS = [
  { cmd: "brew install --cask bloxbean/tap/yano-wallet", note: "macOS · other platforms on GitHub" },
  { cmd: "Connect → Yaci DevKit → localhost:8080", note: "then topup your address", plain: true },
  { cmd: "Settings → Browser connector", note: "for dApps: load the connector extension, then install Native Messaging", plain: true },
];

function YanoWallet() {
  const [copiedAt, setCopiedAt] = useState(-1);
  const [startAt, setStartAt] = useState(0);
  const start = YW_START[startAt];
  const copy = async (i, text) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopiedAt(i);
      setTimeout(() => setCopiedAt(-1), 1600);
    } catch {}
  };

  return (
    <section id="yano-wallet" className="dk-section">
      <div className="dk-container">
        <div className="dk-yw dk-reveal">
          <div className="dk-yw-copy">
            <div className="dk-yw-kicker">
              <span className="tag">Preview</span>
              Works with Yano Wallet
            </div>
            <h2 className="dk-h2">
              Sign with a <span className="dk-gradient-text">real wallet</span>
            </h2>
            <p className="dk-sub">
              Yano Wallet is a desktop Cardano wallet designed for your custom devnet. Pick <b>Yaci DevKit</b> on its
              Connect screen and it runs on your devnet&apos;s Yaci Store, so you can approve your dApp&apos;s
              transactions in a real wallet before you ship.
            </p>
            <ul className="dk-yw-points">
              {YW_POINTS.map((p) => (
                <li key={p}>
                  <Check /> {p}
                </li>
              ))}
            </ul>
            <ol className="dk-yw-steps">
              <li className="start">
                <span className="n">1</span>
                <span className="cmd">
                  <span className="dk-yw-switch" role="group" aria-label="How you installed Yaci DevKit">
                    {YW_START.map((o, i) => (
                      <button key={o.id} type="button" aria-pressed={i === startAt} onClick={() => setStartAt(i)}>
                        {o.label}
                      </button>
                    ))}
                  </span>
                  <code key={start.id} className="dk-fade-swap">
                    {start.lines.map(([prompt, line]) => (
                      <span key={line} className="line">
                        <span className="p">{prompt} </span>
                        {line}
                      </span>
                    ))}
                  </code>
                  <span className="note">
                    {start.docker ? "start a devnet · Yaci Store and Yaci Viewer are included" : "start a devnet with Yaci Store"}
                  </span>
                  {!start.docker && (
                    <span className="note">
                      Viewer (optional): <b>npm i -g @bloxbean/yaci-viewer</b>
                    </span>
                  )}
                </span>
                <button
                  type="button"
                  className="dk-copy"
                  onClick={() => copy(0, start.lines.filter(([prompt]) => prompt === "$").map(([, line]) => line).join("\n"))}
                  aria-label={`Copy: ${start.lines[0][1]}`}
                >
                  {copiedAt === 0 ? <Check /> : <Copy />}
                </button>
              </li>
              {YW_STEPS.map((st, i) => (
                <li key={st.cmd}>
                  <span className="n">{i + 2}</span>
                  <span className="cmd">
                    <code>
                      {!st.plain && <span className="p">$ </span>}
                      {st.cmd}
                    </code>
                    <span className="note">{st.note}</span>
                  </span>
                  {!st.plain && (
                    <button type="button" className="dk-copy" onClick={() => copy(i + 1, st.cmd)} aria-label={`Copy: ${st.cmd}`}>
                      {copiedAt === i + 1 ? <Check /> : <Copy />}
                    </button>
                  )}
                </li>
              ))}
            </ol>
            <div className="dk-yw-cta">
              <Link href="/yano-wallet" className="dk-btn dk-btn-primary">
                Set it up <ArrowRight />
              </Link>
              <a href={YANO_WALLET_URL} target="_blank" rel="noopener noreferrer" className="dk-btn dk-btn-ghost">
                <GitHub /> yano-wallet
              </a>
            </div>
            <p className="dk-yw-note">Preview release, for local devnets and test networks only.</p>
          </div>
          <YanoWalletArt />
        </div>
      </div>
    </section>
  );
}

function Features() {
  return (
    <section id="features" className="dk-section">
      <div className="dk-container">
        <SectionHead
          eyebrow="Features"
          title={
            <>
              Built for how you <span className="dk-gradient-text">build and test</span>
            </>
          }
        >
          From a quick script to a full end-to-end suite, DevKit gives you a chain you control: reset it, speed it
          up, fork it, fund it, and let your tools and agents drive it.
        </SectionHead>

        <div className="dk-bento" onPointerMove={spotlight}>
          <FeatureCard
            span={4}
            accent="var(--ember)"
            icon={<Zap />}
            title="A devnet at any speed"
            text="Create a fresh chain in seconds and choose the block time: 1 second by default, or down to 100ms for fast test suites. Set epoch length, genesis and protocol parameters to match your scenario."
            link={{ href: "/node-configuration", label: "Node configuration" }}
          >
            <BlockSpeed />
          </FeatureCard>

          <FeatureCard
            span={2}
            accent="var(--amber)"
            icon={<Shuffle />}
            badge="Yano"
            title="Node modes"
            text="Run a real Haskell node, or a lightweight Yano-only devnet."
            link={{ href: "/yano-node-modes", label: "Node modes" }}
            delay="0.06s"
          >
            <NodeModes />
          </FeatureCard>

          <FeatureCard
            span={3}
            accent="var(--cyan)"
            icon={<Braces />}
            title="Blockfrost-compatible APIs"
            text="Yaci Store indexes every block and serves Blockfrost-compatible endpoints, so your SDK code just points at localhost. Script costs are evaluated with Scalus, so Ogmios is optional."
            link={{ href: "/services", label: "Services" }}
          >
            <BlockfrostTerm />
          </FeatureCard>

          <FeatureCard
            span={3}
            accent="var(--pink)"
            icon={<Sparkles />}
            badge="MCP"
            title="Ready for AI agents"
            text="The built-in MCP server lets coding agents check the devnet, fund addresses, read UTxOs, submit transactions and reset the chain."
            link={{ href: "/mcp-integration", label: "Connect your agent" }}
            delay="0.06s"
          >
            <McpChat />
          </FeatureCard>

          <FeatureCard
            span={2}
            accent="var(--mint)"
            icon={<Wallet />}
            title="CIP-30 browser wallet"
            text="Test dApps end to end with a built-in wallet, a wallet page and 20 pre-funded accounts."
            link={{ href: "/cip30-wallet", label: "CIP-30 wallet" }}
          >
            <WalletArt />
          </FeatureCard>

          <FeatureCard
            span={2}
            accent="var(--violet)"
            icon={<GitFork />}
            title="Rollback testing"
            text="Split a multi-node devnet into forks, then join them and watch your app handle a real rollback."
            link={{ href: "/rollback-testing", label: "Rollback testing" }}
            delay="0.06s"
          >
            <ForkArt />
          </FeatureCard>

          <FeatureCard
            span={2}
            accent="var(--blue)"
            icon={<Clock />}
            badge={VERSION}
            title="Pick up where you left off"
            text="Stopped for the weekend? DevKit catches the chain up to the current time with Yano and keeps the same devnet."
            link={{ href: "/yano-node-modes#resuming-after-a-long-pause", label: "Resuming a devnet" }}
            delay="0.12s"
          >
            <CatchUpArt />
          </FeatureCard>
        </div>
      </div>
    </section>
  );
}

/* ------------------------------------------------------------------ */

const RUN_OPTIONS = [
  {
    id: "brew",
    icon: <Beer />,
    title: "Homebrew",
    tag: "New",
    desc: "The native CLI on macOS 15+ and Linux. No Node.js or Docker needed.",
    code: [
      ["c", "# Install"],
      ["p", "brew install bloxbean/tap/yaci-devkit"],
      ["", ""],
      ["c", "# Start a devnet with Yaci Store"],
      ["p", "yaci-devkit up --enable-yaci-store"],
      ["", ""],
      ["c", "# Upgrade later"],
      ["p", "brew upgrade yaci-devkit"],
    ],
    points: ["Native binary", "brew upgrade", "Settings in devkit.properties"],
    href: "/getting-started/homebrew",
  },
  {
    id: "npm",
    icon: <Package />,
    title: "npm",
    tag: "CI/CD",
    desc: "The same CLI as an npm package, ideal for GitHub Actions and test pipelines.",
    code: [
      ["p", "npm install -g @bloxbean/yaci-devkit"],
      ["p", "yaci-devkit up --enable-yaci-store"],
      ["", ""],
      ["c", "# Optional: the Yaci Viewer explorer"],
      ["p", "npm install -g @bloxbean/yaci-viewer"],
      ["p", "yaci-viewer"],
    ],
    points: ["Node.js 20.8+", "Runs in CI", "Yaci Viewer package"],
    href: "/getting-started/npm",
  },
  {
    id: "docker",
    icon: <Box />,
    title: "Docker",
    tag: "All-in-one",
    desc: "The whole stack in containers, including Yaci Viewer, Ogmios and Kupo.",
    code: [
      ["p", "curl --proto '=https' --tlsv1.2 -LsSf https://devkit.yaci.xyz/install.sh | bash"],
      ["", ""],
      ["c", "# Start the containers and the Yaci CLI"],
      ["p", "devkit start"],
      ["", ""],
      ["c", "# In the Yaci CLI"],
      ["y", "yaci-cli:> create-node -o --start"],
    ],
    points: ["Viewer included", "amd64 & arm64 images", "Ogmios & Kupo built in"],
    href: "/getting-started/docker",
  },
  {
    id: "zip",
    icon: <Archive />,
    title: "Zip",
    desc: "The native binary and its config. Download only the components you need.",
    code: [
      ["c", "# yaci-cli-<version>-<os>-<arch>.zip from GitHub Releases"],
      ["p", "./yaci-cli"],
      ["", ""],
      ["y", "yaci-cli:> download"],
      ["y", "yaci-cli:> create-node -o --start"],
    ],
    points: ["No containers", "Pick component versions", "Multi-node setups"],
    href: "/getting-started/zip",
  },
];

function RunItYourWay() {
  const [active, setActive] = useState(0);
  const [copied, copy] = useCopy();
  const opt = RUN_OPTIONS[active];

  const copyCode = () =>
    copy(
      opt.code
        .filter(([cls]) => cls === "p")
        .map(([, line]) => line)
        .join("\n")
    );

  return (
    <section id="get-started" className="dk-section">
      <div className="dk-container">
        <SectionHead
          eyebrow="Get started"
          title={
            <>
              Install it <span className="dk-gradient-text">your way</span>
            </>
          }
        >
          Four distributions, one CLI. Pick the one that fits your machine or your pipeline. Homebrew, npm and Zip
          share <code className="dk-inline-code">~/.yaci-cli</code>; Docker keeps its data in a Docker volume.
        </SectionHead>

        <div className="dk-run dk-reveal">
          <div className="dk-run-tabs" role="tablist" aria-label="Distribution">
            {RUN_OPTIONS.map((o, i) => (
              <button
                key={o.id}
                type="button"
                role="tab"
                id={`dk-run-tab-${o.id}`}
                aria-selected={i === active}
                aria-controls="dk-run-panel"
                className={`dk-run-tab${i === active ? " active" : ""}`}
                onClick={() => setActive(i)}
              >
                <span className="ic">{o.icon}</span>
                <span>
                  <span className="t">
                    {o.title}
                    {o.tag && <em>{o.tag}</em>}
                  </span>
                  <span className="d">{o.desc}</span>
                </span>
              </button>
            ))}
          </div>

          <div className="dk-run-panel" id="dk-run-panel" role="tabpanel" aria-labelledby={`dk-run-tab-${opt.id}`}>
            <div className="dk-term-bar">
              <div className="dk-dots" aria-hidden="true">
                <i />
                <i />
                <i />
              </div>
              terminal
              <button type="button" className="dk-copy-btn" onClick={copyCode}>
                {copied ? <Check /> : <Copy />}
                {copied ? "Copied" : "Copy"}
              </button>
            </div>
            <pre className="dk-run-code dk-fade-swap" key={opt.id}>
              {opt.code.map(([cls, line], i) => (
                <div key={i}>
                  {cls === "p" && <span className="p">$ </span>}
                  <span className={cls === "p" ? "" : cls}>{line || " "}</span>
                </div>
              ))}
            </pre>
            <div className="dk-run-foot">
              <ul>
                {opt.points.map((p) => (
                  <li key={p}>
                    <Check />
                    {p}
                  </li>
                ))}
              </ul>
              <Link href={opt.href} className="dk-btn dk-btn-ghost dk-btn-sm">
                Setup guide <ArrowRight />
              </Link>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

/* ------------------------------------------------------------------ */

const NEWS = [
  { icon: <Beer />, c: "var(--ember)", title: "Homebrew formula", text: "brew install bloxbean/tap/yaci-devkit on macOS 15+ and Linux.", href: "/getting-started/homebrew" },
  { icon: <Clock />, c: "var(--blue)", title: "Devnets that survive a pause", text: "Catch up to the current time with Yano on start, and every 30s while running.", href: "/yano-node-modes#resuming-after-a-long-pause" },
  { icon: <Shuffle />, c: "var(--amber)", title: "Yano node modes", text: "Companion mode for a fast PV11 bootstrap, or yano-only for the lightest devnet.", href: "/yano-node-modes" },
  { icon: <Wallet />, c: "var(--mint)", title: "CIP-30 wallet", text: "A browser wallet, wallet page and SDK for testing dApps against the devnet.", href: "/cip30-wallet" },
  { icon: <Sparkles />, c: "var(--pink)", title: "MCP server", text: "Let AI coding agents inspect and drive the devnet at :10000/mcp.", href: "/mcp-integration" },
  { icon: <Settings />, c: "var(--violet)", title: "devkit.properties", text: "Your own settings in ~/.yaci-cli, kept across upgrades.", href: "/user-settings" },
  { icon: <Flask />, c: "var(--ember)", title: "SDK test suites", text: "Evolution SDK, Lucid, MeshJS, PyCardano and CCL checked end to end.", href: GITHUB_URL + "/tree/main/e2e-tests" },
];

function WhatsNew() {
  return (
    <section id="whats-new" className="dk-section">
      <div className="dk-container">
        <SectionHead
          eyebrow={`What's new in ${VERSION}`}
          title={
            <>
              Fresh in the <span className="dk-gradient-text">latest release</span>
            </>
          }
        >
          Yano-powered node modes, a browser wallet, an MCP server, Homebrew and devnets you can leave for the weekend.
        </SectionHead>
        <div className="dk-news">
          {NEWS.map((n, i) => {
            const external = n.href.startsWith("http");
            const inner = (
              <>
                <span className="ic">{n.icon}</span>
                <span className="t">{n.title}</span>
                <span className="d">{n.text}</span>
              </>
            );
            return external ? (
              <a key={n.title} href={n.href} target="_blank" rel="noopener noreferrer" className="dk-news-item dk-reveal" style={{ "--c": n.c, "--delay": `${(i % 4) * 0.05}s` }}>
                {inner}
              </a>
            ) : (
              <Link key={n.title} href={n.href} className="dk-news-item dk-reveal" style={{ "--c": n.c, "--delay": `${(i % 4) * 0.05}s` }}>
                {inner}
              </Link>
            );
          })}
        </div>
        <div className="dk-news-foot dk-reveal">
          <a href={RELEASE_URL} target="_blank" rel="noopener noreferrer">
            Read the {VERSION} release notes <ArrowRight />
          </a>
        </div>
      </div>
    </section>
  );
}

function FinalCta() {
  return (
    <section className="dk-section" style={{ paddingBottom: 0 }}>
      <div className="dk-container">
        <div className="dk-cta dk-reveal">
          <div className="dk-cta-mark" aria-hidden="true">
            <Mark size={56} />
          </div>
          <h2 className="dk-h2">
            Start your devnet <span className="dk-gradient-text">now</span>
          </h2>
          <p className="dk-sub">Open source and MIT licensed. Install, run one command, and submit your first transaction in minutes.</p>
          <div className="dk-cta-term">
            <span className="p">$</span> yaci-devkit up --enable-yaci-store
          </div>
          <div className="dk-cta-buttons">
            <a href="#get-started" className="dk-btn dk-btn-primary">
              Get started <ArrowRight />
            </a>
            <Link href="/introduction" className="dk-btn dk-btn-ghost">
              <Book /> Read the docs
            </Link>
          </div>
          <div className="dk-community">
            <a href={GITHUB_URL} target="_blank" rel="noopener noreferrer">
              <GitHub /> GitHub
            </a>
            <a href={DISCORD_URL} target="_blank" rel="noopener noreferrer">
              <Discord /> Discord
            </a>
            <a href={X_URL} target="_blank" rel="noopener noreferrer">
              <XLogo /> @bloxbean
            </a>
            <a href={`${GITHUB_URL}/discussions`} target="_blank" rel="noopener noreferrer">
              <Terminal /> Discussions
            </a>
          </div>
        </div>
      </div>
    </section>
  );
}

function Footer() {
  return (
    <footer className="dk-footer">
      <div className="dk-container dk-footer-inner">
        <Brand size={20} />
        <span>© {new Date().getFullYear()} BloxBean · MIT License</span>
        <nav aria-label="Footer">
          <Link href="/introduction">Docs</Link>
          <Link href="/commands">CLI commands</Link>
          <a href={`${GITHUB_URL}/releases`} target="_blank" rel="noopener noreferrer">
            Releases
          </a>
          <a href="https://store.yaci.xyz" target="_blank" rel="noopener noreferrer">
            Yaci Store
          </a>
          <a href={GITHUB_URL} target="_blank" rel="noopener noreferrer">
            GitHub
          </a>
        </nav>
      </div>
    </footer>
  );
}

export default function Landing() {
  const [theme, toggleTheme] = useTheme();
  const stars = useGithubStars(GITHUB_REPO);
  useReveal();

  return (
    <div className={`dk ${display.variable} ${body.variable} ${mono.variable}`}>
      <div className="dk-ambient" aria-hidden="true">
        <div className="grid" />
        <div className="blob b1" />
        <div className="blob b2" />
        <div className="blob b3" />
      </div>
      <Nav theme={theme} onToggleTheme={toggleTheme} stars={stars} />
      <main className="dk-main">
        <Hero />
        <Stack />
        <HowItWorks />
        <YanoWallet />
        <Features />
        <RunItYourWay />
        <WhatsNew />
        <FinalCta />
      </main>
      <Footer />
    </div>
  );
}
