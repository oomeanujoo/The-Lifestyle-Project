import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    proxy: {
      '/api/travel': { target: process.env.VITE_TRAVEL_API_URL || 'http://localhost:8081', changeOrigin: true },
      '/api/property': { target: process.env.VITE_PROPERTY_API_URL || 'http://localhost:8082', changeOrigin: true },
      '/api/integration': { target: process.env.VITE_INTEGRATION_API_URL || 'http://localhost:8083', changeOrigin: true },
    },
  },
  test: { environment: 'jsdom', setupFiles: ['./src/test/setup.ts'] },
})
