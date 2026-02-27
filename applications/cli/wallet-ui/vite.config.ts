import { defineConfig } from 'vite';
import { svelte } from '@sveltejs/vite-plugin-svelte';

export default defineConfig({
  plugins: [svelte()],
  base: '/wallet/',
  build: {
    outDir: '../src/main/resources/static/wallet',
    emptyOutDir: true
  },
  server: {
    proxy: {
      '/api': 'http://localhost:10000'
    }
  }
});
