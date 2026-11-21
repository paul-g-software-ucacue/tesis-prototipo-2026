// «Reportes» (F20-H01, RF-45, RF-48, D-115): el reporte individual de un docente de la versión
// publicada vigente, en pantalla y en sus dos descargas. La pantalla propone el periodo de la
// publicación más reciente. Ver el reporte no deja entrada en la bitácora; cada descarga sí.
import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';
import { useEffect, useRef, useState } from 'react';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError, type MensajeDeError } from '../../errores/traducir';
import { useTitulo } from '../../navegacion/useTitulo';
import { Eleccion, EleccionBuscable } from '../comun/Campos';
import { DialogoInformativo } from '../comun/DialogoInformativo';
import { HuellaCopiable } from '../comun/HuellaCopiable';
import {
  COMPONENTES,
  NOMBRE_DE_LA_DEDICACION,
  NOMBRE_DEL_COMPONENTE,
  descargarReporte,
  horas,
  useDocentesDelPeriodo,
  usePublicaciones,
  useReporteIndividual,
  type Formato,
  type Publicacion,
  type ReporteIndividual,
} from './consultas';

const MOMENTO = new Intl.DateTimeFormat('es-EC', {
  dateStyle: 'short',
  timeStyle: 'short',
  hourCycle: 'h23',
  timeZone: 'America/Guayaquil',
});

// Una hoja de cálculo lee la cédula como número y le quita el cero inicial; el archivo la trae
// completa, entre comillas (decisión del autor del 2026-11-21, opción A). Se muestra en un modal al
// descargar el CSV: fijo bajo los botones pasaba desapercibido.
export const AVISO_DEL_CSV =
  'Al abrir el CSV en una hoja de cálculo, importa la columna «Identificación» como texto para que conserve el cero inicial de la cédula.';

const CAPTION = { captionSide: 'top', textAlign: 'left', px: 0 } as const;

function DeLaPublicacion({ publicacion }: { publicacion: Publicacion }) {
  return (
    <>
      <Typography variant="body1" sx={{ my: 1 }}>
        {`Versión ${String(publicacion.version)}, publicada el ${MOMENTO.format(
          new Date(publicacion.publicadaEn),
        )} por ${publicacion.publicadaPor}.`}
      </Typography>
      {publicacion.enReforma && (
        <Alert severity="info" role="status" sx={{ my: 1 }}>
          Hay una reforma en curso; el reporte sale de esta versión, que sigue siendo la publicada.
        </Alert>
      )}
    </>
  );
}

function Descargas({ periodo, identificacion }: { periodo: string; identificacion: string }) {
  const [descargando, setDescargando] = useState<Formato | null>(null);
  const [fallo, setFallo] = useState<MensajeDeError | null>(null);
  const [avisoDelCsv, setAvisoDelCsv] = useState(false);
  // El botón se deshabilita mientras descarga y pierde el foco: al cerrar el aviso se le devuelve.
  const botonDelCsv = useRef<HTMLButtonElement>(null);

  const descargar = async (formato: Formato) => {
    setFallo(null);
    setAvisoDelCsv(false);
    setDescargando(formato);
    try {
      await descargarReporte(periodo, identificacion, formato);
      setAvisoDelCsv(formato === 'csv');
    } catch (error) {
      setFallo(traducirError(error));
    } finally {
      setDescargando(null);
    }
  };

  return (
    <>
      <Stack direction="row" spacing={2} sx={{ my: 2 }}>
        {(['pdf', 'csv'] as const).map((formato) => (
          <Button
            key={formato}
            variant={formato === 'pdf' ? 'contained' : 'outlined'}
            disabled={descargando !== null}
            onClick={() => void descargar(formato)}
            ref={formato === 'csv' ? botonDelCsv : undefined}
          >
            {descargando === formato ? 'Descargando…' : `Descargar ${formato.toUpperCase()}`}
          </Button>
        ))}
      </Stack>
      <DialogoInformativo
        abierto={avisoDelCsv}
        titulo="CSV descargado"
        alCerrar={() => setAvisoDelCsv(false)}
        alTerminarDeCerrar={() => botonDelCsv.current?.focus()}
      >
        {AVISO_DEL_CSV}
      </DialogoInformativo>
      {fallo && <AvisoDeError mensaje={fallo} />}
    </>
  );
}

