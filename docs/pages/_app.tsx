import type { AppProps } from 'next/app'
import '../styles/globals.css'
// Scoped under .dk, so it only styles the landing page
import '../components/landing/landing.css'

export default function App({ Component, pageProps }: AppProps) {
  return <Component {...pageProps} />
}
