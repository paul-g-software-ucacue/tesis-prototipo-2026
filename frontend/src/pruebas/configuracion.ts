// Configuración común de Vitest: los comparadores de jest-dom y la limpieza del DOM.
import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

afterEach(() => {
  cleanup();
});
