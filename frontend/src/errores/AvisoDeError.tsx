// El aviso de error común a todas las pantallas (ADR-019): título, descripción y, si viene, el
// identificador del incidente, con role="alert" para que lo anuncien los lectores de pantalla.
import Alert from '@mui/material/Alert';
import AlertTitle from '@mui/material/AlertTitle';
import Typography from '@mui/material/Typography';
import type { TextoDeError } from './mensajes';

interface Propiedades {
  mensaje: TextoDeError & { idIncidente?: string };
  gravedad?: 'error' | 'warning' | 'info';
}

export function AvisoDeError({ mensaje, gravedad = 'error' }: Propiedades) {
  return (
    <Alert severity={gravedad} role="alert">
      <AlertTitle>{mensaje.titulo}</AlertTitle>
      {mensaje.descripcion}
      {mensaje.idIncidente && (
        <Typography variant="body2" sx={{ mt: 1 }}>
          Identificador del incidente: {mensaje.idIncidente}
        </Typography>
      )}
    </Alert>
  );
}
