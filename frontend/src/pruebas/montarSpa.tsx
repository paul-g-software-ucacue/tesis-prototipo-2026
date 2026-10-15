// Monta la SPA completa en una ruta, con un enrutador de memoria y una caché de consultas nueva.
import { QueryClient } from '@tanstack/react-query';
import { render } from '@testing-library/react';
import { createMemoryRouter } from 'react-router';
import { App } from '../App';
import { rutasDeLaSpa } from '../rutas';

export function montarSpa(ruta: string) {
  const enrutador = createMemoryRouter(rutasDeLaSpa, { initialEntries: [ruta] });
  const consultas = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return { ...render(<App enrutador={enrutador} clienteDeConsultas={consultas} />), enrutador };
}
