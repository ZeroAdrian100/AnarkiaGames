/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  // El backend permite CORS desde este puerto (app.cors.allowed-origins).
  server: { port: 5173, strictPort: true },
  test: { environment: 'jsdom' },
})
