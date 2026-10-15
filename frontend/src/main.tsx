// Punto de entrada de la SPA (ADR-019): la fuente del paquete, el enrutador del navegador y la
// caché de consultas, sin reintentos ante un error para no repetir peticiones que fallan.
import '@fontsource/roboto/400.css';
import '@fontsource/roboto/500.css';
import '@fontsource/roboto/700.css';
import { QueryClient } from '@tanstack/react-query';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { createBrowserRouter } from 'react-router';
import { App } from './App';
import { rutasDeLaSpa } from './rutas';

const raiz = document.getElementById('raiz');
if (raiz) {
  createRoot(raiz).render(
    <StrictMode>
      <App
        enrutador={createBrowserRouter(rutasDeLaSpa)}
        clienteDeConsultas={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
      />
    </StrictMode>,
  );
}
