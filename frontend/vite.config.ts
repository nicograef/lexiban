import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import path from 'path'
import { defineConfig } from 'vitest/config'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  // Dev-only: proxy /api requests to the Spring Boot backend (like nginx does in prod).
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        // Strip the browser's Origin header so Spring Boot doesn't reject
        // proxied requests via CORS. CORS only matters browser↔Vite (same
        // origin), not Vite↔Spring (server-to-server).
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            proxyReq.removeHeader('origin')
          })
        },
      },
    },
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test-setup.ts',
  },
})
