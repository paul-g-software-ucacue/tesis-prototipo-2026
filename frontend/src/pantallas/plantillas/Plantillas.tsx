// La pantalla «Plantillas de carga» (F01-H01, RF-05): las 10 cargas de la ERS §8.3 en su orden, con
// las cargas de las que depende cada una, si es obligatoria y cuántas columnas tiene; la descarga de
// su plantilla y de su diccionario de datos, y el diccionario desplegable, con las convenciones
// comunes. La ven quienes leen las cargas o el distributivo (D-70); el servidor valida cada descarga.
// Con «Cargas: escritura» (F01-H02, D-71), cada carga tiene «Subir archivo»: el servidor valida la
// estructura del CSV o XLSX y revisa cada fila (F01-H03); responde si es válido o está rechazado,
// con los primeros 100 problemas, o por qué rechaza la estructura. No lo guarda: «Descargar el
// informe» vuelve a enviar el mismo archivo y guarda todos los problemas en CSV. Con la vista previa
// (F01-H04), «Confirmar» vuelve a enviarlo con su huella y el servicio dueño lo aplica completo o
// nada; «Historial» muestra cada intento de confirmar una carga. Cada carga dice si tiene datos, y
// «Subir archivo» se deshabilita mientras falte una carga anterior, con cuál falta (F01-H05, RF-56);
// en la de proyectos, «Sin proyectos…» confirma un periodo sin proyectos aprobados (RF-18).
import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import Stack from '@mui/material/Stack';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';
import { useQueryClient } from '@tanstack/react-query';
import { Fragment, useRef, useState, type ChangeEvent } from 'react';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { HistorialDeLaCarga } from './HistorialDeLaCarga';
import { SinProyectos } from './SinProyectos';
import { VistaPreviaDeLaCarga } from './VistaPreviaDeLaCarga';
import { traducirError, type MensajeDeError } from '../../errores/traducir';
import { useTitulo } from '../../navegacion/useTitulo';
import { tienePermiso } from '../../sesion/permisos';
import { useSesionActual } from '../../sesion/useSesionActual';
import {
  ARCHIVO_DEMASIADO_GRANDE,
  CLAVE_ESTADO,
  MAXIMO_DE_UN_ARCHIVO,
  confirmarCarga,
  descargar,
  descargarInforme,
  subirArchivo,
  useDiccionario,
  useEstadoDeLasCargas,
  type Carga,
  type CargaQueFalta,
  type ColumnaDeCarga,
  type Descarga,
  type LecturaDeArchivo,
  type Obligatoriedad,
  type ProblemaDeFila,
  type EstadoDeCarga,
  type ResultadoDeConfirmacion,
} from './consultas';

const FECHA = new Intl.DateTimeFormat('es-EC', { dateStyle: 'long', timeStyle: 'short' });

/** El resultado de una confirmación, en una frase. */
function resultado(confirmacion: ResultadoDeConfirmacion): string {
  if (confirmacion.resultado === 'REPETIDA') {
    return `Este archivo ya estaba confirmado el ${FECHA.format(new Date(confirmacion.confirmadaEl))}: no se aplicó otra vez.`;
  }
  const bajas =
    confirmacion.bajasSinAplicar > 0
      ? `; ${cuantos(confirmacion.bajasSinAplicar, 'baja quedó', 'bajas quedaron')} sin aplicar`
      : '';
  return `Carga aplicada: ${cuantos(confirmacion.nuevos, 'nuevo', 'nuevos')} y ${cuantos(confirmacion.cambiados, 'cambiado', 'cambiados')}${bajas}.`;
}

const TIPO_DE_PROBLEMA: Record<ProblemaDeFila['tipo'], string> = {
  ERROR: 'Error',
  ADVERTENCIA: 'Advertencia',
};

/** «1 error», «3 errores». */
function cuantos(numero: number, uno: string, varios: string): string {
  return `${numero} ${numero === 1 ? uno : varios}`;
}

/** Lo último que se subió: la carga, el archivo, que se guarda para descargar el informe, y su lectura. */
interface Subida {
  carga: Carga;
  archivo: File;
  lectura: LecturaDeArchivo;
}

