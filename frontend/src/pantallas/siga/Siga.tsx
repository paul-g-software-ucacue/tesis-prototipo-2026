// «Exportación a SIGA» (F19-H01, RF-52, ADR-002, D-119): el CSV para SIGA de la versión publicada
// vigente y el layout de exportación que decide sus columnas. Solo la Secretaría general académica
// (PUBLICACION:APROBACION, aclaración del 2026-11-22). «Reportes» no cambia.
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';
import { useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef, useState, type ChangeEvent } from 'react';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError, type MensajeDeError } from '../../errores/traducir';
import { useTitulo } from '../../navegacion/useTitulo';
import { Eleccion } from '../comun/Campos';
import { usePublicaciones, type Publicacion } from '../reportes/consultas';
import {
  ARCHIVO_DEMASIADO_GRANDE,
  CLAVE_LAYOUT,
  MAXIMO_DEL_ARCHIVO,
  cargarLayout,
  descargarPlantilla,
  generarCsvSiga,
  useLayoutsSiga,
  type LayoutSiga,
} from './consultas';

const FECHA = new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Guayaquil' });
const CAPTION = { captionSide: 'top', textAlign: 'left', px: 0 } as const;
const ACCION_DE_CARGA = 'Cargar archivo de mapeo';
// Para el lector de pantalla: el encabezado de la columna de los botones.
const OCULTO = {
  border: 0,
  clip: 'rect(0 0 0 0)',
  height: 1,
  margin: -1,
  overflow: 'hidden',
  padding: 0,
  position: 'absolute',
  whiteSpace: 'nowrap',
  width: 1,
} as const;

function fecha(momento: string) {
  return FECHA.format(new Date(momento));
}

/** «Se generará con el layout por defecto.» o con la versión cargada, su fecha y quién. */
function conQueLayout(layout: LayoutSiga) {
  return layout.porDefecto
    ? 'Se generará con el layout por defecto.'
    : `Se generará con la versión ${String(layout.numero)} del layout de exportación, cargada el ${fecha(
        layout.cargadoEn ?? '',
      )} por ${layout.cargadoPor ?? ''}.`;
}

function DeLaPublicacion({ publicacion }: { publicacion: Publicacion }) {
  return (
    <>
      <Typography variant="body1" sx={{ my: 1 }}>
        {`Versión ${String(publicacion.version)}, publicada el ${fecha(
          publicacion.publicadaEn,
        )} por ${publicacion.publicadaPor}.`}
      </Typography>
      {publicacion.enReforma && (
        <Alert severity="info" role="status" sx={{ my: 1 }}>
          Hay una reforma en curso: el CSV sale de esta versión publicada.
        </Alert>
      )}
    </>
  );
}

function CsvParaSiga({ layout }: { layout: LayoutSiga | undefined }) {
  const publicaciones = usePublicaciones();
  const [periodo, setPeriodo] = useState<string | null>(null);
  const [generando, setGenerando] = useState(false);
  const [generado, setGenerado] = useState<string | null>(null);
  const [fallo, setFallo] = useState<MensajeDeError | null>(null);
  const elegido = periodo ?? publicaciones.data?.[0]?.periodo ?? null;
  const publicacion = publicaciones.data?.find((una) => una.periodo === elegido);

  const generar = async (codigo: string) => {
    setFallo(null);
    setGenerado(null);
    setGenerando(true);
    try {
      setGenerado(await generarCsvSiga(codigo));
    } catch (error) {
      setFallo(traducirError(error));
    } finally {
      setGenerando(false);
    }
  };

  return (
    <section aria-labelledby="csv-para-siga">
      <Typography id="csv-para-siga" component="h2" variant="h5" gutterBottom>
        CSV para SIGA
      </Typography>
      {publicaciones.error && <AvisoDeError mensaje={traducirError(publicaciones.error)} />}
      {!publicaciones.data && !publicaciones.error && (
        <Typography role="status" aria-live="polite">
          Cargando…
        </Typography>
      )}
      {publicaciones.data?.length === 0 && (
        <Alert severity="info" role="status" sx={{ my: 2 }}>
          Ningún periodo tiene un distributivo publicado. El CSV para SIGA sale de la versión
          publicada.
        </Alert>
      )}
      {publicaciones.data && publicacion && elegido !== null && (
        <Stack spacing={1}>
          <Eleccion
            etiqueta="Periodo"
            campo="periodo"
            valor={elegido}
            opciones={publicaciones.data.map((una) => [una.periodo, una.periodo] as const)}
            aviso={null}
            alCambiar={(valor) => {
              setPeriodo(valor === '' ? null : valor);
              setGenerado(null);
              setFallo(null);
            }}
          />
          <DeLaPublicacion publicacion={publicacion} />
          {layout && <Typography variant="body1">{conQueLayout(layout)}</Typography>}
          <div>
            <Button variant="contained" disabled={generando} onClick={() => void generar(elegido)}>
              {generando ? 'Generando…' : 'Generar CSV para SIGA'}
            </Button>
          </div>
        </Stack>
      )}
      <Typography role="status" aria-live="polite" sx={{ my: 1 }}>
        {generado ? `Se generó ${generado}.` : ''}
      </Typography>
      {fallo && <AvisoDeError mensaje={fallo} />}
    </section>
  );
}

