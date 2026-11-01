// Configuración común de Vitest: los comparadores de jest-dom y la limpieza del DOM.
import '@testing-library/jest-dom/vitest';
import { cleanup, configure } from '@testing-library/react';
import { afterEach } from 'vitest';

// Con toda la suite en paralelo, una pantalla con varias consultas tarda más que el segundo por
// omisión: esperar un poco más no oculta un fallo, porque lo que no aparece sigue sin aparecer.
configure({ asyncUtilTimeout: 5000 });

afterEach(() => {
  cleanup();
});
