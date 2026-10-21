// Los diálogos de la pantalla «Periodos» con escritura (F02-H01): registrar o corregir un periodo,
// abrirlo eligiendo su versión de normativa y confirmar su cierre. El servidor valida todo; aquí
// solo se muestra lo que responde, sin códigos (ADR-019).
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
import { useVersiones } from '../normativa/consultas';
import {
  abrirPeriodo,
  corregirPeriodo,
  registrarPeriodo,
  type PeriodoAcademico,
} from './consultas';

function detalleDe(aviso: MensajeDeError | null, campo: string) {
  return aviso?.detalles.find((detalle) => detalle.campo === campo)?.descripcion;
}

/** Los detalles de un 400 que no son de ningún campo del formulario, para mostrarlos aparte. */
function otrosDetalles(aviso: MensajeDeError | null, campos: string[]) {
  return (aviso?.detalles ?? []).filter(({ campo }) => !campos.includes(campo));
}

/** Envía y guarda el error que responda el servidor, para mostrarlo en el diálogo. */
function useEnvio() {
  const [aviso, setAviso] = useState<MensajeDeError | null>(null);
  const [enviando, setEnviando] = useState(false);
  const enviar = async (trabajo: () => Promise<void>) => {
    setAviso(null);
    setEnviando(true);
    try {
      await trabajo();
    } catch (fallo) {
      setAviso(traducirError(fallo));
    } finally {
      setEnviando(false);
    }
  };
  return { aviso, enviando, enviar };
}

interface PropiedadesDeDatos {
  /** Sin periodo, registra uno nuevo; con periodo, lo corrige sin cambiar su código. */
  periodo?: PeriodoAcademico;
  alCerrar: () => void;
  alGuardar: () => Promise<void>;
}

