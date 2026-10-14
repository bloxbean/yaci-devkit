import { Head, Html, Main, NextScript } from 'next/document'

// Landing page theme, applied before first paint: the saved theme (next-themes' "theme" key, shared with the
// docs) or the OS preference. data-dk-js lets the landing page's scroll-reveal styles apply.
const landingThemeScript = `(function(){try{var d=document.documentElement;d.setAttribute('data-dk-js','');var t=localStorage.getItem('theme');if(t!=='light'&&t!=='dark'){t=window.matchMedia('(prefers-color-scheme: light)').matches?'light':'dark'}d.setAttribute('data-dk-theme',t)}catch(e){}})();`

export default function Document() {
  return (
    <Html lang="en">
      <Head />
      <body>
        <script dangerouslySetInnerHTML={{ __html: landingThemeScript }} />
        <Main />
        <NextScript />
      </body>
    </Html>
  )
}