/** El resultado de una subida: si es válido o está rechazado, los totales y los problemas. */
function ResultadoDeLaSubida({ subida, alDescargar }: { subida: Subida; alDescargar: () => void }) {
  const { lectura } = subida;
  const advertencias = cuantos(lectura.advertencias, 'advertencia', 'advertencias');
  const resumen = !lectura.valido
    ? `${lectura.archivo} está rechazado: ${cuantos(lectura.errores, 'error', 'errores')} y ${advertencias}. Corrígelo y vuelve a subirlo.`
    : lectura.advertencias > 0
      ? `${lectura.archivo} es válido, con ${advertencias}: ${lectura.registros} registros leídos.`
      : `${lectura.archivo} es válido: ${lectura.registros} registros leídos.`;
  const total = lectura.errores + lectura.advertencias;
  return (
    <>
      <Alert
        severity={lectura.valido ? (lectura.advertencias > 0 ? 'warning' : 'success') : 'error'}
        role="status"
        sx={{ mb: 1 }}
      >
        {resumen}
      </Alert>
      {lectura.problemas.length > 0 && (
        <>
          <Stack direction="row" spacing={2} sx={{ mb: 1, alignItems: 'center', flexWrap: 'wrap' }}>
            <Button
              size="small"
              variant="outlined"
              aria-label={`Descargar el informe de ${lectura.archivo}`}
              onClick={alDescargar}
            >
              Descargar el informe
            </Button>
            {total > lectura.problemas.length && (
              <Typography variant="body2">
                {`Se muestran ${lectura.problemas.length} de ${total}; el informe los trae todos.`}
              </Typography>
            )}
          </Stack>
          <TableContainer sx={{ mb: 2 }}>
            <Table aria-label={`Problemas de ${lectura.archivo}`} size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Fila</TableCell>
                  <TableCell>Campo</TableCell>
                  <TableCell>Valor</TableCell>
                  <TableCell>Motivo</TableCell>
                  <TableCell>Tipo</TableCell>
                  <TableCell>Sugerencia</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {lectura.problemas.map((problema, indice) => (
                  <TableRow key={`${problema.fila}-${problema.campo}-${indice}`}>
                    <TableCell>{problema.fila}</TableCell>
                    <TableCell>
                      <code>{problema.campo}</code>
                    </TableCell>
                    <TableCell>{problema.valor}</TableCell>
                    <TableCell>{problema.motivo}</TableCell>
                    <TableCell>{TIPO_DE_PROBLEMA[problema.tipo]}</TableCell>
                    <TableCell>{problema.sugerencia}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </>
      )}
    </>
  );
}

/** El estado de una carga, como chip: si tiene datos, o en qué periodos está «sin proyectos». */
function ChipDelEstado({ estado }: { estado: EstadoDeCarga | undefined }) {
  if (!estado) {
    return <>—</>;
  }
  if (estado.estado === 'CARGADA') {
    return <Chip size="small" variant="outlined" color="success" label="Cargada" />;
  }
  if (estado.estado === 'SIN_PROYECTOS') {
    return (
      <Chip
        size="small"
        variant="outlined"
        color="primary"
        label={`Sin proyectos (${estado.periodosSinProyectos.join(', ')})`}
      />
    );
  }
  return <Chip size="small" variant="outlined" label="Pendiente" />;
}

/** «Falta la carga de Carreras.», «Faltan las cargas de Carreras y Docentes…». */
function queFalta(faltan: CargaQueFalta[]): string {
  const nombres = faltan.map((carga) => carga.nombre);
  if (nombres.length === 1) {
    return `Falta la carga de ${nombres[0]}.`;
  }
  return `Faltan las cargas de ${nombres.slice(0, -1).join(', ')} y ${nombres.at(-1)}.`;
}

const OBLIGATORIA: Record<Obligatoriedad, string> = {
  OBLIGATORIA: 'Sí',
  OBLIGATORIA_SALVO_SIN_PROYECTOS: 'Sí, o sin proyectos',
  RECOMENDADA: 'Recomendada',
};

/** El formato de la columna y, si toma los de un catálogo, sus valores. */
function formatoOValores(columna: ColumnaDeCarga): string {
  const valores = (columna.valores ?? [])
    .map((valor) => [valor.codigo, valor.descripcion].filter(Boolean).join(' '))
    .join('; ');
  return [columna.formato, valores].filter(Boolean).join(': ');
}

function DiccionarioDeLaCarga({ carga }: { carga: Carga }) {
  return (
    <Table aria-label={`Diccionario de ${carga.nombre}`} size="small">
      <TableHead>
        <TableRow>
          <TableCell>Columna</TableCell>
          <TableCell>Descripción</TableCell>
          <TableCell>Tipo</TableCell>
          <TableCell>Longitud</TableCell>
          <TableCell>Obligatorio</TableCell>
          <TableCell>Formato o valores</TableCell>
          <TableCell>Clave</TableCell>
          <TableCell>Validación</TableCell>
          <TableCell>Ejemplo</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {carga.columnas.map((columna) => (
          <TableRow key={columna.campo}>
            <TableCell>
              <code>{columna.campo}</code>
            </TableCell>
            <TableCell>{columna.descripcion}</TableCell>
            <TableCell>{columna.tipo}</TableCell>
            <TableCell>{columna.longitud}</TableCell>
            <TableCell>{columna.obligatorio}</TableCell>
            <TableCell>{formatoOValores(columna)}</TableCell>
            <TableCell>{columna.clave}</TableCell>
            <TableCell>{columna.validacion}</TableCell>
            <TableCell>
              <code>{columna.ejemplo}</code>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}

/**
 * El botón «Subir archivo» de una carga y su selector de archivos, oculto. Mientras falte una carga
 * anterior está deshabilitado, y su descripción dice cuál falta.
 */
function SubirArchivo({
  carga,
  faltan,
  alElegir,
}: {
  carga: Carga;
  faltan: CargaQueFalta[];
  alElegir: (archivo: File) => void;
}) {
  const selector = useRef<HTMLInputElement>(null);
  const elegido = (evento: ChangeEvent<HTMLInputElement>) => {
    const archivo = evento.target.files?.[0];
    // Vacía el selector para poder elegir el mismo archivo después de corregirlo.
    evento.target.value = '';
    if (archivo) {
      alElegir(archivo);
    }
  };
  return (
    <>
      <Button
        size="small"
        variant="contained"
        aria-label={`Subir un archivo de ${carga.nombre}`}
        aria-describedby={faltan.length > 0 ? `faltan-${carga.codigo}` : undefined}
        disabled={faltan.length > 0}
        onClick={() => selector.current?.click()}
      >
        Subir archivo
      </Button>
      <input
        ref={selector}
        type="file"
        accept=".csv,.xlsx"
        aria-label={`Archivo de ${carga.nombre}`}
        hidden
        onChange={elegido}
      />
    </>
  );
}

export function Plantillas() {
  useTitulo('Plantillas de carga');
  const diccionario = useDiccionario();
  const estado = useEstadoDeLasCargas();
  const consultas = useQueryClient();
  const sesion = useSesionActual();
  const escritura = tienePermiso(sesion.data?.permisos, 'CARGAS', 'ESCRITURA');
  const verPeriodos = tienePermiso(sesion.data?.permisos, 'DISTRIBUTIVO', 'LECTURA');
  const [subida, setSubida] = useState<Subida | null>(null);
  const [desplegada, setDesplegada] = useState<number | null>(null);
  const [convenciones, setConvenciones] = useState(false);
  const [aviso, setAviso] = useState<MensajeDeError | null>(null);
  const [confirmacion, setConfirmacion] = useState<ResultadoDeConfirmacion | null>(null);
  const [confirmando, setConfirmando] = useState(false);
  const [historialDe, setHistorialDe] = useState<Carga | null>(null);
  const [sinProyectos, setSinProyectos] = useState(false);
  const [sinProyectosHecho, setSinProyectosHecho] = useState<string | null>(null);

  const estadoDe = (carga: Carga) => estado.data?.cargas.find((una) => una.codigo === carga.codigo);
  const actualizarElEstado = () => void consultas.invalidateQueries({ queryKey: CLAVE_ESTADO });

  const nombreDe = (numero: number) =>
    diccionario.data?.cargas.find((carga) => carga.numero === numero)?.nombre ?? String(numero);

  const subir = async (carga: Carga, archivo: File) => {
    setAviso(null);
    setSubida(null);
    setConfirmacion(null);
    setSinProyectosHecho(null);
    if (archivo.size > MAXIMO_DE_UN_ARCHIVO) {
      setAviso(ARCHIVO_DEMASIADO_GRANDE);
      return;
    }
    try {
      setSubida({ carga, archivo, lectura: await subirArchivo(carga, archivo) });
    } catch (fallo) {
      setAviso(traducirError(fallo));
    }
  };

  const confirmar = async ({ carga, archivo, lectura }: Subida) => {
    setAviso(null);
    setConfirmando(true);
    try {
      setConfirmacion(await confirmarCarga(carga, archivo, lectura.vistaPrevia.huella ?? ''));
      setSubida(null);
      actualizarElEstado();
    } catch (fallo) {
      setAviso(traducirError(fallo));
    } finally {
      setConfirmando(false);
    }
  };

  const cancelar = () => {
    setAviso(null);
    setSubida(null);
  };

  const bajarInforme = async ({ carga, archivo }: Subida) => {
    setAviso(null);
    try {
      await descargarInforme(carga, archivo);
    } catch (fallo) {
      setAviso(traducirError(fallo));
    }
  };

  const bajar = async (carga: Carga, que: Descarga) => {
    setAviso(null);
    setSubida(null);
    setConfirmacion(null);
    try {
      await descargar(carga, que);
    } catch (fallo) {
      setAviso(traducirError(fallo));
    }
  };

  return (
    <>
      <Typography component="h1" variant="h4" gutterBottom>
        Plantillas de carga
      </Typography>
      <Typography sx={{ mb: 2 }}>
        Descarga la plantilla de cada carga y su diccionario de datos para preparar los archivos.
        Las cargas van en este orden, porque cada una depende de las anteriores.
      </Typography>
      {diccionario.error && <AvisoDeError mensaje={traducirError(diccionario.error)} />}
      {estado.error && <AvisoDeError mensaje={traducirError(estado.error)} />}
      {aviso && (
        <AvisoDeError mensaje={aviso}>
          {aviso.detalles.length > 0 && (
            <ul>
              {aviso.detalles.map((detalle) => (
                <li key={`${detalle.campo}-${detalle.descripcion}`}>
                  {`${detalle.campo}: ${detalle.descripcion}`}
                </li>
              ))}
            </ul>
          )}
        </AvisoDeError>
      )}
      {confirmacion && (
        <Alert severity="success" role="status" sx={{ mb: 1 }}>
          {resultado(confirmacion)}
        </Alert>
      )}
      {sinProyectosHecho && (
        <Alert severity="success" role="status" sx={{ mb: 1 }}>
          {sinProyectosHecho}
        </Alert>
      )}
      {subida && (
        <ResultadoDeLaSubida subida={subida} alDescargar={() => void bajarInforme(subida)} />
      )}
      {subida && (
        <VistaPreviaDeLaCarga
          archivo={subida.lectura.archivo}
          vista={subida.lectura.vistaPrevia}
          confirmando={confirmando}
          alConfirmar={() => void confirmar(subida)}
          alCancelar={cancelar}
        />
      )}
      {historialDe && (
        <HistorialDeLaCarga carga={historialDe} alCerrar={() => setHistorialDe(null)} />
      )}
      {sinProyectos && (
        <SinProyectos
          verPeriodos={verPeriodos}
          alCerrar={() => setSinProyectos(false)}
          alConfirmar={(periodo) => {
            setSinProyectos(false);
            setAviso(null);
            setConfirmacion(null);
            setSinProyectosHecho(
              `El periodo ${periodo} quedó confirmado sin proyectos: la carga de proyectos cuenta como cargada en él.`,
            );
            actualizarElEstado();
          }}
        />
      )}
      {diccionario.data && (
        <>
          <Button
            aria-expanded={convenciones}
            aria-controls="convenciones-de-las-cargas"
            onClick={() => setConvenciones(!convenciones)}
            sx={{ mb: 1 }}
          >
            Convenciones de todas las cargas
          </Button>
          {convenciones && (
            <Table id="convenciones-de-las-cargas" aria-label="Convenciones" size="small">
              <TableBody>
                {diccionario.data.convenciones.map((convencion) => (
                  <TableRow key={convencion.tema}>
                    <TableCell component="th" scope="row">
                      {convencion.tema}
                    </TableCell>
                    <TableCell>{convencion.regla}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
          <TableContainer>
            <Table aria-label="Plantillas de carga" size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Nº</TableCell>
                  <TableCell>Carga</TableCell>
                  <TableCell>Depende de</TableCell>
                  <TableCell>Obligatoria</TableCell>
                  <TableCell>Columnas</TableCell>
                  <TableCell>Estado</TableCell>
                  <TableCell>Descargas</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {diccionario.data.cargas.map((carga) => {
                  const abierta = desplegada === carga.numero;
                  const idDelDiccionario = `diccionario-${carga.codigo}`;
                  const suEstado = estadoDe(carga);
                  const faltan = suEstado?.faltan ?? [];
                  return (
                    <Fragment key={carga.codigo}>
                      <TableRow data-carga={carga.codigo}>
                        <TableCell>{carga.numero}</TableCell>
                        <TableCell>
                          {carga.nombre}
                          <div>
                            <Button
                              size="small"
                              aria-expanded={abierta}
                              aria-controls={idDelDiccionario}
                              aria-label={`Ver el diccionario de ${carga.nombre}`}
                              onClick={() => setDesplegada(abierta ? null : carga.numero)}
                            >
                              {abierta ? 'Ocultar el diccionario' : 'Ver el diccionario'}
                            </Button>
                          </div>
                        </TableCell>
                        <TableCell>
                          {carga.dependeDe.length === 0
                            ? '—'
                            : carga.dependeDe.map(nombreDe).join('; ')}
                        </TableCell>
                        <TableCell>{OBLIGATORIA[carga.obligatoriedad]}</TableCell>
                        <TableCell>{carga.columnas.length}</TableCell>
                        <TableCell>
                          <ChipDelEstado estado={suEstado} />
                        </TableCell>
                        <TableCell>
                          <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap' }}>
                            <Button
                              size="small"
                              variant="outlined"
                              aria-label={`Descargar la plantilla de ${carga.nombre}`}
                              onClick={() => void bajar(carga, 'plantilla')}
                            >
                              Plantilla
                            </Button>
                            <Button
                              size="small"
                              variant="outlined"
                              aria-label={`Descargar el diccionario de ${carga.nombre}`}
                              onClick={() => void bajar(carga, 'diccionario')}
                            >
                              Diccionario
                            </Button>
                            <Button
                              size="small"
                              variant="outlined"
                              aria-label={`Ver el historial de ${carga.nombre}`}
                              onClick={() => setHistorialDe(carga)}
                            >
                              Historial
                            </Button>
                            {escritura && (
                              <SubirArchivo
                                carga={carga}
                                faltan={faltan}
                                alElegir={(archivo) => void subir(carga, archivo)}
                              />
                            )}
                            {escritura &&
                              carga.obligatoriedad === 'OBLIGATORIA_SALVO_SIN_PROYECTOS' && (
                                <Button
                                  size="small"
                                  variant="outlined"
                                  aria-label={`Confirmar un periodo sin proyectos en ${carga.nombre}`}
                                  onClick={() => {
                                    setSinProyectosHecho(null);
                                    setSinProyectos(true);
                                  }}
                                >
                                  Sin proyectos…
                                </Button>
                              )}
                          </Stack>
                          {escritura && faltan.length > 0 && (
                            <Typography
                              id={`faltan-${carga.codigo}`}
                              variant="body2"
                              sx={{ mt: 0.5 }}
                            >
                              {queFalta(faltan)}
                            </Typography>
                          )}
                        </TableCell>
                      </TableRow>
                      {abierta && (
                        <TableRow id={idDelDiccionario}>
                          <TableCell colSpan={7}>
                            <TableContainer>
                              <DiccionarioDeLaCarga carga={carga} />
                            </TableContainer>
                          </TableCell>
                        </TableRow>
                      )}
                    </Fragment>
                  );
                })}
              </TableBody>
            </Table>
          </TableContainer>
        </>
      )}
    </>
  );
}