export function DialogoDatosDelPeriodo({ periodo, alCerrar, alGuardar }: PropiedadesDeDatos) {
  const [codigo, setCodigo] = useState('');
  const [nombre, setNombre] = useState(periodo?.nombre ?? '');
  const [inicio, setInicio] = useState(periodo?.inicio ?? '');
  const [fin, setFin] = useState(periodo?.fin ?? '');
  const [falta, setFalta] = useState(false);
  const { aviso, enviando, enviar } = useEnvio();
  const titulo = periodo ? `Corregir el periodo ${periodo.codigo}` : 'Registrar periodo';

  const guardar = () => {
    if ((!periodo && !codigo.trim()) || !nombre.trim() || !inicio || !fin) {
      setFalta(true);
      return;
    }
    setFalta(false);
    void enviar(async () => {
      const datos = { nombre: nombre.trim(), inicio, fin };
      if (periodo) {
        await corregirPeriodo(periodo.codigo, datos);
      } else {
        await registrarPeriodo({ codigo: codigo.trim(), ...datos });
      }
      await alGuardar();
    });
  };

  return (
    <Dialog open onClose={alCerrar} aria-labelledby="titulo-datos-del-periodo" fullWidth>
      <DialogTitle id="titulo-datos-del-periodo">{titulo}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          {!periodo && (
            <Typography>
              El periodo empieza en Planificación, sin versión de normativa. Su código no cambia
              después.
            </Typography>
          )}
          {aviso && <AvisoDeError mensaje={aviso} />}
          {otrosDetalles(aviso, ['codigo', 'nombre', 'inicio', 'fin']).map(({ descripcion }) => (
            <Typography key={descripcion} color="error">
              {descripcion}
            </Typography>
          ))}
          {!periodo && (
            <TextField
              label="Código"
              value={codigo}
              onChange={(evento) => setCodigo(evento.target.value)}
              error={Boolean(detalleDe(aviso, 'codigo'))}
              helperText={detalleDe(aviso, 'codigo') ?? 'Por ejemplo, 2026-2.'}
              slotProps={{ htmlInput: { maxLength: 20 } }}
            />
          )}
          <TextField
            label="Nombre"
            value={nombre}
            onChange={(evento) => setNombre(evento.target.value)}
            error={Boolean(detalleDe(aviso, 'nombre'))}
            helperText={detalleDe(aviso, 'nombre')}
            slotProps={{ htmlInput: { maxLength: 120 } }}
          />
          <TextField
            label="Inicio"
            type="date"
            value={inicio}
            onChange={(evento) => setInicio(evento.target.value)}
            error={Boolean(detalleDe(aviso, 'inicio'))}
            helperText={detalleDe(aviso, 'inicio')}
            slotProps={{ inputLabel: { shrink: true } }}
          />
          <TextField
            label="Fin"
            type="date"
            value={fin}
            onChange={(evento) => setFin(evento.target.value)}
            error={Boolean(detalleDe(aviso, 'fin'))}
            helperText={detalleDe(aviso, 'fin')}
            slotProps={{ inputLabel: { shrink: true } }}
          />
          {falta && (
            <Typography color="error">
              {periodo
                ? 'Escribe el nombre y las dos fechas.'
                : 'Escribe el código, el nombre y las dos fechas.'}
            </Typography>
          )}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={alCerrar}>Cancelar</Button>
        <Button variant="contained" disabled={enviando} onClick={guardar}>
          {periodo ? 'Guardar' : 'Registrar'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

interface PropiedadesDelPeriodo {
  periodo: PeriodoAcademico;
  alCerrar: () => void;
  alTerminar: () => Promise<void>;
}

export function DialogoAbrir({ periodo, alCerrar, alTerminar }: PropiedadesDelPeriodo) {
  const versiones = useVersiones();
  const [elegida, setElegida] = useState<string>('');
  const { aviso, enviando, enviar } = useEnvio();
  // Sin elegir, la más reciente.
  const valor = elegida || String(versiones.data?.at(-1)?.numero ?? '');

  const abrir = () => {
    if (!valor) {
      return;
    }
    void enviar(async () => {
      await abrirPeriodo(periodo.codigo, Number(valor));
      await alTerminar();
    });
  };

  return (
    <Dialog open onClose={alCerrar} aria-labelledby="titulo-abrir" fullWidth>
      <DialogTitle id="titulo-abrir">{`Abrir el periodo ${periodo.codigo}`}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <Typography>
            Desde que rige este periodo, la versión ya no se puede cambiar, y el periodo no pasa a
            otra.
          </Typography>
          {versiones.error && <AvisoDeError mensaje={traducirError(versiones.error)} />}
          {aviso && <AvisoDeError mensaje={aviso} />}
          {aviso?.detalles.map(({ descripcion }) => (
            <Typography key={descripcion} color="error">
              {descripcion}
            </Typography>
          ))}
          <TextField
            select
            label="Versión de normativa"
            value={valor}
            onChange={(evento) => setElegida(evento.target.value)}
            slotProps={{ select: { native: true }, inputLabel: { shrink: true } }}
          >
            {(versiones.data ?? []).map((version) => (
              <option key={version.numero} value={version.numero}>
                {`Versión ${version.numero} · ${version.origen}`}
                {version.periodos.length > 0 ? ` (rige ${version.periodos.join(', ')})` : ''}
              </option>
            ))}
          </TextField>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={alCerrar}>Cancelar</Button>
        <Button variant="contained" disabled={enviando || !valor} onClick={abrir}>
          Abrir
        </Button>
      </DialogActions>
    </Dialog>
  );
}

export function DialogoCerrar({
  periodo,
  alCerrar,
  alConfirmar,
}: {
  periodo: PeriodoAcademico;
  alCerrar: () => void;
  alConfirmar: () => void;
}) {
  return (
    <Dialog open onClose={alCerrar} aria-labelledby="titulo-cerrar" fullWidth>
      <DialogTitle id="titulo-cerrar">{`Cerrar el periodo ${periodo.codigo}`}</DialogTitle>
      <DialogContent>
        <Typography>
          Un periodo cerrado ya no admite cambios: ni de sus datos, ni de su estado, ni de su
          versión. No se puede deshacer.
        </Typography>
      </DialogContent>
      <DialogActions>
        <Button onClick={alCerrar}>Cancelar</Button>
        <Button variant="contained" color="error" onClick={alConfirmar}>
          Cerrar el periodo
        </Button>
      </DialogActions>
    </Dialog>
  );
}
