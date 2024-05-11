import React from 'react'
import { DocsThemeConfig } from 'nextra-theme-docs'

const config: DocsThemeConfig = {
  logo: <span><b>Yaci DevKit</b></span>,
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
   useNextSeoProps() {
        return {
            titleTemplate: '%s – Yaci DevKit'
        }
   },
    head: (
        <>
            <meta property="description" content="Yaci DevKit - Accelerate Cardano development with a customizable devnet, featuring rapid setup, lightweight indexing, and browser-based viewer"/>
            <meta property="og:title" content="Yaci DevKit - Accelerate Cardano development with a customizable devnet"/>
            <meta property="og:description" content="Yaci DevKit - Accelerate Cardano development with a customizable devnet, featuring rapid setup, lightweight indexing, and browser-based viewer"/>
        </>
    )
}

export default config
