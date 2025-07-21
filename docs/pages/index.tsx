import React from 'react'
import Head from 'next/head'
import Link from 'next/link'
import { useState } from 'react'

const HomePage = () => {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)

  return (
    <>
      <Head>
        <title>Yaci DevKit - Complete Cardano Development Environment</title>
        <meta name="description" content="Launch, customize, and manage your own Cardano blockchain in seconds. Build DApps, test smart contracts, and integrate with popular SDKs using a lightning-fast local devnet." />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
      </Head>

      <div className="landing-page">
        {/* Header */}
        <header className="header">
          <nav className="navbar">
            <div className="nav-brand">
              <span className="brand-text">Yaci DevKit</span>
            </div>
            <div className="nav-links">
              <Link href="/getting-started/docker" className="nav-link">Get Started</Link>
              <Link href="/introduction" className="nav-link">Documentation</Link>
              <a href="https://github.com/bloxbean/yaci-devkit" className="nav-link" target="_blank" rel="noopener noreferrer">GitHub</a>
              <a href="https://discord.gg/JtQ54MSw6p" className="nav-link" target="_blank" rel="noopener noreferrer">Discord</a>
            </div>
            <button
              className="mobile-menu-btn"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            >
              ☰
            </button>
          </nav>
          {mobileMenuOpen && (
            <div className="mobile-menu">
              <Link href="/getting-started/docker" className="mobile-nav-link">Get Started</Link>
              <Link href="/introduction" className="mobile-nav-link">Documentation</Link>
              <a href="https://github.com/bloxbean/yaci-devkit" className="mobile-nav-link">GitHub</a>
              <a href="https://discord.gg/JtQ54MSw6p" className="mobile-nav-link">Discord</a>
            </div>
          )}
        </header>

        {/* Hero Section */}
        <section className="hero">
          <div className="hero-container">
            <div className="hero-content">
              <div className="hero-text">
                <h1 className="hero-title">Yaci DevKit</h1>
                <p className="hero-subtitle">
                  Complete Cardano development environment with instant local devnet
                </p>
                <p className="hero-description">
                  Launch, customize, and manage your own Cardano blockchain in seconds.
                  Build DApps, test smart contracts, and integrate with popular SDKs using a lightning-fast local devnet.
                </p>

                <div className="hero-buttons">
                  <Link href="/getting-started/docker" className="btn-primary">
                    Get Started
                  </Link>
                  <Link href="/introduction" className="btn-secondary">
                    📖 Documentation
                  </Link>
                </div>

                <div className="hero-stats">
                  <div className="stat">
                    <div className="stat-icon">⚡</div>
                    <div className="stat-label">Instant Setup</div>
                  </div>
                  <div className="stat">
                    <div className="stat-icon">📊</div>
                    <div className="stat-label">Built-in Indexer</div>
                  </div>
                  <div className="stat">
                    <div className="stat-icon">🔄</div>
                    <div className="stat-label">Rollback Testing</div>
                  </div>
                </div>
              </div>

              <div className="hero-illustration">
                <svg width="500" height="400" viewBox="0 0 500 400" className="illustration-svg">
                  {/* Development workflow illustration */}
                  <defs>
                    <linearGradient id="cardanoGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                      <stop offset="0%" stopColor="#0033AD" stopOpacity="0.9"/>
                      <stop offset="100%" stopColor="#1e3a8a" stopOpacity="0.9"/>
                    </linearGradient>
                    <linearGradient id="devkitGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                      <stop offset="0%" stopColor="#667eea" stopOpacity="0.9"/>
                      <stop offset="100%" stopColor="#764ba2" stopOpacity="0.9"/>
                    </linearGradient>
                    <linearGradient id="toolGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                      <stop offset="0%" stopColor="#4facfe" stopOpacity="0.8"/>
                      <stop offset="100%" stopColor="#00f2fe" stopOpacity="0.8"/>
                    </linearGradient>
                    <linearGradient id="sdkGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                      <stop offset="0%" stopColor="#a8edea" stopOpacity="0.8"/>
                      <stop offset="100%" stopColor="#fed6e3" stopOpacity="0.8"/>
                    </linearGradient>
                    <filter id="glow">
                      <feGaussianBlur stdDeviation="2" result="coloredBlur"/>
                      <feMerge>
                        <feMergeNode in="coloredBlur"/>
                        <feMergeNode in="SourceGraphic"/>
                      </feMerge>
                    </filter>
                  </defs>

                  {/* Background pattern */}
                  <circle cx="80" cy="80" r="20" fill="rgba(255,255,255,0.1)" opacity="0.3"/>
                  <circle cx="420" cy="340" r="25" fill="rgba(255,255,255,0.1)" opacity="0.2"/>
                  <circle cx="450" cy="100" r="15" fill="rgba(255,255,255,0.1)" opacity="0.4"/>

                  {/* Developer at top */}
                  <g className="developer">
                    <circle cx="180" cy="40" r="18" fill="#4ade80" opacity="0.9"/>
                    <text x="180" y="46" textAnchor="middle" fill="white" fontSize="14">👨‍💻</text>
                    <text x="180" y="25" textAnchor="middle" fill="rgba(255,255,255,0.9)" fontSize="8">Developer</text>
                  </g>

                  {/* Central Cardano Node */}
                  <g className="cardano-node">
                    <circle cx="250" cy="130" r="35" fill="url(#cardanoGradient)" filter="url(#glow)" className="node-pulse"/>
                    <text x="250" y="125" textAnchor="middle" fill="white" fontSize="11" fontWeight="bold">Cardano</text>
                    <text x="250" y="138" textAnchor="middle" fill="white" fontSize="11" fontWeight="bold">Node</text>
                    {/*<text x="250" y="148" textAnchor="middle" fill="rgba(255,255,255,0.8)" fontSize="7">Local Devnet</text>*/}
                  </g>

                  {/* DevKit CLI - Developer interacts directly */}
                  <g className="devkit-cli">
                    <rect x="280" y="25" width="140" height="30" rx="8" fill="url(#devkitGradient)" filter="url(#glow)" className="devkit-cli"/>
                    <text x="350" y="43" textAnchor="middle" fill="white" fontSize="10" fontWeight="bold">Yaci DevKit CLI</text>
                    <text x="350" y="53" textAnchor="middle" fill="rgba(255,255,255,0.8)" fontSize="6">create-node, tip, etc.</text>
                  </g>

                  {/* Connection from Developer to CLI */}
                  <line x1="198" y1="40" x2="280" y2="40" stroke="rgba(255,255,255,0.6)" strokeWidth="2" strokeDasharray="4,4" className="connection-pulse"/>
                  <polygon points="275,35 285,40 275,45" fill="rgba(255,255,255,0.6)"/>

                  {/* Connection from CLI to Node */}
                  <line x1="350" y1="55" x2="275" y2="105" stroke="rgba(255,255,255,0.6)" strokeWidth="2" strokeDasharray="4,4" className="connection-pulse"/>
                  <polygon points="280,100 275,110 270,100" fill="rgba(255,255,255,0.6)"/>

                  {/* Services Layer */}
                  <g className="services">
                    {/* Yaci Store */}
                    <rect x="90" y="210" width="80" height="35" rx="8" fill="url(#toolGradient)" className="service-float"/>
                    <text x="130" y="225" textAnchor="middle" fill="white" fontSize="8" fontWeight="bold">📊 Yaci Store</text>
                    <text x="130" y="235" textAnchor="middle" fill="white" fontSize="6">Blockfrost API</text>
                    <text x="130" y="242" textAnchor="middle" fill="rgba(255,255,255,0.8)" fontSize="5">:8080</text>

                    {/* Ogmios - same level */}
                    <rect x="210" y="210" width="80" height="35" rx="8" fill="url(#toolGradient)" className="service-float" opacity="0.8"/>
                    <text x="250" y="225" textAnchor="middle" fill="white" fontSize="8" fontWeight="bold">🔌 Ogmios</text>
                    <text x="250" y="235" textAnchor="middle" fill="white" fontSize="6">Optional</text>
                    <text x="250" y="242" textAnchor="middle" fill="rgba(255,255,255,0.8)" fontSize="5">:1337</text>

                    {/* Kupo - same level */}
                    <rect x="330" y="210" width="80" height="35" rx="8" fill="url(#toolGradient)" className="service-float" opacity="0.8"/>
                    <text x="370" y="225" textAnchor="middle" fill="white" fontSize="8" fontWeight="bold">📡 Kupo</text>
                    <text x="370" y="235" textAnchor="middle" fill="white" fontSize="6">Optional</text>
                    <text x="370" y="242" textAnchor="middle" fill="rgba(255,255,255,0.8)" fontSize="5">:1442</text>
                  </g>

                  {/* User Interface Layer - closer to user */}
                  <g className="user-interface">
                    {/* Yaci Viewer - user accessible, aligned left like Yaci Store */}
                    <rect x="60" y="75" width="100" height="35" rx="8" fill="url(#devkitGradient)" className="service-float"/>
                    <text x="110" y="90" textAnchor="middle" fill="white" fontSize="9" fontWeight="bold">🎯 Yaci Viewer</text>
                    <text x="110" y="100" textAnchor="middle" fill="white" fontSize="7">Web UI</text>
                    <text x="110" y="107" textAnchor="middle" fill="rgba(255,255,255,0.8)" fontSize="6">:5173</text>
                  </g>

                  {/* Data flow connections */}
                  <g className="data-flow" stroke="rgba(255,255,255,0.5)" strokeWidth="2" fill="none">
                    {/* Node to Services */}
                    <line x1="230" y1="155" x2="130" y2="210" className="data-line"/>
                    <polygon points="125,205 130,215 135,205" fill="rgba(255,255,255,0.5)"/>

                    <line x1="250" y1="165" x2="250" y2="210" className="data-line" opacity="0.7"/>
                    <polygon points="245,205 250,215 255,205" fill="rgba(255,255,255,0.4)"/>

                    <line x1="270" y1="155" x2="370" y2="210" className="data-line" opacity="0.7"/>
                    <polygon points="365,205 370,215 375,205" fill="rgba(255,255,255,0.4)"/>

                    {/* Yaci Store to Yaci Viewer */}
                    <line x1="130" y1="210" x2="110" y2="110" className="data-line"/>
                    <polygon points="105,115 110,105 115,115" fill="rgba(255,255,255,0.5)"/>

                    {/* User to Yaci Viewer */}
                    <line x1="180" y1="58" x2="110" y2="75" className="data-line"/>
                    <polygon points="115,70 110,80 105,70" fill="rgba(255,255,255,0.5)"/>

                    {/* Ogmios to Kupo */}
                    <line x1="290" y1="228" x2="330" y2="228" className="data-line" opacity="0.6"/>
                    <polygon points="325,223 335,228 325,233" fill="rgba(255,255,255,0.3)"/>
                  </g>

                  {/* SDK Integration Layer */}
                  <g className="sdk-layer">
                    <text x="250" y="320" textAnchor="middle" fill="rgba(255,255,255,0.9)" fontSize="10" fontWeight="bold">Popular SDKs</text>

                    {/* SDK boxes */}
                    <rect x="90" y="330" width="70" height="25" rx="4" fill="url(#sdkGradient)" className="sdk-box"/>
                    <text x="125" y="345" textAnchor="middle" fill="#333" fontSize="7" fontWeight="bold">Mesh.js</text>

                    <rect x="175" y="330" width="70" height="25" rx="4" fill="url(#sdkGradient)" className="sdk-box"/>
                    <text x="210" y="345" textAnchor="middle" fill="#333" fontSize="7" fontWeight="bold">Lucid Evo</text>

                    <rect x="260" y="330" width="70" height="25" rx="4" fill="url(#sdkGradient)" className="sdk-box"/>
                    <text x="295" y="345" textAnchor="middle" fill="#333" fontSize="7" fontWeight="bold">CCL</text>

                    <rect x="345" y="330" width="70" height="25" rx="4" fill="url(#sdkGradient)" className="sdk-box"/>
                    <text x="380" y="345" textAnchor="middle" fill="#333" fontSize="7" fontWeight="bold">PyCardano</text>
                  </g>

                  {/* SDK Connections - to Yaci Store, Ogmios, Kupo */}
                  <g className="sdk-connections" stroke="rgba(255,255,255,0.3)" strokeWidth="1" fill="none" strokeDasharray="2,3">
                    {/* SDKs to Yaci Store */}
                    <line x1="125" y1="330" x2="130" y2="245"/>
                    <line x1="210" y1="330" x2="130" y2="245"/>
                    <line x1="295" y1="330" x2="130" y2="245"/>
                    <line x1="380" y1="330" x2="130" y2="245"/>

                    {/* SDKs to Ogmios (optional) */}
                    <line x1="150" y1="330" x2="250" y2="245" opacity="0.5"/>
                    <line x1="235" y1="330" x2="250" y2="245" opacity="0.5"/>

                    {/* SDKs to Kupo (optional) */}
                    <line x1="320" y1="330" x2="370" y2="245" opacity="0.5"/>
                    <line x1="405" y1="330" x2="370" y2="245" opacity="0.5"/>
                  </g>

                  {/* Labels for data flow */}
                  <text x="180" y="185" fill="rgba(255,255,255,0.7)" fontSize="6">blockchain data</text>
                  <text x="250" y="310" fill="rgba(255,255,255,0.7)" fontSize="6">build/submit tx</text>

                  {/* Floating data particles */}
                  <g className="particles">
                    <circle cx="350" cy="120" r="1.5" fill="rgba(255,255,255,0.6)" className="particle-float"/>
                    <circle cx="150" cy="160" r="1" fill="rgba(255,255,255,0.7)" className="particle-float"/>
                    <circle cx="380" cy="180" r="1.5" fill="rgba(255,255,255,0.5)" className="particle-float"/>
                    <circle cx="120" cy="280" r="1" fill="rgba(255,255,255,0.6)" className="particle-float"/>
                  </g>
                </svg>
              </div>
            </div>
          </div>
        </section>

        {/* Live Demo Section */}
        <section className="live-demo">
          <div className="container">
            <div className="demo-header">
              <h2 className="section-title">🚀 See Yaci DevKit in Action</h2>
              <p className="section-subtitle">Watch common yaci-cli commands execute in real-time</p>
            </div>

            <div className="terminals-grid">
              {/* Terminal 1: create-node */}
              <div className="mini-terminal">
                <div className="terminal-header">
                  <div className="terminal-buttons">
                    <span className="terminal-btn close"></span>
                    <span className="terminal-btn minimize"></span>
                    <span className="terminal-btn maximize"></span>
                  </div>
                  <div className="terminal-title">create-node</div>
                </div>
                <div className="terminal-body">
                  <div className="terminal-line">
                    <span className="terminal-prompt">cli:&gt; </span>
                    <span className="terminal-command cmd-create">create-node -o --start</span>
                  </div>
                  <div className="terminal-output output-create">
                    <div>Creating local devnet...</div>
                    <div>✓ Node started on port 3001</div>
                    <div>✓ Yaci Store on port 8080</div>
                    <div>✓ Viewer on port 5173</div>
                    <div>✓ Ogmios on port 1337</div>
                    <div>✓ Kupo on port 1442</div>
                  </div>
                </div>
              </div>

              {/* Terminal 2: topup */}
              <div className="mini-terminal">
                <div className="terminal-header">
                  <div className="terminal-buttons">
                    <span className="terminal-btn close"></span>
                    <span className="terminal-btn minimize"></span>
                    <span className="terminal-btn maximize"></span>
                  </div>
                  <div className="terminal-title">topup</div>
                </div>
                <div className="terminal-body">
                  <div className="terminal-line">
                    <span className="terminal-prompt">cli:&gt;</span>
                    <span className="terminal-command cmd-topup">topup addr_test1...90xn5f 6777</span>
                  </div>
                  <div className="terminal-output output-topup">
                    <div>✓ Transaction submitted successfully</div>
                    <div>✓ Waiting for tx to be included in block...</div>
                    <div>✓ Tx: d4f2a8b9c3e1...</div>
                  </div>
                </div>
              </div>

              {/* Terminal 3: tip */}
              <div className="mini-terminal">
                <div className="terminal-header">
                  <div className="terminal-buttons">
                    <span className="terminal-btn close"></span>
                    <span className="terminal-btn minimize"></span>
                    <span className="terminal-btn maximize"></span>
                  </div>
                  <div className="terminal-title">tip</div>
                </div>
                <div className="terminal-body">
                  <div className="terminal-line">
                    <span className="terminal-prompt">cli:&gt; </span>
                    <span className="terminal-command cmd-tip">tip</span>
                  </div>
                  <div className="terminal-output output-tip">
                    <div>Block#: 1242</div>
                    <div>Slot#: 1242</div>
                    <div>Hash: 8f7e6d5c...</div>
                  </div>
                </div>
              </div>

              {/* Terminal 4: utxos */}
              <div className="mini-terminal">
                <div className="terminal-header">
                  <div className="terminal-buttons">
                    <span className="terminal-btn close"></span>
                    <span className="terminal-btn minimize"></span>
                    <span className="terminal-btn maximize"></span>
                  </div>
                  <div className="terminal-title">utxos</div>
                </div>
                <div className="terminal-body">
                  <div className="terminal-line">
                    <span className="terminal-prompt">cli:&gt; </span>
                    <span className="terminal-command cmd-utxos">utxos addr_test1...90xn5f</span>
                  </div>
                  <div className="terminal-output output-utxos">
                    <div>1. d4f2a8b9...  #0    6777₳</div>
                    <div>2. d4f2a8b9...  #1    1₳</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Features Section */}
        <section className="features">
          <div className="container">
            <h2 className="section-title">✨ Key Features</h2>

            <div className="features-grid">
              <div className="feature-card">
                <div className="feature-icon">⚡</div>
                <h3>Lightning Fast</h3>
                <p>Create and destroy a complete Cardano devnet in seconds. Sub-second block times supported (100ms, 200ms, etc.) with full configurability (default 1s).</p>
              </div>

              <div className="feature-card">
                <div className="feature-icon">🎛️</div>
                <h3>Flexible Configuration</h3>
                <p>Customize block times, epochs, and network parameters to match your testing needs.</p>
              </div>

              <div className="feature-card">
                <div className="feature-icon">📊</div>
                <h3>Built-in Indexer</h3>
                <p>Integrated Yaci Store with Blockfrost-compatible APIs for seamless SDK integration.</p>
              </div>

              <div className="feature-card">
                <div className="feature-icon">🌐</div>
                <h3>Multi-Distribution</h3>
                <p>Docker, ZIP, and NPM distributions. Perfect for local development and CI/CD pipelines.</p>
              </div>

              <div className="feature-card">
                <div className="feature-icon">🔗</div>
                <h3>SDK Ready</h3>
                <p>Works seamlessly with Mesh.js, Cardano Client Lib, Lucid Evolution, and more.</p>
              </div>

              <div className="feature-card">
                <div className="feature-icon">🔄</div>
                <h3>Rollback Testing</h3>
                <p>Advanced rollback simulation with consensus-based testing for robust application development.</p>
              </div>
            </div>
          </div>
        </section>

        {/* Distributions Section */}
        <section className="distributions">
          <div className="container">
            <h2 className="section-title">🎯 Choose Your Distribution</h2>
            <p className="section-subtitle">Select the distribution that best fits your workflow</p>

            <div className="distribution-grid">
              <Link href="/getting-started/docker" className="distribution-card-link">
                <div className="distribution-card">
                  <div className="card-header">
                    <div className="card-icon-wrapper">
                      <span className="card-icon">🐳</span>
                    </div>
                  </div>
                  <div className="card-content">
                    <h3 className="card-title">Docker Distribution</h3>
                    <p className="card-description">
                      Complete solution with all components. Perfect for local development with Docker Compose setup.
                    </p>
                    <div className="card-features">
                      <span className="feature-tag">✓ All-in-one</span>
                      <span className="feature-tag">✓ Easy setup</span>
                    </div>
                  </div>
                  <div className="card-footer">
                    <span className="card-cta">Getting Started →</span>
                  </div>
                </div>
              </Link>

              <Link href="/getting-started/zip" className="distribution-card-link">
                <div className="distribution-card">
                  <div className="card-header">
                    <div className="card-icon-wrapper">
                      <span className="card-icon">📦</span>
                    </div>
                    <div className="card-badge alternative">Non-Docker</div>
                  </div>
                  <div className="card-content">
                    <h3 className="card-title">ZIP Distribution</h3>
                    <p className="card-description">
                      Standalone ZIP with Yaci CLI. Download and manage components directly without Docker.
                    </p>
                    <div className="card-features">
                      <span className="feature-tag">✓ No containers</span>
                      <span className="feature-tag">✓ Direct control</span>
                    </div>
                  </div>
                  <div className="card-footer">
                    <span className="card-cta">Getting Started →</span>
                  </div>
                </div>
              </Link>

              <Link href="/getting-started/npm" className="distribution-card-link">
                <div className="distribution-card">
                  <div className="card-header">
                    <div className="card-icon-wrapper">
                      <svg width="40" height="40" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path d="M1 9.68V5.32a1.68 1.68 0 0 1 1.68-1.68h4.64a1.68 1.68 0 0 1 1.68 1.68v14.36A1.68 1.68 0 0 1 7.32 21H2.68A1.68 1.68 0 0 1 1 19.32V14.5" stroke="#dc2626" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                        <path d="M15 12v7a2 2 0 0 0 2 2h4a2 2 0 0 0 2-2v-7" stroke="#dc2626" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                        <path d="M16 3h6v6h-6z" fill="#dc2626"/>
                        <path d="M19 8v4" stroke="white" strokeWidth="2" strokeLinecap="round"/>
                      </svg>
                    </div>
                    <div className="card-badge cicd">CI/CD Ready</div>
                  </div>
                  <div className="card-content">
                    <h3 className="card-title">NPM Distribution</h3>
                    <p className="card-description">
                      NPM package perfect for automated testing, GitHub Actions, and continuous integration.
                    </p>
                    <div className="card-features">
                      <span className="feature-tag">✓ Auto testing</span>
                      <span className="feature-tag">✓ CI/CD native</span>
                    </div>
                  </div>
                  <div className="card-footer">
                    <span className="card-cta">Getting Started →</span>
                  </div>
                </div>
              </Link>
            </div>
          </div>
        </section>

        {/* What's New Section */}
        <section className="whats-new">
          <div className="container">
            <h2 className="section-title">🔥 What&apos;s New in v0.11.0-beta1</h2>
            <div className="new-features">
              <div className="new-feature">
                <span className="feature-emoji">🔄</span>
                <div>
                  <strong>Rollback Testing</strong>
                  <p>Consensus-based rollback simulation</p>
                </div>
              </div>
              <div className="new-feature">
                <span className="feature-emoji">⏱️</span>
                <div>
                  <strong>Sub-second Block Times</strong>
                  <p>Support for 100ms, 200ms blocks and any custom interval (default 1s)</p>
                </div>
              </div>
              <div className="new-feature">
                <span className="feature-emoji">🌐</span>
                <div>
                  <strong>Multi-node Support</strong>
                  <p>3-node cluster for advanced rollback testing</p>
                </div>
              </div>
              <div className="new-feature">
                <span className="feature-emoji">📊</span>
                <div>
                  <strong>Enhanced Tip Command</strong>
                  <p>Shows all nodes in multi-node setup</p>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Community Section */}
        <section className="community">
          <div className="container">
            <h2 className="section-title">🤝 Community & Support</h2>

            <div className="community-links">
              <a href="https://github.com/bloxbean/yaci-devkit/discussions" className="community-link" target="_blank" rel="noopener noreferrer">
                <div className="link-icon">💬</div>
                <div>
                  <h4>GitHub Discussions</h4>
                  <p>Ask questions and share ideas</p>
                </div>
              </a>

              <a href="https://discord.gg/JtQ54MSw6p" className="community-link" target="_blank" rel="noopener noreferrer">
                <div className="link-icon">
                  <svg width="32" height="32" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M20.317 4.3698a19.7913 19.7913 0 00-4.8851-1.5152.0741.0741 0 00-.0785.0371c-.211.3753-.4447.8648-.6083 1.2495-1.8447-.2762-3.68-.2762-5.4868 0-.1636-.3933-.4058-.8742-.6177-1.2495a.077.077 0 00-.0785-.037 19.7363 19.7363 0 00-4.8852 1.515.0699.0699 0 00-.0321.0277C.5334 9.0458-.319 13.5799.0992 18.0578a.0824.0824 0 00.0312.0561c2.0528 1.5076 4.0413 2.4228 5.9929 3.0294a.0777.0777 0 00.0842-.0276c.4616-.6304.8731-1.2952 1.226-1.9942a.076.076 0 00-.0416-.1057c-.6528-.2476-1.2743-.5495-1.8722-.8923a.077.077 0 01-.0076-.1277c.1258-.0943.2517-.1923.3718-.2914a.0743.0743 0 01.0776-.0105c3.9278 1.7933 8.18 1.7933 12.0614 0a.0739.0739 0 01.0785.0095c.1202.099.246.1981.3728.2924a.077.077 0 01-.0066.1276 12.2986 12.2986 0 01-1.873.8914.0766.0766 0 00-.0407.1067c.3604.698.7719 1.3628 1.225 1.9932a.076.076 0 00.0842.0286c1.961-.6067 3.9495-1.5219 6.0023-3.0294a.077.077 0 00.0313-.0552c.5004-5.177-.8382-9.6739-3.5485-13.6604a.061.061 0 00-.0312-.0286zM8.02 15.3312c-1.1825 0-2.1569-1.0857-2.1569-2.419 0-1.3332.9555-2.4189 2.157-2.4189 1.2108 0 2.1757 1.0952 2.1568 2.419-.0189 1.3332-.9555 2.4189-2.1569 2.4189zm7.9748 0c-1.1825 0-2.1569-1.0857-2.1569-2.419 0-1.3332.9554-2.4189 2.1569-2.4189 1.2108 0 2.1757 1.0952 2.1568 2.419 0 1.3332-.9554 2.4189-2.1568 2.4189Z" fill="#5865F2"/>
                  </svg>
                </div>
                <div>
                  <h4>Discord Server</h4>
                  <p>Real-time community support</p>
                </div>
              </a>

              <a href="https://github.com/bloxbean/yaci-devkit/issues" className="community-link" target="_blank" rel="noopener noreferrer">
                <div className="link-icon">🐛</div>
                <div>
                  <h4>Report Issues</h4>
                  <p>Bug reports and feature requests</p>
                </div>
              </a>
            </div>
          </div>
        </section>

        {/* CTA Section */}
        <section className="cta">
          <div className="container">
            <h2>Ready to accelerate your Cardano development?</h2>
            <Link href="/getting-started/docker" className="btn-primary large">
              🚀 Get Started Now
            </Link>
          </div>
        </section>

        {/* Footer */}
        <footer className="footer">
          <div className="container">
            <p>© 2025 BloxBean project</p>
          </div>
        </footer>

        <style jsx>{`
          .landing-page {
            min-height: 100vh;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif;
          }

          .header {
            background: rgba(255, 255, 255, 0.95);
            backdrop-filter: blur(10px);
            border-bottom: 1px solid rgba(0, 0, 0, 0.1);
            position: sticky;
            top: 0;
            z-index: 1000;
          }

          .navbar {
            max-width: 1200px;
            margin: 0 auto;
            padding: 1rem 2rem;
            display: flex;
            justify-content: space-between;
            align-items: center;
          }

          .nav-brand {
            display: flex;
            align-items: center;
            gap: 0.5rem;
            font-weight: bold;
            font-size: 1.2rem;
          }

          .logo {
            font-size: 1.5rem;
          }

          .nav-links {
            display: flex;
            gap: 2rem;
            align-items: center;
          }

          .nav-link {
            text-decoration: none;
            color: #333;
            font-weight: 500;
            transition: color 0.3s ease;
          }

          .nav-link:hover {
            color: #667eea;
          }

          .mobile-menu-btn {
            display: none;
            background: none;
            border: none;
            font-size: 1.5rem;
            cursor: pointer;
          }

          .mobile-menu {
            display: none;
            flex-direction: column;
            padding: 1rem 2rem;
            background: white;
            border-top: 1px solid #eee;
          }

          .mobile-nav-link {
            padding: 0.5rem 0;
            text-decoration: none;
            color: #333;
          }

          .hero {
            background: linear-gradient(135deg, #0f172a 0%, #1e293b 25%, #334155 100%);
            position: relative;
            color: white;
            padding: 6rem 2rem 8rem 2rem;
            overflow: hidden;
          }

          .hero::before {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: 
              radial-gradient(circle at 20% 80%, rgba(120, 119, 198, 0.3) 0%, transparent 50%),
              radial-gradient(circle at 80% 20%, rgba(255, 119, 198, 0.15) 0%, transparent 50%),
              radial-gradient(circle at 40% 40%, rgba(120, 200, 255, 0.2) 0%, transparent 50%);
            pointer-events: none;
          }

          .hero-container {
            max-width: 1200px;
            margin: 0 auto;
            position: relative;
            z-index: 1;
          }

          .hero-content {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 4rem;
            align-items: center;
          }

          .hero-text {
            text-align: left;
          }

          .hero-illustration {
            display: flex;
            justify-content: center;
            align-items: center;
          }

          .illustration-svg {
            max-width: 100%;
            height: auto;
          }

          /* Animations */
          @keyframes pulse {
            0%, 100% { opacity: 1; transform: scale(1); }
            50% { opacity: 0.7; transform: scale(1.05); }
          }

          @keyframes float {
            0%, 100% { transform: translateY(0px); }
            50% { transform: translateY(-10px); }
          }

          @keyframes dash {
            0% { stroke-dashoffset: 20; }
            100% { stroke-dashoffset: 0; }
          }

          @keyframes particleFloat {
            0%, 100% { transform: translateY(0px) translateX(0px); }
            25% { transform: translateY(-5px) translateX(2px); }
            50% { transform: translateY(-10px) translateX(0px); }
            75% { transform: translateY(-5px) translateX(-2px); }
          }

          .node-pulse {
            animation: pulse 2s ease-in-out infinite;
          }

          .service-float {
            animation: float 3s ease-in-out infinite;
          }

          .sdk-box {
            animation: float 4s ease-in-out infinite;
          }

          .connection-pulse {
            stroke-dasharray: 5, 5;
            animation: dash 2s linear infinite;
          }

          .data-line {
            animation: dash 3s linear infinite reverse;
          }

          .particle-float {
            animation: particleFloat 4s ease-in-out infinite;
          }

          .devkit-cli {
            animation: pulse 3s ease-in-out infinite;
          }

          /* Blockchain Animation Styles */
          .blockchain-animations {
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            pointer-events: none;
            z-index: 0;
          }

          /* Moving Blocks */
          @keyframes blockMove {
            0% { transform: translateX(-100px) translateY(0px); opacity: 0; }
            10% { opacity: 1; }
            90% { opacity: 1; }
            100% { transform: translateX(calc(100vw + 100px)) translateY(-50px); opacity: 0; }
          }

          .moving-blocks {
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            overflow: hidden;
          }

          .block {
            position: absolute;
            font-size: 24px;
            color: rgba(102, 126, 234, 0.6);
            animation: blockMove 15s linear infinite;
          }

          .block-1 { top: 20%; animation-delay: 0s; color: rgba(102, 126, 234, 0.8); }
          .block-2 { top: 35%; animation-delay: 3s; color: rgba(255, 119, 198, 0.6); }
          .block-3 { top: 50%; animation-delay: 6s; color: rgba(120, 200, 255, 0.7); }
          .block-4 { top: 65%; animation-delay: 9s; color: rgba(102, 126, 234, 0.5); }
          .block-5 { top: 80%; animation-delay: 12s; color: rgba(255, 119, 198, 0.4); }

          /* Cardano Addresses */
          @keyframes addrFloat {
            0% { 
              transform: translateY(100vh) translateX(0px); 
              opacity: 0; 
              font-size: 8px;
            }
            20% { 
              opacity: 0.7; 
              font-size: 9px;
            }
            80% { 
              opacity: 0.8; 
              font-size: 10px;
            }
            100% { 
              transform: translateY(-100px) translateX(-15px); 
              opacity: 0; 
              font-size: 8px;
            }
          }

          .cardano-addresses {
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            overflow: hidden;
          }

          .addr {
            position: absolute;
            font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
            color: rgba(102, 126, 234, 0.6);
            animation: addrFloat 14s linear infinite;
            font-size: 9px;
          }

          .addr-1 { left: 15%; animation-delay: 0s; }
          .addr-2 { left: 30%; animation-delay: 2.5s; }
          .addr-3 { left: 45%; animation-delay: 5s; }
          .addr-4 { left: 60%; animation-delay: 7.5s; }
          .addr-5 { left: 75%; animation-delay: 10s; }
          .addr-6 { left: 85%; animation-delay: 12.5s; }

          /* UTXO Flow Animation */
          @keyframes utxoConsume {
            0% { 
              transform: translateX(-100px) scale(1); 
              opacity: 1; 
              filter: brightness(1);
            }
            30% { 
              transform: translateX(40vw) scale(0.8); 
              opacity: 0.8;
            }
            50% { 
              transform: translateX(50vw) scale(0.5); 
              opacity: 0.3;
              filter: brightness(0.5);
            }
            100% { 
              transform: translateX(60vw) scale(0.2); 
              opacity: 0;
            }
          }

          @keyframes utxoCreate {
            0% { 
              transform: translateX(40vw) scale(0.2); 
              opacity: 0; 
            }
            50% { 
              transform: translateX(50vw) scale(0.8); 
              opacity: 0.5;
              filter: brightness(1.5);
            }
            80% { 
              transform: translateX(calc(100vw - 100px)) scale(1); 
              opacity: 1;
            }
            100% { 
              transform: translateX(calc(100vw + 50px)) scale(1); 
              opacity: 0.8;
            }
          }

          @keyframes plutusExecute {
            0% { 
              transform: translateX(40vw) scale(0.5); 
              opacity: 0; 
            }
            25% { 
              transform: translateX(45vw) scale(1.2); 
              opacity: 1;
              filter: brightness(1.5) drop-shadow(0 0 10px rgba(120, 200, 255, 0.8));
            }
            50% { 
              transform: translateX(50vw) scale(1.5); 
              opacity: 0.9;
              filter: brightness(2) drop-shadow(0 0 15px rgba(120, 200, 255, 1));
            }
            75% { 
              transform: translateX(55vw) scale(1.2); 
              opacity: 1;
            }
            100% { 
              transform: translateX(60vw) scale(0.5); 
              opacity: 0;
            }
          }

          .utxo-flow {
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            overflow: hidden;
          }

          .utxo {
            position: absolute;
            font-size: 12px;
          }

          .utxo-box {
            background: rgba(102, 126, 234, 0.8);
            color: white;
            padding: 4px 8px;
            border-radius: 6px;
            border: 2px solid rgba(255, 255, 255, 0.3);
            font-weight: bold;
            font-size: 10px;
          }

          .utxo-input-1 { 
            top: 25%; 
            animation: utxoConsume 10s ease-in-out infinite;
            animation-delay: 0s;
          }
          .utxo-input-2 { 
            top: 40%; 
            animation: utxoConsume 10s ease-in-out infinite;
            animation-delay: 1s;
          }

          .utxo-output-1 { 
            top: 30%; 
            animation: utxoCreate 10s ease-in-out infinite;
            animation-delay: 5s;
          }
          .utxo-output-2 { 
            top: 45%; 
            animation: utxoCreate 10s ease-in-out infinite;
            animation-delay: 5.5s;
          }

          .smart-contract {
            position: absolute;
          }

          .contract-box {
            background: rgba(255, 119, 198, 0.9);
            color: white;
            padding: 6px 10px;
            border-radius: 8px;
            border: 2px solid rgba(255, 255, 255, 0.4);
            font-weight: bold;
            font-size: 10px;
            text-align: center;
          }

          .plutus-1 { 
            top: 32%; 
            animation: plutusExecute 10s ease-in-out infinite;
            animation-delay: 3s;
          }
          .plutus-2 { 
            top: 42%; 
            animation: plutusExecute 10s ease-in-out infinite;
            animation-delay: 3.5s;
          }

          /* Native Tokens */
          @keyframes tokenFlow {
            0% { 
              transform: translateX(-80px) translateY(20px) rotate(0deg); 
              opacity: 0; 
            }
            20% { 
              opacity: 1; 
              transform: translateX(20vw) translateY(0px) rotate(180deg);
            }
            50% { 
              transform: translateX(50vw) translateY(-10px) rotate(360deg);
              opacity: 0.9;
            }
            80% { 
              transform: translateX(80vw) translateY(10px) rotate(540deg);
              opacity: 1;
            }
            100% { 
              transform: translateX(calc(100vw + 80px)) translateY(30px) rotate(720deg); 
              opacity: 0; 
            }
          }

          .native-tokens {
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            overflow: hidden;
          }

          .token {
            position: absolute;
            font-size: 14px;
            color: rgba(255, 215, 0, 0.8);
            animation: tokenFlow 12s ease-in-out infinite;
            font-weight: bold;
          }

          .token-1 { top: 60%; animation-delay: 0s; color: rgba(255, 215, 0, 0.9); }
          .token-2 { top: 70%; animation-delay: 3s; color: rgba(147, 51, 234, 0.8); }
          .token-3 { top: 75%; animation-delay: 6s; color: rgba(34, 197, 94, 0.8); }
          .token-4 { top: 80%; animation-delay: 9s; color: rgba(239, 68, 68, 0.8); }

          /* Transaction Flow */
          @keyframes txFlow {
            0% { 
              transform: translateX(-50px) scale(0.5); 
              opacity: 0; 
            }
            10% { 
              opacity: 1; 
              transform: translateX(0px) scale(1);
            }
            50% { 
              transform: translateX(50vw) scale(1.2);
              opacity: 0.8;
            }
            90% { 
              opacity: 1; 
              transform: translateX(calc(100vw - 50px)) scale(0.8);
            }
            100% { 
              transform: translateX(calc(100vw + 50px)) scale(0.3); 
              opacity: 0; 
            }
          }

          .transaction-flow {
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            overflow: hidden;
          }

          .tx-particle {
            position: absolute;
            font-size: 16px;
            color: rgba(120, 200, 255, 0.8);
            animation: txFlow 8s ease-in-out infinite;
          }

          .tx-1 { top: 25%; animation-delay: 0s; }
          .tx-2 { top: 45%; animation-delay: 2s; }
          .tx-3 { top: 65%; animation-delay: 4s; }
          .tx-4 { top: 85%; animation-delay: 6s; }

          /* Hide animations on mobile for performance */
          @media (max-width: 768px) {
            .blockchain-animations {
              display: none;
            }
          }

          .hero-icon {
            font-size: 4rem;
            margin-bottom: 1rem;
          }

          .hero-title {
            font-size: 3.5rem;
            font-weight: 700;
            margin-bottom: 1rem;
            background: linear-gradient(45deg, #fff, #e0e7ff);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            background-clip: text;
          }

          .hero-subtitle {
            font-size: 1.5rem;
            margin-bottom: 1rem;
            opacity: 0.9;
          }

          .hero-description {
            font-size: 1.1rem;
            line-height: 1.6;
            margin-bottom: 2rem;
            opacity: 0.8;
            max-width: 600px;
            margin-left: auto;
            margin-right: auto;
            margin-bottom: 2rem;
          }

          .hero-buttons {
            display: flex;
            gap: 1rem;
            justify-content: center;
            margin-bottom: 3rem;
            flex-wrap: wrap;
          }

          .btn-primary, .btn-secondary {
            padding: 0.75rem 2rem;
            border-radius: 0.5rem;
            text-decoration: none;
            font-weight: 600;
            transition: all 0.3s ease;
            display: inline-block;
          }

          .btn-primary {
            background: #fff;
            color: #667eea;
          }

          .btn-primary:hover {
            background: #f0f4ff;
            transform: translateY(-2px);
          }

          .btn-primary.large {
            padding: 1rem 3rem;
            font-size: 1.2rem;
          }

          .btn-secondary {
            background: rgba(255,255,255,0.1);
            color: white;
            border: 2px solid rgba(255,255,255,0.3);
          }

          .btn-secondary:hover {
            background: rgba(255,255,255,0.2);
            transform: translateY(-2px);
          }

          .hero-stats {
            display: flex;
            justify-content: center;
            gap: 3rem;
            flex-wrap: wrap;
          }

          .stat {
            text-align: center;
          }

          .stat-icon {
            font-size: 2rem;
            margin-bottom: 0.5rem;
          }

          .stat-label {
            font-size: 0.9rem;
            opacity: 0.8;
          }

          .container {
            max-width: 1200px;
            margin: 0 auto;
            padding: 0 2rem;
          }

          .section-title {
            font-size: 2.5rem;
            text-align: center;
            margin-bottom: 1rem;
            color: #1f2937;
          }

          .section-subtitle {
            text-align: center;
            color: #6b7280;
            margin-bottom: 3rem;
          }

          .features {
            padding: 6rem 0;
            background: #f8fafc;
          }

          .features-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: 2rem;
            margin-top: 3rem;
          }

          .feature-card {
            background: white;
            padding: 2rem;
            border-radius: 0.75rem;
            text-align: center;
            transition: all 0.3s ease;
            border: 1px solid #e5e7eb;
          }

          .feature-card:hover {
            transform: translateY(-4px);
            box-shadow: 0 10px 25px rgba(0,0,0,0.1);
            border-color: #667eea;
          }

          .feature-icon {
            font-size: 2.5rem;
            margin-bottom: 1rem;
          }

          .feature-card h3 {
            margin-bottom: 1rem;
            color: #1f2937;
          }



          .code-block {
            background: #1f2937;
            color: #e5e7eb;
            padding: 1rem;
            border-radius: 0.5rem;
            overflow-x: auto;
          }

          .code-block pre {
            margin: 0;
            font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
            font-size: 0.9rem;
          }

          .distributions {
            padding: 6rem 0;
            background: #f8fafc;
          }

          .distribution-cards {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: 2rem;
            margin-top: 3rem;
          }

          .distribution-card {
            background: white;
            padding: 2rem;
            border-radius: 0.75rem;
            text-decoration: none;
            color: inherit;
            transition: all 0.3s ease;
            border: 1px solid #e5e7eb;
            position: relative;
          }

          .distribution-card:hover {
            transform: translateY(-4px);
            box-shadow: 0 10px 25px rgba(0,0,0,0.1);
            border-color: #667eea;
          }

          .card-icon {
            font-size: 2.5rem;
            margin-bottom: 1rem;
          }

          .card-badge {
            position: absolute;
            top: 1rem;
            right: 1rem;
            background: #667eea;
            color: white;
            padding: 0.25rem 0.5rem;
            border-radius: 0.25rem;
            font-size: 0.75rem;
            font-weight: 600;
          }

          .distribution-card h3 {
            margin-bottom: 1rem;
            color: #1f2937;
          }

          .whats-new {
            padding: 6rem 0;
          }

          .new-features {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 2rem;
            margin-top: 3rem;
          }

          .new-feature {
            display: flex;
            align-items: flex-start;
            gap: 1rem;
            padding: 1.5rem;
            background: #f8fafc;
            border-radius: 0.75rem;
            border-left: 4px solid #667eea;
          }

          .feature-emoji {
            font-size: 1.5rem;
            flex-shrink: 0;
          }

          .new-feature strong {
            display: block;
            color: #1f2937;
            margin-bottom: 0.25rem;
          }

          .new-feature p {
            margin: 0;
            color: #6b7280;
            font-size: 0.9rem;
          }

          .community {
            padding: 6rem 0;
            background: #f8fafc;
          }

          .community-links {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 1.5rem;
            margin-top: 3rem;
          }

          .community-link {
            display: flex;
            align-items: center;
            gap: 1rem;
            padding: 1.5rem;
            background: white;
            border: 1px solid #e5e7eb;
            border-radius: 0.75rem;
            text-decoration: none;
            color: inherit;
            transition: all 0.3s ease;
          }

          .community-link:hover {
            border-color: #667eea;
            transform: translateY(-2px);
            box-shadow: 0 4px 12px rgba(0,0,0,0.1);
          }

          .link-icon {
            font-size: 2rem;
            flex-shrink: 0;
          }

          .community-link h4 {
            margin: 0 0 0.25rem 0;
            color: #1f2937;
          }

          .community-link p {
            margin: 0;
            color: #6b7280;
            font-size: 0.9rem;
          }

          .cta {
            padding: 6rem 0;
            background: linear-gradient(135deg, #0f172a 0%, #1e293b 25%, #334155 100%);
            position: relative;
            color: white;
            text-align: center;
            overflow: hidden;
          }

          .cta::before {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: 
              radial-gradient(circle at 70% 30%, rgba(120, 119, 198, 0.25) 0%, transparent 50%),
              radial-gradient(circle at 30% 70%, rgba(255, 119, 198, 0.1) 0%, transparent 50%);
            pointer-events: none;
          }

          .cta .container {
            position: relative;
            z-index: 1;
          }

          .cta h2 {
            font-size: 2.5rem;
            margin-bottom: 2rem;
          }

          .footer {
            padding: 2rem 0;
            background: #f8fafc;
            color: #4b5563;
            text-align: center;
            border-top: 1px solid #e5e7eb;
          }

          @media (max-width: 768px) {
            .nav-links {
              display: none;
            }

            .mobile-menu-btn {
              display: block;
            }

            .mobile-menu {
              display: flex;
            }

            .hero-content {
              grid-template-columns: 1fr;
              gap: 2rem;
              text-align: center;
            }

            .hero-text {
              text-align: center;
            }

            .hero-title {
              font-size: 2.5rem;
            }

            .hero-subtitle {
              font-size: 1.2rem;
            }

            .hero-buttons {
              flex-direction: column;
              align-items: center;
            }

            .hero-stats {
              gap: 2rem;
            }

            .hero-illustration {
              order: -1;
            }

            .illustration-svg {
              width: 300px;
              height: 240px;
            }

            .step {
              flex-direction: column;
              text-align: center;
            }

            .features-grid,
            .distribution-cards,
            .new-features,
            .community-links {
              grid-template-columns: 1fr;
            }

            .section-title {
              font-size: 2rem;
            }

            .cta h2 {
              font-size: 2rem;
            }
          }
\n        `}</style>
      </div>
    </>
  )
}

export default HomePage
