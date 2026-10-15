// La raíz de la SPA (ADR-019): el tema en español, la caché de consultas, la sesión en memoria y el
// enrutador. main.tsx la monta con el enrutador del navegador; las pruebas, con uno de memoria.
import CssBaseline from '@mui/material/CssBaseline';
import { ThemeProvider } from '@mui/material/styles';
import { QueryClientProvider, type QueryClient } from '@tanstack/react-query';
import { RouterProvider, type createBrowserRouter } from 'react-router';
import { SesionProvider } from './sesion/SesionProvider';
import { tema } from './tema';

interface Propiedades {
  enrutador: ReturnType<typeof createBrowserRouter>;
  clienteDeConsultas: QueryClient;
}

export function App({ enrutador, clienteDeConsultas }: Propiedades) {
  return (
    <ThemeProvider theme={tema}>
      <CssBaseline />
      <QueryClientProvider client={clienteDeConsultas}>
        <SesionProvider clienteDeConsultas={clienteDeConsultas}>
          <RouterProvider router={enrutador} />
        </SesionProvider>
      </QueryClientProvider>
    </ThemeProvider>
  );
}
