import { useEffect, useState } from "react";
import { Check } from "./icons";
import { formatNumber, useInView, useInterval, usePrefersReducedMotion } from "./hooks";

/*
 * A Yano Wallet window connected to a DevKit devnet, modelled on the real app. A dApp asks it to sign a mint, the
 * wallet shows what the transaction does, the user approves, and it lands in recent activity. One step per tick,
 * LOOP ticks per cycle.
 */
const LOOP = 10;
const REDUCED_STEP = 3;

const phaseOf = (t) => {
  if (t < 2) return "idle";
  if (t < 3) return "request";
  if (t < 5) return "simulated";
  if (t < 6) return "signed";
  if (t < 8) return "pending";
  return "confirmed";
};

const NAV = ["Dashboard", "Send", "Receive", "History", "Staking", "Governance"];

export default function YanoWalletArt() {
  const [ref, inView] = useInView();
  const reduced = usePrefersReducedMotion();
  const [t, setT] = useState(0);
  const [block, setBlock] = useState(4120);

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
  const sheetOpen = ["request", "simulated", "signed"].includes(phase);
  const landed = phase === "pending" || phase === "confirmed";
  const balance = landed ? "999.81" : "1,000.00";

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
              <span className="pill net-pill">Yaci DevKit</span>
              <span className="pill sync-pill">synced · block {formatNumber(block)}</span>
            </div>
          </nav>

          <div className="main">
            <div className="acct">
              <span className="who">Account 0 · Test</span>
              <span className="hw">CIP-30 · window.cardano.yano</span>
            </div>
            <div className="bal">
              <span className="lbl">Total balance</span>
              <b key={balance} className="dk-fade-swap">
                {balance} <em>₳</em>
              </b>
              <span className="assets">
                <span>₳</span>
                <span>TEST ×250</span>
                {landed && <span className="new dk-fade-swap">YaciTestNFT ×1</span>}
              </span>
            </div>

            <div className="act">
              <div className="act-head">Recent activity</div>
              {landed && (
                <div className={`row fresh ${phase}`}>
                  <span className="kind">Mint · dApp</span>
                  <span className="hash">8c1f…a7e2 ↗</span>
                  <span className="amt neg">−0.19 ₳</span>
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
            <span className="what">Approve transaction</span>
            <span className="app">localhost:3000 asks you to sign a transaction</span>
          </div>
          <div className="sim">
            <div className="sim-row">
              <span>Leaves your wallet</span>
              <b>₳ 0.185565</b>
            </div>
            <div className="sim-row">
              <span>Network fee</span>
              <b>included</b>
            </div>
            <div className="sim-row">
              <span>Mints</span>
              <b className="pos">+1 YaciTestNFT</b>
            </div>
            <div className={`sim-warn ${phase === "request" ? "" : "on"}`}>Creates new tokens: worth questioning if you didn&apos;t ask to mint.</div>
          </div>
          <div className="sheet-actions">
            {phase === "signed" ? (
              <span className="sheet-btn ok">
                <Check /> Signed and submitted
              </span>
            ) : (
              <>
                <span className="sheet-btn">Approve</span>
                <span className="sheet-btn ghost">Reject</span>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