function Reporte({ reporte }: { reporte: ReporteIndividual }) {
  const titulo = useRef<HTMLHeadingElement>(null);
  useEffect(() => {
    titulo.current?.focus();
  }, [reporte.docente.identificacion]);
  const { docente } = reporte;
  return (
    <section aria-labelledby="reporte-del-docente">
      <Typography
        id="reporte-del-docente"
        component="h2"
        variant="h5"
        tabIndex={-1}
        ref={titulo}
        sx={{ mt: 3 }}
      >
        {docente.nombre}
      </Typography>
      <Typography variant="body1">
        {`Identificación: ${docente.identificacion} · Dedicación: ${
          docente.dedicacion ? NOMBRE_DE_LA_DEDICACION[docente.dedicacion] : 'sin dato'
        }${
          docente.horasContratadas === undefined
            ? ''
            : ` · ${String(docente.horasContratadas)} horas contratadas`
        }`}
      </Typography>
      <Typography variant="body2" color="text.secondary">
        {`Periodo ${reporte.publicacion.periodo}, versión ${String(reporte.publicacion.version)}`}
      </Typography>
      <HuellaCopiable etiqueta="Huella de la versión" hash={reporte.publicacion.hash} />

      <TableContainer sx={{ my: 2 }}>
        <Table size="small">
          <caption style={{ captionSide: 'top' }}>
            <Typography component="span" variant="h6" sx={CAPTION}>
              Asignaturas
            </Typography>
          </caption>
          <TableHead>
            <TableRow>
              <TableCell scope="col">Carrera</TableCell>
              <TableCell scope="col">Código</TableCell>
              <TableCell scope="col">Asignatura</TableCell>
              <TableCell scope="col">Paralelo</TableCell>
              <TableCell scope="col" align="right">
                Horas
              </TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {reporte.asignaturas.length === 0 && (
              <TableRow>
                <TableCell colSpan={5}>No tiene asignaturas en esta versión.</TableCell>
              </TableRow>
            )}
            {reporte.asignaturas.map((una) => (
              <TableRow key={`${una.codigo}/${una.paralelo}`}>
                <TableCell>{una.carrera}</TableCell>
                <TableCell>{una.codigo}</TableCell>
                <TableCell>{una.nombre}</TableCell>
                <TableCell>{una.paralelo}</TableCell>
                <TableCell align="right">{horas(una.horas)}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      <TableContainer sx={{ my: 2 }}>
        <Table size="small">
          <caption style={{ captionSide: 'top' }}>
            <Typography component="span" variant="h6" sx={CAPTION}>
              Actividades
            </Typography>
          </caption>
          <TableHead>
            <TableRow>
              <TableCell scope="col">Componente</TableCell>
              <TableCell scope="col">Actividad</TableCell>
              <TableCell scope="col">Unidad o proyecto</TableCell>
              <TableCell scope="col" align="right">
                Horas
              </TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {reporte.actividades.length === 0 && (
              <TableRow>
                <TableCell colSpan={4}>No tiene actividades en esta versión.</TableCell>
              </TableRow>
            )}
            {reporte.actividades.map((una) => (
              <TableRow key={`${una.componente}/${una.codigo}/${una.unidad ?? ''}`}>
                <TableCell>{NOMBRE_DEL_COMPONENTE[una.componente]}</TableCell>
                <TableCell>{una.nombre}</TableCell>
                <TableCell>{una.unidad ?? '—'}</TableCell>
                <TableCell align="right">{horas(una.horas)}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      <TableContainer sx={{ my: 2, maxWidth: 480 }}>
        <Table size="small">
          <caption style={{ captionSide: 'top' }}>
            <Typography component="span" variant="h6" sx={CAPTION}>
              Horas por componente
            </Typography>
          </caption>
          <TableHead>
            <TableRow>
              <TableCell scope="col">Componente</TableCell>
              <TableCell scope="col" align="right">
                Horas
              </TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {COMPONENTES.map(([campo, nombre]) => (
              <TableRow key={campo}>
                <TableCell component="th" scope="row">
                  {nombre}
                </TableCell>
                <TableCell align="right">{horas(reporte.horas[campo])}</TableCell>
              </TableRow>
            ))}
            <TableRow>
              <TableCell component="th" scope="row" sx={{ fontWeight: 'bold' }}>
                Total
              </TableCell>
              <TableCell align="right" sx={{ fontWeight: 'bold' }}>
                {horas(reporte.horas.total)}
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </TableContainer>

      <Descargas periodo={reporte.publicacion.periodo} identificacion={docente.identificacion} />
    </section>
  );
}

function DelPeriodo({ periodo }: { periodo: string }) {
  const docentes = useDocentesDelPeriodo(periodo);
  const [elegido, setElegido] = useState('');
  const reporte = useReporteIndividual(periodo, elegido === '' ? null : elegido);

  if (docentes.error) {
    return <AvisoDeError mensaje={traducirError(docentes.error)} />;
  }
  if (!docentes.data) {
    return (
      <Typography role="status" aria-live="polite">
        Cargando los docentes…
      </Typography>
    );
  }
  const opciones = docentes.data.docentes.map(
    (una) => [una.identificacion, `${una.nombre} · ${una.identificacion}`] as const,
  );
  return (
    <>
      <DeLaPublicacion publicacion={docentes.data.publicacion} />
      <EleccionBuscable
        etiqueta="Docente"
        campo="docente"
        valor={elegido}
        opciones={opciones}
        aviso={null}
        pista={`${String(opciones.length)} docentes en la versión publicada. Escribe su nombre o su identificación.`}
        alCambiar={setElegido}
      />
      {reporte.error && <AvisoDeError mensaje={traducirError(reporte.error)} />}
      {elegido !== '' && !reporte.data && !reporte.error && (
        <Typography role="status" aria-live="polite" sx={{ my: 2 }}>
          Cargando el reporte…
        </Typography>
      )}
      {reporte.data && <Reporte reporte={reporte.data} />}
    </>
  );
}

export function Reportes() {
  useTitulo('Reportes');
  const publicaciones = usePublicaciones();
  const [periodo, setPeriodo] = useState<string | null>(null);
  const elegido = periodo ?? publicaciones.data?.[0]?.periodo ?? null;

  return (
    <>
      <Typography component="h1" variant="h4" gutterBottom>
        Reportes
      </Typography>
      <Typography variant="body1" sx={{ mb: 2 }}>
        El reporte individual de un docente sale de la versión publicada del periodo.
      </Typography>
      {publicaciones.error && <AvisoDeError mensaje={traducirError(publicaciones.error)} />}
      {!publicaciones.data && !publicaciones.error && (
        <Typography role="status" aria-live="polite">
          Cargando los periodos publicados…
        </Typography>
      )}
      {publicaciones.data?.length === 0 && (
        <Alert severity="info" role="status" sx={{ my: 2 }}>
          Todavía no hay un distributivo publicado. Los reportes salen de la versión publicada.
        </Alert>
      )}
      {publicaciones.data && publicaciones.data.length > 0 && elegido !== null && (
        <Stack spacing={2}>
          <Eleccion
            etiqueta="Periodo"
            campo="periodo"
            valor={elegido}
            opciones={publicaciones.data.map((una) => [una.periodo, una.periodo] as const)}
            aviso={null}
            alCambiar={(valor) => setPeriodo(valor === '' ? null : valor)}
          />
          <DelPeriodo key={elegido} periodo={elegido} />
        </Stack>
      )}
    </>
  );
}
