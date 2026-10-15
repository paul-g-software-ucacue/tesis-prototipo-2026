// El tema de la SPA (constitución §3, ADR-019): MUI en español (esES) con la fuente Roboto del
// paquete, sin CDN externo. Los colores cumplen el contraste de WCAG 2.1 AA sobre fondo blanco.
import { createTheme } from '@mui/material/styles';
import { esES } from '@mui/material/locale';

export const tema = createTheme(
  {
    palette: {
      primary: { main: '#1f4e79' },
      secondary: { main: '#5c3d7a' },
    },
    typography: {
      fontFamily: 'Roboto, Arial, sans-serif',
    },
  },
  esES,
);
