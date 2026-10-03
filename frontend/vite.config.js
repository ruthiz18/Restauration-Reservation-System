import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In development the SPA runs on :5173 and proxies API calls to the Spring Boot backend on :8080,
// so the browser sees a single origin (no CORS headaches).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
});