function Columnas({ layout, titulo }: { layout: LayoutSiga; titulo: string }) {
  return (
    <TableContainer sx={{ my: 2 }}>
      <Table size="small">
        <caption style={{ captionSide: 'top' }}>
          <Typography component="span" variant="h6" sx={CAPTION}>
            {titulo}
          </Typography>
        </caption>
        <TableHead>
          <TableRow>
            <TableCell scope="col">Posición</TableCell>
            <TableCell scope="col">Nombre</TableCell>
            <TableCell scope="col">Campo</TableCell>
            <TableCell scope="col">Traducción</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {layout.columnas.map((columna, indice) => (
            <TableRow key={columna.nombre}>
              <TableCell>{indice + 1}</TableCell>
              <TableCell component="th" scope="row">
                {columna.nombre}
              </TableCell>
              <TableCell>{columna.campo}</TableCell>
              <TableCell>
                {columna.traduccion
                  ? columna.traduccion.map((una) => `${una.codigo} → ${una.valor}`).join(', ')
                  : '—'}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}

function Anterior({ layout }: { layout: LayoutSiga }) {
  const [abierta, setAbierta] = useState(false);
  const id = `layout-${String(layout.numero)}`;
  return (
    <>
      <TableRow>
        <TableCell component="th" scope="row">
          {layout.numero}
        </TableCell>
        <TableCell>{layout.cargadoPor}</TableCell>
        <TableCell>{fecha(layout.cargadoEn ?? '')}</TableCell>
        <TableCell>{layout.columnas.length}</TableCell>
        <TableCell>
          <Button
            size="small"
            aria-expanded={abierta}
            aria-controls={id}
            onClick={() => setAbierta(!abierta)}
          >
            {abierta
              ? `Ocultar las columnas de la versión ${String(layout.numero)}`
              : `Ver las columnas de la versión ${String(layout.numero)}`}
          </Button>
        </TableCell>
      </TableRow>
      {abierta && (
        <TableRow id={id}>
          <TableCell colSpan={5}>
            <Columnas layout={layout} titulo={`Columnas de la versión ${String(layout.numero)}`} />
          </TableCell>
        </TableRow>
      )}
    </>
  );
}

function LayoutDeExportacion({
  layouts,
}: {
  layouts: { vigente: LayoutSiga; anteriores: LayoutSiga[] };
}) {
  const consultas = useQueryClient();
  const selector = useRef<HTMLInputElement>(null);
  const tituloDeProblemas = useRef<HTMLHeadingElement>(null);
  const [enviando, setEnviando] = useState(false);
  const [fallo, setFallo] = useState<MensajeDeError | null>(null);
  const [cargado, setCargado] = useState<number | null>(null);

  // Con problemas, el foco va al título de la lista (contracts/spa.md).
  useEffect(() => {
    if (fallo && fallo.detalles.length > 0) {
      tituloDeProblemas.current?.focus();
    }
  }, [fallo]);

  const elegir = (evento: ChangeEvent<HTMLInputElement>) => {
    const archivo = evento.target.files?.[0];
    // Sin esto, elegir otra vez el mismo archivo corregido no avisaría nada.
    evento.target.value = '';
    setCargado(null);
    if (!archivo) {
      return;
    }
    if (archivo.size > MAXIMO_DEL_ARCHIVO) {
      setFallo(ARCHIVO_DEMASIADO_GRANDE);
      return;
    }
    setFallo(null);
    setEnviando(true);
    void (async () => {
      try {
        const nuevo = await cargarLayout(archivo);
        setCargado(nuevo.numero);
        await consultas.invalidateQueries({ queryKey: CLAVE_LAYOUT });
      } catch (error) {
        setFallo(traducirError(error));
      } finally {
        setEnviando(false);
      }
    })();
  };

  const descargar = () => {
    setFallo(null);
    void descargarPlantilla().catch((error: unknown) => setFallo(traducirError(error)));
  };

  return (
    <section aria-labelledby="layout-de-exportacion">
      <Typography
        id="layout-de-exportacion"
        component="h2"
        variant="h5"
        sx={{ mt: 4 }}
        gutterBottom
      >
        Layout de exportación
      </Typography>
      <Typography variant="body1">{conQueLayout(layouts.vigente)}</Typography>
      <Stack direction="row" spacing={1} sx={{ my: 2, flexWrap: 'wrap' }}>
        <Button onClick={descargar}>Descargar plantilla</Button>
        <Button variant="contained" disabled={enviando} onClick={() => selector.current?.click()}>
          {enviando ? 'Cargando…' : ACCION_DE_CARGA}
        </Button>
        <input
          ref={selector}
          type="file"
          hidden
          accept=".yaml,.yml,text/yaml"
          aria-label={ACCION_DE_CARGA}
          onChange={elegir}
        />
      </Stack>
      <Typography role="status" aria-live="polite">
        {cargado === null
          ? ''
          : `Se cargó la versión ${String(cargado)} del layout de exportación. El próximo CSV saldrá con sus columnas.`}
      </Typography>
      {fallo && fallo.detalles.length > 0 && (
        <Alert severity="error" role="alert" sx={{ my: 2 }}>
          <Typography component="h3" variant="subtitle1" tabIndex={-1} ref={tituloDeProblemas}>
            {`El archivo tiene ${String(fallo.detalles.length)} ${
              fallo.detalles.length === 1 ? 'problema' : 'problemas'
            }. El layout vigente no cambió.`}
          </Typography>
          <ul style={{ margin: '8px 0 0', paddingLeft: 20 }}>
            {fallo.detalles.map((detalle, indice) => (
              <li key={`${String(indice)}-${detalle.campo}`}>
                {detalle.campo === 'archivo'
                  ? detalle.descripcion
                  : `${detalle.campo}: ${detalle.descripcion}`}
              </li>
            ))}
          </ul>
        </Alert>
      )}
      {fallo && fallo.detalles.length === 0 && <AvisoDeError mensaje={fallo} />}
      <Columnas layout={layouts.vigente} titulo="Layout vigente" />

      <Typography component="h3" variant="h6" sx={{ mt: 2 }}>
        Versiones anteriores
      </Typography>
      {layouts.anteriores.length === 0 ? (
        <Typography>No hay versiones anteriores.</Typography>
      ) : (
        <TableContainer sx={{ my: 1 }}>
          <Table size="small">
            <caption style={{ captionSide: 'top' }}>
              <Typography component="span" variant="body2" sx={CAPTION}>
                Las versiones anteriores del layout de exportación, de la más reciente a la más
                antigua
              </Typography>
            </caption>
            <TableHead>
              <TableRow>
                <TableCell scope="col">Versión</TableCell>
                <TableCell scope="col">Cargada por</TableCell>
                <TableCell scope="col">Cargada el</TableCell>
                <TableCell scope="col">Columnas</TableCell>
                <TableCell scope="col">
                  <Box component="span" sx={OCULTO}>
                    Acciones
                  </Box>
                </TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {layouts.anteriores.map((anterior) => (
                <Anterior key={anterior.numero} layout={anterior} />
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </section>
  );
}

export function Siga() {
  useTitulo('Exportación a SIGA');
  const layouts = useLayoutsSiga();

  return (
    <>
      <Typography component="h1" variant="h4" gutterBottom>
        Exportación a SIGA
      </Typography>
      <Typography variant="body1" sx={{ mb: 2 }}>
        El CSV para SIGA sale de la versión publicada, con una fila por docente y actividad y las
        columnas del layout de exportación.
      </Typography>
      <CsvParaSiga layout={layouts.data?.vigente} />
      {layouts.error && <AvisoDeError mensaje={traducirError(layouts.error)} />}
      {layouts.data && <LayoutDeExportacion layouts={layouts.data} />}
    </>
  );
}
