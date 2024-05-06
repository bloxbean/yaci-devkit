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
   }
}

export default config
