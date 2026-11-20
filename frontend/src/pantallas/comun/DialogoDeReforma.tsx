// Iniciar una reforma del distributivo publicado (F18-H02, D-113, contracts/spa.md): el Vicerrector
// escribe el motivo y confirma. El diálogo dice qué pasará y a quiénes se notificará, enfoca
// «Cancelar» al abrirse, porque la acción no se deshace, y no envía un motivo de menos de 10
// caracteres. Un error queda en el diálogo.
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import TextField from '@mui/material/TextField';
import { useCallback, useId, useState } from 'react';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError, type MensajeDeError } from '../../errores/traducir';
import { iniciarReforma } from './flujo';

const MINIMO = 10;
const MAXIMO = 500;

/**
 * @param version la versión publicada, la que se reforma
 * @param alIniciar se llama después de iniciar la reforma, para recargar
 */
export function DialogoDeReforma({
  periodo,
  version,
  alCerrar,
  alIniciar,
}: {
  periodo: string;
  version: number;
  alCerrar: () => void;
  alIniciar: () => Promise<void>;
}) {
  const [motivo, setMotivo] = useState('');
  const [tocado, setTocado] = useState(false);
  const [aviso, setAviso] = useState<MensajeDeError | null>(null);
  const [enviando, setEnviando] = useState(false);
  const idDelTitulo = useId();
  const idDelTexto = useId();
  // Al montarse, el foco va a «Cancelar»: la trampa de foco del diálogo lo deja ahí.
  const enfocar = useCallback((boton: HTMLButtonElement | null) => {
    boton?.focus();
  }, []);
  const limpio = motivo.trim();
  const valido = limpio.length >= MINIMO && limpio.length <= MAXIMO;
  const corto = tocado && !valido;

  const iniciar = async () => {
    setAviso(null);
    setEnviando(true);
    try {
      await iniciarReforma(periodo, limpio);
      await alIniciar();
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
      fullWidth
    >
      <DialogTitle id={idDelTitulo}>Iniciar una reforma</DialogTitle>
      <DialogContent>
        <DialogContentText id={idDelTexto} sx={{ mb: 2 }}>
          {`Se creará la versión ${String(version + 1)}, una reforma de la versión ${String(version)}, y el distributivo volverá a consolidación con el ciclo completo de aprobación y firmas. La versión ${String(version)} sigue vigente hasta que se publique la reforma. Se notificará a quien consolida, a los docentes, a Talento Humano y a las coordinaciones.`}
        </DialogContentText>
        <TextField
          label="Motivo de la reforma"
          value={motivo}
          onChange={(evento) => {
            setTocado(true);
            setMotivo(evento.target.value);
          }}
          multiline
          minRows={3}
          required
          fullWidth
          disabled={enviando}
          error={corto}
          helperText={`${corto ? 'Escribe por qué hay que cambiar el distributivo publicado, de 10 a 500 caracteres. ' : ''}${String(motivo.length)} de ${String(MAXIMO)} caracteres.`}
          slotProps={{ htmlInput: { maxLength: MAXIMO } }}
        />
        {aviso && <AvisoDeError mensaje={aviso} />}
      </DialogContent>
      <DialogActions>
        <Button ref={enfocar} onClick={alCerrar} disabled={enviando}>
          Cancelar
        </Button>
        <Button variant="contained" disabled={enviando || !valido} onClick={() => void iniciar()}>
          Iniciar la reforma
        </Button>
      </DialogActions>
    </Dialog>
  );
}
