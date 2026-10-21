// El diálogo de la pantalla «Normativa» con escritura (F04-H02): crear una versión nueva a partir
// de la elegida. Asociarla a un periodo se hace al abrirlo, en «Periodos» (D-69). El servidor
// valida todo; aquí solo se muestra lo que responde, sin códigos (ADR-019).
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useState } from 'react';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError, type MensajeDeError } from '../../errores/traducir';
import { crearVersion, type VersionNormativa } from './consultas';

interface Propiedades {
  /** La versión elegida: la que se copia. */
  numero: number;
  alCerrar: () => void;
}

/** Los detalles de un 400 que no son de ningún campo del formulario, para mostrarlos aparte. */
function otrosDetalles(aviso: MensajeDeError | null, campos: string[]) {
  return (aviso?.detalles ?? []).filter(({ campo }) => !campos.includes(campo));
}

function detalleDe(aviso: MensajeDeError | null, campo: string) {
  return aviso?.detalles.find((detalle) => detalle.campo === campo)?.descripcion;
}

export function DialogoVersionNueva({
  numero,
  alCerrar,
  alCrear,
}: Propiedades & { alCrear: (creada: VersionNormativa) => Promise<void> }) {
  const [codigo, setCodigo] = useState('');
  const [origen, setOrigen] = useState('');
  const [vigenteDesde, setVigenteDesde] = useState('');
  const [aviso, setAviso] = useState<MensajeDeError | null>(null);
  const [falta, setFalta] = useState(false);
  const [enviando, setEnviando] = useState(false);

  const crear = async () => {
    if (!codigo.trim() || !origen.trim() || !vigenteDesde) {
      setFalta(true);
      return;
    }
    setFalta(false);
    setAviso(null);
    setEnviando(true);
    try {
      await alCrear(
        await crearVersion({
          copiarDe: numero,
          codigo: codigo.trim(),
          origen: origen.trim(),
          vigenteDesde,
        }),
      );
    } catch (fallo) {
      setAviso(traducirError(fallo));
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Dialog open onClose={alCerrar} aria-labelledby="titulo-version-nueva" fullWidth>
      <DialogTitle id="titulo-version-nueva">
        {`Versión nueva a partir de la versión ${numero}`}
      </DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <Typography>
            La versión nueva copia la tabla de carga horaria y los parámetros. No rige ningún
            periodo, así que se puede editar.
          </Typography>
          {aviso && <AvisoDeError mensaje={aviso} />}
          {otrosDetalles(aviso, ['codigo', 'origen', 'vigenteDesde']).map(({ descripcion }) => (
            <Typography key={descripcion} color="error">
              {descripcion}
            </Typography>
          ))}
          <TextField
            label="Código"
            value={codigo}
            onChange={(evento) => setCodigo(evento.target.value)}
            error={Boolean(detalleDe(aviso, 'codigo'))}
            helperText={detalleDe(aviso, 'codigo') ?? 'Por ejemplo, SENESCYT-2025-01.'}
            slotProps={{ htmlInput: { maxLength: 40 } }}
          />
          <TextField
            label="Origen"
            value={origen}
            onChange={(evento) => setOrigen(evento.target.value)}
            error={Boolean(detalleDe(aviso, 'origen'))}
            helperText={detalleDe(aviso, 'origen') ?? 'De dónde sale, por ejemplo una reforma.'}
            slotProps={{ htmlInput: { maxLength: 120 } }}
          />
          <TextField
            label="Vigente desde"
            type="date"
            value={vigenteDesde}
            onChange={(evento) => setVigenteDesde(evento.target.value)}
            error={Boolean(detalleDe(aviso, 'vigenteDesde'))}
            helperText={detalleDe(aviso, 'vigenteDesde')}
            slotProps={{ inputLabel: { shrink: true } }}
          />
          {falta && (
            <Typography color="error">Escribe el código, el origen y la vigencia.</Typography>
          )}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={alCerrar}>Cancelar</Button>
        <Button variant="contained" disabled={enviando} onClick={() => void crear()}>
          Crear
        </Button>
      </DialogActions>
    </Dialog>
  );
}
