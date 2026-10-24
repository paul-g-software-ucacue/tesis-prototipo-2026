// Pruebas de componentes con Vitest, jsdom y Testing Library (principio VIII). La cobertura es solo
// de src/ y exige el 60 % de líneas, como cualquier módulo (D-32).
import { defineConfig, mergeConfig } from 'vitest/config';
import configuracionVite from './vite.config';

export default mergeConfig(
  configuracionVite,
  defineConfig({
    test: {
      environment: 'jsdom',
      setupFiles: ['src/pruebas/configuracion.ts'],
      include: ['src/**/*.test.{ts,tsx}'],
      restoreMocks: true,
      // Llenar un formulario largo con user-event (los de carreras, asignaturas y oferta) pasa de
      // los 5 s por omisión en un runner o una PC cargados, sin que nada esté mal (PR #139).
      testTimeout: 15_000,
      coverage: {
        provider: 'v8',
        include: ['src/**'],
        exclude: [
          'src/api/generado/**',
          'src/main.tsx',
          'src/pruebas/**',
          'src/**/*.test.{ts,tsx}',
        ],
        reporter: ['text', 'lcov'],
        thresholds: { lines: 60 },
      },
    },
  }),
);
