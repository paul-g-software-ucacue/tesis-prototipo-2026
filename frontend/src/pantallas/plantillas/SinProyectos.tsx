// La confirmación «sin proyectos» de un periodo (F01-H05, RF-18): quien confirma las cargas declara
// que el periodo no tiene proyectos aprobados, y la carga de proyectos cuenta como cargada en él. El
// periodo se revisa antes de enviar con la regla del contrato (principio XIV); el servidor decide si
// existe y si tiene proyectos asignables. Quien puede leer los periodos los ve como sugerencias.
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useState, type FormEvent } from 'react';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError, type MensajeDeError } from '../../errores/traducir';
import { usePeriodos } from '../periodos/consultas';
import { confirmarSinProyectos } from './consultas';
import { motivoDelPeriodo } from './validacionDelPeriodo';

const TITULO = 'titulo-sin-proyectos';
const SUGERENCIAS = 'periodos-de-los-ciclos';

export function SinProyectos({
  verPeriodos,
  alConfirmar,
  alCerrar,
}: {
  /** Si quien confirma puede leer los periodos, para sugerirlos. */
  verPeriodos: boolean;
  alConfirmar: (periodo: string) => void;
  alCerrar: () => void;
}) {
  const periodos = usePeriodos(verPeriodos);
  const [periodo, setPeriodo] = useState('');
  const [motivo, setMotivo] = useState<string | null>(null);
  const [aviso, setAviso] = useState<MensajeDeError | null>(null);
  const [enviando, setEnviando] = useState(false);

  const enviar = async (evento: FormEvent) => {
    evento.preventDefault();
    setAviso(null);
    const revisado = motivoDelPeriodo(periodo);
    setMotivo(revisado);
    if (revisado) {
      return;
    }
    setEnviando(true);
    try {
      await confirmarSinProyectos(periodo.trim());
      alConfirmar(periodo.trim());
    } catch (fallo) {
      setAviso(traducirError(fallo));
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Dialog open onClose={alCerrar} aria-labelledby={TITULO} maxWidth="sm" fullWidth>
      <form noValidate onSubmit={(evento) => void enviar(evento)}>
        <DialogTitle id={TITULO}>Confirmar un periodo sin proyectos</DialogTitle>
        <DialogContent>
          <Typography sx={{ mb: 2 }}>
            Confírmalo solo si el periodo no tiene proyectos de investigación, vinculación ni
            prácticas aprobados. La carga de proyectos contará como cargada en ese periodo, y la
            confirmación quedará en su historial.
          </Typography>
          {aviso && <AvisoDeError mensaje={aviso} />}
          <TextField
            label="Periodo"
            value={periodo}
            onChange={(evento) => setPeriodo(evento.target.value)}
            error={motivo !== null}
            helperText={motivo ?? 'Como en los ciclos académicos: 2026-I o 2026-II.'}
            required
            fullWidth
            slotProps={{ htmlInput: { maxLength: 10, list: SUGERENCIAS } }}
          />
          <datalist id={SUGERENCIAS}>
            {(periodos.data ?? []).map((uno) => (
              <option key={uno.codigo} value={uno.codigo} />
            ))}
          </datalist>
        </DialogContent>
        <DialogActions>
          <Button onClick={alCerrar}>Cancelar</Button>
          <Button type="submit" variant="contained" disabled={enviando}>
            Confirmar sin proyectos
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
