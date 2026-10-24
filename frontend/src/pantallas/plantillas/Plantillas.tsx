// La pantalla «Plantillas de carga» (F01-H01, RF-05): las 10 cargas de la ERS §8.3 en su orden, con
// las cargas de las que depende cada una, si es obligatoria y cuántas columnas tiene; la descarga de
// su plantilla y de su diccionario de datos, y el diccionario desplegable, con las convenciones
// comunes. La ven quienes leen las cargas o el distributivo (D-70); el servidor valida cada descarga.
// Con «Cargas: escritura» (F01-H02, D-71), cada carga tiene «Subir archivo»: el servidor valida la
// estructura del CSV o XLSX y responde cuántos registros leyó, o por qué lo rechaza; no lo guarda.
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
import { Fragment, useRef, useState, type ChangeEvent } from 'react';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError, type MensajeDeError } from '../../errores/traducir';
import { useTitulo } from '../../navegacion/useTitulo';
import { tienePermiso } from '../../sesion/permisos';
import { useSesionActual } from '../../sesion/useSesionActual';
import {
  ARCHIVO_DEMASIADO_GRANDE,
  MAXIMO_DE_UN_ARCHIVO,
  descargar,
  subirArchivo,
  useDiccionario,
  type Carga,
  type ColumnaDeCarga,
  type Descarga,
  type LecturaDeArchivo,
  type Obligatoriedad,
} from './consultas';

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

/** El botón «Subir archivo» de una carga y su selector de archivos, oculto. */
function SubirArchivo({ carga, alElegir }: { carga: Carga; alElegir: (archivo: File) => void }) {
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
  const sesion = useSesionActual();
  const escritura = tienePermiso(sesion.data?.permisos, 'CARGAS', 'ESCRITURA');
  const [lectura, setLectura] = useState<LecturaDeArchivo | null>(null);
  const [desplegada, setDesplegada] = useState<number | null>(null);
  const [convenciones, setConvenciones] = useState(false);
  const [aviso, setAviso] = useState<MensajeDeError | null>(null);

  const nombreDe = (numero: number) =>
    diccionario.data?.cargas.find((carga) => carga.numero === numero)?.nombre ?? String(numero);

  const subir = async (carga: Carga, archivo: File) => {
    setAviso(null);
    setLectura(null);
    if (archivo.size > MAXIMO_DE_UN_ARCHIVO) {
      setAviso(ARCHIVO_DEMASIADO_GRANDE);
      return;
    }
    try {
      setLectura(await subirArchivo(carga, archivo));
    } catch (fallo) {
      setAviso(traducirError(fallo));
    }
  };

  const bajar = async (carga: Carga, que: Descarga) => {
    setAviso(null);
    setLectura(null);
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
      {lectura && (
        <Alert severity="success" role="status" sx={{ mb: 1 }}>
          {`${lectura.archivo}: ${lectura.registros} registros leídos.`}
        </Alert>
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
                  <TableCell>Descargas</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {diccionario.data.cargas.map((carga) => {
                  const abierta = desplegada === carga.numero;
                  const idDelDiccionario = `diccionario-${carga.codigo}`;
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
                            {escritura && (
                              <SubirArchivo
                                carga={carga}
                                alElegir={(archivo) => void subir(carga, archivo)}
                              />
                            )}
                          </Stack>
                        </TableCell>
                      </TableRow>
                      {abierta && (
                        <TableRow id={idDelDiccionario}>
                          <TableCell colSpan={6}>
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
