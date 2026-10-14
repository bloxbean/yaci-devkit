import Head from 'next/head'
import Landing from '../components/landing/Landing'

const TITLE = 'Yaci DevKit · Your own Cardano devnet, in seconds'
const DESCRIPTION =
  'Yaci DevKit runs a complete local Cardano network: a block-producing node, a Blockfrost-compatible indexer, a CIP-30 wallet, an explorer and an MCP server for AI agents. One command, block times down to 100ms.'

export default function HomePage() {
  return (
    <>
      <Head>
        <title>{TITLE}</title>
        <meta name="description" content={DESCRIPTION} />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <meta property="og:title" content={TITLE} />
        <meta property="og:description" content={DESCRIPTION} />
        <link rel="icon" href="/favicon.svg" type="image/svg+xml" />
      </Head>
      <Landing />
    </>
  )
}
