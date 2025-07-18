import { defineConfig } from 'vite'
import { svelte } from '@sveltejs/vite-plugin-svelte'

export default defineConfig({
  plugins: [svelte()],
  build: {
    outDir: '../static',
    emptyOutDir: true
  },
  server: {
    port: 5173,
    proxy: {
      '/local-cluster': {
        target: 'http://localhost:10000',
        changeOrigin: true
      }
    }
  }
})