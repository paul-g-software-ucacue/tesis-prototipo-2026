// Configuración de Vite (ADR-019). En desarrollo, /api va al API Gateway del perfil consolidado,
// en el mismo origen que la SPA, como hacen Nginx en el perfil y Caddy en el VPS (D-52).
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: { '/api': 'http://localhost:8080' },
  },
  preview: { port: 4173, strictPort: true },
});
