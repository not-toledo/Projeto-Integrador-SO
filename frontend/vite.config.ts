/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// O backend escuta somente em 127.0.0.1:8080. O proxy evita CORS em desenvolvimento e no preview.
const backend = { '/api': { target: 'http://127.0.0.1:8080', changeOrigin: true } }

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: { proxy: backend },
  preview: { proxy: backend },
  test: {
    environment: 'jsdom',
    include: ['src/**/*.test.{ts,tsx}'],
  },
})
