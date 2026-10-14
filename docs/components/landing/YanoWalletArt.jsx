import { useEffect, useState } from "react";
import { Check } from "./icons";
import { formatNumber, useInView, useInterval, usePrefersReducedMotion } from "./hooks";

/*
 * A Yano Wallet window connected to a DevKit devnet. A dApp asks it to sign a mint, the wallet shows the simulated
 * effect, the Ledger confirms, and the transaction lands in the activity list. One step per tick, LOOP ticks per cycle.
 */
const LOOP = 11;
const REDUCED_STEP = 4;

const phaseOf = (t) => {
  if (t < 2) return "idle";
  if (t < 3) return "request";
  if (t < 5) return "simulated";
  if (t < 6) return "ledger";
  if (t < 7) return "signed";
  if (t < 9) return "pending";
  return "confirmed";
};

const NAV = ["Dashboard", "Send", "Receive", "Staking", "Governance", "dApps"];

function LedgerGlyph() {
  return (
    <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <rect x="3" y="7" width="18" height="10" rx="2" />
      <path d="M7 12h6" />
      <circle cx="17" cy="12" r="1.4" fill="currentColor" stroke="none" />
    </svg>
  );
}

export default function YanoWalletArt() {
  const [ref, inView] = useInView();
  const reduced = usePrefersReducedMotion();
  const [t, setT] = useState(0);
  const [block, setBlock] = useState(18342);

  useEffect(() => {
    if (reduced) setT(REDUCED_STEP);
  }, [reduced]);
  useInterval(
    () => {
      setT((x) => (x + 1) % LOOP);
      setBlock((b) => b + 1);
    },
    1000,
    inView && !reduced,
  );

  const phase = phaseOf(t);
  const sheetOpen = ["request", "simulated", "ledger", "signed"].includes(phase);
  const landed = phase === "pending" || phase === "confirmed";
  const balance = landed ? "994.82" : "1,000.00";

  return (
    <div ref={ref} className="dk-yw-art" aria-hidden="true">
      <div className="dk-yw-win">
        <div className="bar">
          <span className="lights">
            <i />
            <i />
            <i />
          </span>
          <span className="name">Yano Wallet</span>
          <span className="net">
            <span className="dot" />
            Yaci DevKit<span className="port"> · :8080</span>
          </span>
        </div>

        <div className="body">
          <nav className="side">
            {NAV.map((n, i) => (
              <span key={n} className={i === 0 ? "on" : ""}>
                {n}
              </span>
            ))}
            <div className="sync">
              <span className="lbl">Yaci Store tip</span>
              <span className="val">
                <span className="dot" /> #{formatNumber(block)}
              </span>
            </div>
          </nav>

          <div className="main">
            <div className="acct">
              <span className="who">Account 0</span>
              <span className="hw">
                <LedgerGlyph /> Ledger
              </span>
            </div>
            <div className="bal">
              <b key={balance} className="dk-fade-swap">
                {balance} <em>₳</em>
              </b>
              <span className="assets">
                <span>₳</span>
                <span>TEST ×250</span>
                {landed && <span className="new dk-fade-swap">DevPass ×1</span>}
              </span>
            </div>

            <div className="act">
              <div className="act-head">Activity</div>
              {landed && (
                <div className={`row fresh ${phase}`}>
                  <span className="kind">Mint · dApp</span>
                  <span className="hash">8c1f…a7e2 ↗</span>
                  <span className="amt neg">−5.18 ₳</span>
                  <span className={`st ${phase}`}>{phase === "pending" ? "pending" : "confirmed"}</span>
                </div>
              )}
              <div className="row">
                <span className="kind">Top-up · faucet</span>
                <span className="hash">41d9…0b3c ↗</span>
                <span className="amt pos">+1,000 ₳</span>
                <span className="st confirmed">confirmed</span>
              </div>
              <div className="row dim">
                <span className="kind">Receive</span>
                <span className="hash">e07a…5d11 ↗</span>
                <span className="amt pos">+250 TEST</span>
                <span className="st confirmed">confirmed</span>
              </div>
            </div>
          </div>
        </div>

        <div className={`sheet ${sheetOpen ? "open" : ""} ${phase}`}>
          <div className="sheet-top">
            <span className="app">localhost:3000</span>
            <span className="what">wants you to sign</span>
          </div>
          <div className="sim">
            <div className="sim-row">
              <span>You send</span>
              <b>−5.00 ₳</b>
            </div>
            <div className="sim-row">
              <span>You receive</span>
              <b className="pos">+1 DevPass</b>
            </div>
            <div className="sim-row">
              <span>Network fee</span>
              <b>0.18 ₳</b>
            </div>
            <div className={`sim-ok ${phase === "request" ? "" : "on"}`}>
              <Check /> Simulated · script evaluated by Yaci Store
            </div>
          </div>
          <div className="sheet-btn">
            {(phase === "request" || phase === "simulated") && <span>Approve</span>}
            {phase === "ledger" && (
              <span className="ledger">
                <LedgerGlyph /> Confirm on your Ledger
                <span className="dk-typing">
                  <i />
                  <i />
                  <i />
                </span>
              </span>
            )}
            {phase === "signed" && (
              <span className="ok">
                <Check /> Signed and submitted
              </span>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
