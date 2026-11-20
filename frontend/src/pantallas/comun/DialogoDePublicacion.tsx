// Publicar el distributivo aprobado (F18-H01, D-111, contracts/spa.md): la Secretaría confirma que
// la versión queda publicada y bloqueada, y a quiénes se notificará. El diálogo enfoca «Cancelar»
// al abrirse, porque la acción no se deshace; un error queda en el diálogo.
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import { useCallback, useId, useState } from 'react';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError, type MensajeDeError } from '../../errores/traducir';
import { publicarDistributivo } from './flujo';

/**
 * @param version la versión vigente, la que queda publicada
 * @param alPublicar se llama después de publicar, para recargar
 */
export function DialogoDePublicacion({
  periodo,
  version,
  alCerrar,
  alPublicar,
}: {
  periodo: string;
  version: number;
  alCerrar: () => void;
  alPublicar: () => Promise<void>;
}) {
  const [aviso, setAviso] = useState<MensajeDeError | null>(null);
  const [enviando, setEnviando] = useState(false);
  const idDelTitulo = useId();
  const idDelTexto = useId();
  // Al montarse, el foco va a «Cancelar»: la trampa de foco del diálogo lo deja ahí.
  const enfocar = useCallback((boton: HTMLButtonElement | null) => {
    boton?.focus();
  }, []);

  const publicar = async () => {
    setAviso(null);
    setEnviando(true);
    try {
      await publicarDistributivo(periodo);
      await alPublicar();
    } catch (fallo) {
      setAviso(traducirError(fallo));
      setEnviando(false);
    }
  };

  return (
    <Dialog
      open
      onClose={enviando ? undefined : alCerrar}
      aria-labelledby={idDelTitulo}
      aria-describedby={idDelTexto}
    >
      <DialogTitle id={idDelTitulo}>Publicar el distributivo</DialogTitle>
      <DialogContent>
        <DialogContentText id={idDelTexto}>
          {`La versión ${String(version)} de ${periodo} quedará publicada y bloqueada: no se podrá cambiar sin una reforma. Se notificará a los docentes, a Talento Humano y a las coordinaciones.`}
        </DialogContentText>
        {aviso && <AvisoDeError mensaje={aviso} />}
      </DialogContent>
      <DialogActions>
        <Button ref={enfocar} onClick={alCerrar} disabled={enviando}>
          Cancelar
        </Button>
        <Button variant="contained" disabled={enviando} onClick={() => void publicar()}>
          Publicar
        </Button>
      </DialogActions>
    </Dialog>
  );
}
