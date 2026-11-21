// Un mensaje informativo que el usuario tiene que ver, en un modal (F20-H01, pedido del autor del
// 2026-11-21): un aviso fijo en la pantalla pasaba desapercibido. El foco va a «Entendido» al
// abrirse y vuelve al control que lo abrió al cerrarse; Escape también lo cierra.
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import { useCallback, useId, type ReactNode } from 'react';

/**
 * @param abierto si se muestra; queda montado cerrado para que el cierre termine su transición
 * @param titulo lo que pasó, por ejemplo «CSV descargado»
 * @param children el mensaje
 * @param alCerrar se llama con «Entendido», con Escape o al pulsar fuera
 * @param alTerminarDeCerrar opcional: se llama cuando el modal ya se cerró, para llevar el foco a
 *     un control que no lo tenía al abrirse (por ejemplo, un botón deshabilitado mientras trabajaba)
 */
export function DialogoInformativo({
  abierto,
  titulo,
  children,
  alCerrar,
  alTerminarDeCerrar,
}: {
  abierto: boolean;
  titulo: string;
  children: ReactNode;
  alCerrar: () => void;
  alTerminarDeCerrar?: () => void;
}) {
  const idDelTitulo = useId();
  const idDelTexto = useId();
  const enfocar = useCallback((boton: HTMLButtonElement | null) => {
    boton?.focus();
  }, []);

  return (
    <Dialog
      open={abierto}
      onClose={alCerrar}
      aria-labelledby={idDelTitulo}
      aria-describedby={idDelTexto}
      disableRestoreFocus={alTerminarDeCerrar !== undefined}
      slotProps={{ transition: { onExited: alTerminarDeCerrar } }}
    >
      <DialogTitle id={idDelTitulo}>{titulo}</DialogTitle>
      <DialogContent>
        <DialogContentText id={idDelTexto}>{children}</DialogContentText>
      </DialogContent>
      <DialogActions>
        <Button ref={enfocar} variant="contained" onClick={alCerrar}>
          Entendido
        </Button>
      </DialogActions>
    </Dialog>
  );
}
