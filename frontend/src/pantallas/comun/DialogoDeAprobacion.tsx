// Aprobar el contenido (F13-H01, D-100, contracts/spa.md): el Vicerrector confirma que la versión
// queda congelada para la firma. Sin comentario. El diálogo enfoca «Cancelar» al abrirse, porque la
// acción no se deshace.
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import { useCallback, useState } from 'react';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError, type MensajeDeError } from '../../errores/traducir';
import { aprobarContenido } from './flujo';

/**
 * @param version la versión vigente, la que queda congelada
 * @param alAprobar se llama después de aprobar, para recargar
 */
export function DialogoDeAprobacion({
  periodo,
  version,
  alCerrar,
  alAprobar,
}: {
  periodo: string;
  version: number;
  alCerrar: () => void;
  alAprobar: () => Promise<void>;
}) {
  const [aviso, setAviso] = useState<MensajeDeError | null>(null);
  const [enviando, setEnviando] = useState(false);
  // Al montarse, el foco va a «Cancelar»: la trampa de foco del diálogo lo deja ahí.
  const enfocar = useCallback((boton: HTMLButtonElement | null) => {
    boton?.focus();
  }, []);

  const aprobar = async () => {
    setAviso(null);
    setEnviando(true);
    try {
      await aprobarContenido(periodo);
      await alAprobar();
    } catch (fallo) {
      setAviso(traducirError(fallo));
      setEnviando(false);
    }
  };

  return (
    <Dialog
      open
      onClose={enviando ? undefined : alCerrar}
      aria-labelledby="titulo-aprobar"
      aria-describedby="texto-aprobar"
    >
      <DialogTitle id="titulo-aprobar">Aprobar el contenido</DialogTitle>
      <DialogContent>
        <DialogContentText id="texto-aprobar">
          {`La versión ${String(version)} queda congelada: es la que se firmará. Todo cambio posterior exigirá una versión nueva.`}
        </DialogContentText>
        {aviso && <AvisoDeError mensaje={aviso} />}
      </DialogContent>
      <DialogActions>
        <Button ref={enfocar} onClick={alCerrar} disabled={enviando}>
          Cancelar
        </Button>
        <Button variant="contained" disabled={enviando} onClick={() => void aprobar()}>
          Aprobar y congelar
        </Button>
      </DialogActions>
    </Dialog>
  );
}
