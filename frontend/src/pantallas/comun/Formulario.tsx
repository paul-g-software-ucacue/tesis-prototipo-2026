// Los componentes comunes de los diálogos de los datos maestros (F03-H02, F03-H03): el marco del
// diálogo con «Cancelar» y su acción, y los avisos del servidor que no son de un campo.
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import type { ReactNode } from 'react';
import { AvisoDeError } from '../../errores/AvisoDeError';
import type { MensajeDeError } from '../../errores/traducir';

/** El aviso del servidor y los detalles que no son de un campo del diálogo. */
export function Avisos({
  aviso,
  campos,
}: {
  aviso: MensajeDeError | null;
  campos: readonly string[];
}) {
  return (
    <>
      {aviso && <AvisoDeError mensaje={aviso} />}
      {(aviso?.detalles ?? [])
        .filter(({ campo }) => !campos.includes(campo))
        .map(({ descripcion }) => (
          <Typography key={descripcion} color="error">
            {descripcion}
          </Typography>
        ))}
    </>
  );
}

export function Marco({
  id,
  titulo,
  accion,
  enviando,
  alCerrar,
  alEnviar,
  ancho = 'sm',
  children,
}: {
  id: string;
  titulo: string;
  accion: string;
  enviando: boolean;
  alCerrar: () => void;
  alEnviar: () => void;
  /** El ancho máximo del diálogo; el formulario del docente usa «md» (F03-H04). */
  ancho?: 'sm' | 'md';
  children: ReactNode;
}) {
  return (
    <Dialog open onClose={alCerrar} aria-labelledby={id} fullWidth maxWidth={ancho}>
      <DialogTitle id={id}>{titulo}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          {children}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={alCerrar}>Cancelar</Button>
        <Button variant="contained" disabled={enviando} onClick={alEnviar}>
          {accion}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
