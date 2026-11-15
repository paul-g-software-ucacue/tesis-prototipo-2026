// La resolución del OCS en el estado del flujo (F15-H01, D-107, contracts/spa.md): el formulario
// para el rol de EN_OCS, con su confirmación, y después la constancia con la descarga del PDF. El
// servidor comprueba todo; la pantalla solo ayuda. Las firmas del PDF no se validan (D-19).
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useCallback, useId, useRef, useState, type ChangeEvent } from 'react';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError, type MensajeDeError } from '../../errores/traducir';
import { descargarResolucion, registrarResolucion, type ResolucionDelOcs } from './flujo';
import { HuellaCopiable } from './HuellaCopiable';

const MOMENTO = new Intl.DateTimeFormat('es-EC', {
  dateStyle: 'short',
  timeStyle: 'short',
  hourCycle: 'h23',
  timeZone: 'America/Guayaquil',
});

// La fecha de la resolución es un día, sin hora: se muestra tal cual, sin moverla de zona.
const DIA = new Intl.DateTimeFormat('es-EC', { dateStyle: 'short', timeZone: 'UTC' });

/** Hoy en la institución, AAAA-MM-DD: la fecha de la resolución no puede pasar de hoy. */
function hoy() {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Guayaquil' }).format(new Date());
}

/**
 * El formulario «Registrar la resolución del OCS».
 *
 * @param alRegistrar se llama después de registrar, para recargar
 */
export function RegistroDeLaResolucion({
  periodo,
  alRegistrar,
}: {
  periodo: string;
  alRegistrar: () => Promise<void>;
}) {
  const titulo = useId();
  const [numero, setNumero] = useState('');
  const [fecha, setFecha] = useState('');
  const [archivo, setArchivo] = useState<File | null>(null);
  const [confirmar, setConfirmar] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [aviso, setAviso] = useState<MensajeDeError | null>(null);
  const selector = useRef<HTMLInputElement>(null);
  // Al abrirse la confirmación, el foco va a «Cancelar»: registrar no se deshace.
  const enfocar = useCallback((boton: HTMLButtonElement | null) => {
    boton?.focus();
  }, []);

  const elegir = (evento: ChangeEvent<HTMLInputElement>) => {
    setArchivo(evento.target.files?.[0] ?? null);
  };

  const registrar = async () => {
    if (!archivo) {
      return;
    }
    setAviso(null);
    setEnviando(true);
    try {
      await registrarResolucion(periodo, numero, fecha, archivo);
      setConfirmar(false);
      await alRegistrar();
    } catch (fallo) {
      setConfirmar(false);
      setAviso(traducirError(fallo));
    } finally {
      setEnviando(false);
    }
  };

  const completo = numero.trim().length > 0 && fecha.length > 0 && archivo !== null;

  return (
    <Box
      component="section"
      aria-labelledby={titulo}
      sx={{ my: 1, p: 2, border: 1, borderColor: 'divider', borderRadius: 1 }}
    >
      <Typography id={titulo} component="h3" variant="subtitle1" sx={{ fontWeight: 'bold' }}>
        Registrar la resolución del OCS
      </Typography>
      {aviso && <AvisoDeError mensaje={aviso} />}
      <Stack spacing={2} sx={{ mt: 1, maxWidth: 420 }}>
        <TextField
          label="Número de la resolución"
          value={numero}
          onChange={(evento) => setNumero(evento.target.value)}
          slotProps={{ htmlInput: { maxLength: 60 } }}
          helperText="Como figura en la resolución, de hasta 60 caracteres."
        />
        <TextField
          label="Fecha de la resolución"
          type="date"
          value={fecha}
          onChange={(evento) => setFecha(evento.target.value)}
          slotProps={{ inputLabel: { shrink: true }, htmlInput: { max: hoy() } }}
          helperText="Desde el día en que el distributivo llegó al OCS hasta hoy."
        />
        <Box>
          <Button variant="outlined" onClick={() => selector.current?.click()}>
            Elegir el PDF
          </Button>
          <Typography component="span" variant="body2" sx={{ ml: 1 }}>
            {archivo ? archivo.name : 'Ningún archivo elegido'}
          </Typography>
          <input
            ref={selector}
            type="file"
            hidden
            accept="application/pdf,.pdf"
            aria-label="PDF de la resolución"
            onChange={elegir}
          />
        </Box>
        <Box>
          <Button
            variant="contained"
            disabled={!completo || enviando}
            onClick={() => setConfirmar(true)}
          >
            Registrar la resolución
          </Button>
        </Box>
      </Stack>
      {confirmar && (
        <Dialog
          open
          onClose={enviando ? undefined : () => setConfirmar(false)}
          aria-labelledby="titulo-resolucion"
          aria-describedby="texto-resolucion"
        >
          <DialogTitle id="titulo-resolucion">Registrar la resolución del OCS</DialogTitle>
          <DialogContent>
            <DialogContentText id="texto-resolucion">
              El distributivo quedará aprobado con esta resolución. Después no se corrige.
            </DialogContentText>
          </DialogContent>
          <DialogActions>
            <Button ref={enfocar} onClick={() => setConfirmar(false)} disabled={enviando}>
              Cancelar
            </Button>
            <Button variant="contained" disabled={enviando} onClick={() => void registrar()}>
              Registrar y aprobar
            </Button>
          </DialogActions>
        </Dialog>
      )}
    </Box>
  );
}

/** La constancia de la resolución registrada, con la descarga del PDF. */
export function ConstanciaDeLaResolucion({
  periodo,
  resolucion,
}: {
  periodo: string;
  resolucion: ResolucionDelOcs;
}) {
  const [aviso, setAviso] = useState<MensajeDeError | null>(null);
  const [descargando, setDescargando] = useState(false);

  const descargar = async () => {
    setAviso(null);
    setDescargando(true);
    try {
      await descargarResolucion(periodo);
    } catch (fallo) {
      setAviso(traducirError(fallo));
    } finally {
      setDescargando(false);
    }
  };

  return (
    <Box sx={{ my: 1, pl: 1, borderLeft: 3, borderColor: 'success.main' }}>
      <Typography variant="body2" sx={{ fontWeight: 'bold' }}>
        {`Resolución del OCS n.º ${resolucion.numero}`}
      </Typography>
      <Typography variant="body2">
        {`Del ${DIA.format(new Date(`${resolucion.fecha}T00:00:00Z`))}, registrada por ${resolucion.registradaPor} el ${MOMENTO.format(new Date(resolucion.registradaEn))}`}
      </Typography>
      <HuellaCopiable etiqueta="Huella del PDF" hash={resolucion.sha256} />
      {aviso && <AvisoDeError mensaje={aviso} />}
      <Button
        variant="outlined"
        sx={{ mt: 1 }}
        disabled={descargando}
        onClick={() => void descargar()}
      >
        Descargar la resolución
      </Button>
      {descargando && (
        <Alert severity="info" role="status" sx={{ mt: 1 }}>
          Descargando…
        </Alert>
      )}
    </Box>
  );
}
