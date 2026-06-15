import React from 'react'
import { DocsThemeConfig } from 'nextra-theme-docs'

const config: DocsThemeConfig = {
  logo: (
    <img
      src="/DevKit-logo.svg"
      alt="Yaci DevKit"
      style={{ height: 32, width: 'auto' }}
    />
  ),
  project: {
    link: 'https://github.com/bloxbean/yaci-devkit',
  },
  chat: {
    link: 'https://discord.gg/JtQ54MSw6p',
  },
  docsRepositoryBase: 'https://github.com/bloxbean/yaci-devkit/tree/develop/docs',
  footer: {
    text: '© 2024 BloxBean project',
  },
  sidebar: {
    defaultMenuCollapseLevel: 2,
  },
  toc: {
    backToTop: true,
  },
  useNextSeoProps() {
    return {
      titleTemplate: '%s – Yaci DevKit'
    }
  },
  head: (
    <>
      <link rel="icon" href="/favicon.svg" type="image/svg+xml" />
      <meta property="description" content="Yaci DevKit - Accelerate Cardano development with a customizable devnet, featuring rapid setup, lightweight indexing, and browser-based viewer"/>
      <meta property="og:title" content="Yaci DevKit - Accelerate Cardano development with a customizable devnet"/>
      <meta property="og:description" content="Yaci DevKit - Accelerate Cardano development with a customizable devnet, featuring rapid setup, lightweight indexing, and browser-based viewer"/>
      <meta property="og:image" content="/DevKit-logo.svg"/>
      <meta name="twitter:image" content="/DevKit-logo.svg"/>
    </>
  )
}

export default config
