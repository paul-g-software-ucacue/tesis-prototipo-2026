// Rechazar en firma (F14-H04, D-105, contracts/spa.md): el firmante en turno escribe qué hay que
// corregir, de 10 a 500 caracteres sin los espacios de los extremos (RF-36). El servidor vuelve a
// comprobar el turno y la observación.
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useState } from 'react';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { FalloDeApi, traducirError, type MensajeDeError } from '../../errores/traducir';
import { rechazarEnFirma, type FirmaPendiente, type RechazoEnFirma } from './consultas';

const MINIMO = 10;
const MAXIMO = 500;

function valida(texto: string) {
  const limpio = texto.trim();
  return limpio.length >= MINIMO && limpio.length <= MAXIMO;
}

/**
 * @param pendiente el documento en turno que se rechaza
 * @param alRechazar se llama con el rechazo registrado
 * @param alFallar se llama si el servidor responde algo que no es de la observación (un 403 de quien
 *   ya no está en turno); el diálogo se cierra
 */
export function DialogoDeRechazo({
  pendiente,
  alCerrar,
  alRechazar,
  alFallar,
}: {
  pendiente: FirmaPendiente;
  alCerrar: () => void;
  alRechazar: (rechazo: RechazoEnFirma) => Promise<void>;
  alFallar: (fallo: unknown) => Promise<void>;
}) {
  const [observacion, setObservacion] = useState('');
  const [tocada, setTocada] = useState(false);
  const [aviso, setAviso] = useState<MensajeDeError | null>(null);
  const [enviando, setEnviando] = useState(false);
  const corta = tocada && !valida(observacion);

  const rechazar = async () => {
    setAviso(null);
    setEnviando(true);
    try {
      const rechazo = await rechazarEnFirma(pendiente.documento, observacion);
      await alRechazar(rechazo);
    } catch (fallo) {
      // La observación la corrige quien rechaza; cualquier otro fallo cierra el diálogo.
      if (fallo instanceof FalloDeApi && fallo.estado === 400) {
        setAviso(traducirError(fallo));
        setEnviando(false);
      } else {
        await alFallar(fallo);
      }
    }
  };

  return (
    <Dialog open onClose={alCerrar} aria-labelledby="titulo-rechazar" fullWidth maxWidth="sm">
      <DialogTitle id="titulo-rechazar">Rechazar en firma</DialogTitle>
      <DialogContent>
        <Typography variant="body2" sx={{ mb: 2 }}>
          {`${pendiente.periodo}, versión ${String(pendiente.version)} (paso «${pendiente.rotulo}»). El documento no se firma y vuelve a la revisión del Vicerrectorado con tu observación. Las firmas ya hechas se conservan.`}
        </Typography>
        {aviso && <AvisoDeError mensaje={aviso} />}
        <TextField
          label="Observación"
          value={observacion}
          onChange={(evento) => {
            setTocada(true);
            setObservacion(evento.target.value);
          }}
          multiline
          minRows={3}
          required
          fullWidth
          error={corta}
          helperText={`${corta ? 'Escribe qué hay que corregir, de 10 a 500 caracteres. ' : 'De 10 a 500 caracteres. '}${String(observacion.length)} de ${String(MAXIMO)} caracteres.`}
          slotProps={{ htmlInput: { maxLength: MAXIMO } }}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={alCerrar}>Cancelar</Button>
        <Button
          variant="contained"
          color="error"
          disabled={!valida(observacion) || enviando}
          onClick={() => void rechazar()}
        >
          Rechazar en firma
        </Button>
      </DialogActions>
    </Dialog>
  );
}
