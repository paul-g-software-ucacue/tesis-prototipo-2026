// Pruebas en navegador con Playwright sobre la SPA construida (ADR-019, research §7 y §8): la
// accesibilidad con axe-core solo en Chromium, que también calcula el contraste; los flujos y los
// errores, en Chromium y en Firefox (Edge usa el motor de Chromium). La API se simula con page.route.
import { defineConfig, devices } from '@playwright/test';

const ancho = { width: 1366, height: 768 };

export default defineConfig({
  testDir: 'e2e',
  fullyParallel: true,
  forbidOnly: !!process.env['CI'],
  retries: process.env['CI'] ? 1 : 0,
  reporter: process.env['CI'] ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: { baseURL: 'http://localhost:4173', viewport: ancho, locale: 'es-EC' },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'], viewport: ancho } },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'], viewport: ancho },
      testMatch: /(sesion|errores)\.spec\.ts/,
    },
  ],
  webServer: {
    command: 'npm run build && npm run preview',
    url: 'http://localhost:4173',
    reuseExistingServer: !process.env['CI'],
    timeout: 120_000,
  },
});
