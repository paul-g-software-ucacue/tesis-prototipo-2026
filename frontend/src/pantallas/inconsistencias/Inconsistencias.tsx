// La pantalla «Panel de inconsistencias» (F10-H01, RF-29, MD-07): lo que impide enviar a revisión,
// con su regla y qué hacer, y lo ya justificado aparte. El servidor evalúa y ordena: aquí solo se
// muestra. Lo leen Consolidación y el Vicerrectorado; justifica Consolidación las blandas (D-87).
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
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link as RouterLink } from 'react-router';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError } from '../../errores/traducir';
import { useTitulo } from '../../navegacion/useTitulo';
import {
  ESCRITURA_DEL_PANEL,
  LECTURA_DE_LA_OFERTA,
  LECTURA_DE_LOS_DOCENTES,
} from '../../sesion/funcionalidades';
import { tieneAlguno, type PermisoRequerido } from '../../sesion/permisos';
import { useSesionActual } from '../../sesion/useSesionActual';
import { useCarreras } from '../carreras/consultas';
import { ayuda, detalleDe, SELECCION } from '../comun/formulario';
import { Avisos, Marco } from '../comun/Formulario';
import { useEnvio } from '../comun/useEnvio';
import { usePeriodos } from '../periodos/consultas';
import { NOMBRE_DEL_ESTADO } from '../periodos/estados';
import {
  CLAVE_PANEL,
  justificarInconsistencia,
  usePanel,
  type InconsistenciaDelPanel,
} from './consultas';

const LARGO_MINIMO = 10;
const LARGO_MAXIMO = 500;
const TEXTO_OBLIGATORIO = `Escribe por qué se acepta, de ${String(LARGO_MINIMO)} a ${String(LARGO_MAXIMO)} caracteres.`;

const PANTALLAS: Record<
  InconsistenciaDelPanel['pantalla'],
  { ruta: string; texto: string; permisos: readonly PermisoRequerido[] }
> = {
  OFERTA: { ruta: '/oferta', texto: 'Ir a la oferta', permisos: LECTURA_DE_LA_OFERTA },
  ACTIVIDADES: {
    ruta: '/actividades',
    texto: 'Ir a las actividades',
    permisos: LECTURA_DE_LA_OFERTA,
  },
  CARGOS: { ruta: '/cargos', texto: 'Ir a los cargos', permisos: LECTURA_DE_LOS_DOCENTES },
};

/** Una frase como «2 duras y 1 blanda abiertas». */
function totales(duras: number, blandas: number) {
  const parte = (cuantas: number, singular: string, plural: string) =>
    `${String(cuantas)} ${cuantas === 1 ? singular : plural}`;
  return `${parte(duras, 'dura', 'duras')} y ${parte(blandas, 'blanda', 'blandas')} abiertas`;
}

/** Las blandas que evalúa el panel; RN-19 se justifica en su propuesta (FR-025). */
function sePuedeJustificar(una: InconsistenciaDelPanel) {
  return una.severidad === 'BLANDA' && una.tipo !== 'EXCEPCION_DE_AFINIDAD';
}

function DialogoDeJustificacion({
  periodo,
  inconsistencia,
  nombre,
  alCerrar,
  alGuardar,
}: {
  periodo: string;
  inconsistencia: InconsistenciaDelPanel;
  nombre: string;
  alCerrar: () => void;
  alGuardar: () => Promise<void>;
}) {
  const [texto, setTexto] = useState(inconsistencia.justificacion?.texto ?? '');
  const [falta, setFalta] = useState(false);
  const { aviso, enviando, enviar } = useEnvio(alGuardar);
  const pista = `${String(texto.length)} de ${String(LARGO_MAXIMO)} caracteres.`;

  const guardar = () => {
    const limpio = texto.trim();
    const corto = limpio.length < LARGO_MINIMO;
    setFalta(corto);
    if (!corto) {
      enviar(() =>
        justificarInconsistencia(periodo, {
          tipo: inconsistencia.tipo,
          objeto: inconsistencia.objeto.clave,
          texto: limpio,
        }),
      );
    }
  };

  return (
    <Marco
      id="titulo-justificar-inconsistencia"
      titulo={`Justificar: ${nombre}`}
      accion="Guardar justificación"
      enviando={enviando}
      alCerrar={alCerrar}
      alEnviar={guardar}
    >
      <Typography>{inconsistencia.detalle}</Typography>
      <Avisos aviso={aviso} campos={['texto']} />
      <TextField
        label="Justificación"
        value={texto}
        onChange={(evento) => {
          setFalta(false);
          setTexto(evento.target.value);
        }}
        multiline
        minRows={3}
        required
        error={falta || Boolean(detalleDe(aviso, 'texto'))}
        helperText={falta ? `${TEXTO_OBLIGATORIO} ${pista}` : ayuda(aviso, 'texto', pista)}
        slotProps={{ htmlInput: { maxLength: LARGO_MAXIMO } }}
      />
    </Marco>
  );
}

function EnlaceALaPantalla({
  inconsistencia,
  nombre,
  permisos,
}: {
  inconsistencia: InconsistenciaDelPanel;
  nombre: string;
  permisos: readonly string[] | undefined;
}) {
  const destino = PANTALLAS[inconsistencia.pantalla];
  if (!tieneAlguno(permisos, destino.permisos)) {
    return null;
  }
  return (
    <Button
      component={RouterLink}
      to={destino.ruta}
      size="small"
      aria-label={`${destino.texto}: ${nombre}`}
    >
      {destino.texto}
    </Button>
  );
}

function Tabla({
  titulo,
  filas,
  nombreDe,
  puedeJustificar,
  permisos,
  alJustificar,
}: {
  titulo: string;
  filas: InconsistenciaDelPanel[];
  nombreDe: (una: InconsistenciaDelPanel) => string;
  puedeJustificar: boolean;
  permisos: readonly string[] | undefined;
  alJustificar: (una: InconsistenciaDelPanel) => void;
}) {
  return (
    <TableContainer sx={{ mb: 3 }}>
      <Table aria-label={titulo} size="small">
        <TableHead>
          <TableRow>
            <TableCell>Regla</TableCell>
            <TableCell>Severidad</TableCell>
            <TableCell>Qué afecta</TableCell>
            <TableCell>Qué pasa y qué hacer</TableCell>
            <TableCell>Acciones</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {filas.map((una) => {
            const nombre = nombreDe(una);
            return (
              <TableRow key={`${una.tipo}/${una.objeto.clave}`}>
                <TableCell>{una.regla}</TableCell>
                <TableCell>
                  <Chip
                    size="small"
                    variant="outlined"
                    color={una.severidad === 'DURA' ? 'error' : 'default'}
                    label={una.severidad === 'DURA' ? 'Dura' : 'Blanda'}
                  />
                </TableCell>
                <TableCell>{nombre}</TableCell>
                <TableCell>
                  <Typography variant="body2">{una.detalle}</Typography>
                  <Typography variant="body2" color="text.secondary">
                    {una.indicacion}
                  </Typography>
                  {una.justificacion && (
                    <>
                      <Typography variant="body2" sx={{ mt: 1 }}>
                        {una.justificacion.texto}
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        {`${una.justificacion.por} · ${new Date(una.justificacion.en).toLocaleString('es-EC')}`}
                      </Typography>
                    </>
                  )}
                </TableCell>
                <TableCell>
                  <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap' }}>
                    <EnlaceALaPantalla inconsistencia={una} nombre={nombre} permisos={permisos} />
                    {puedeJustificar && sePuedeJustificar(una) && (
                      <Button
                        size="small"
                        aria-label={`${una.justificacion ? 'Cambiar la justificación' : 'Justificar'}: ${nombre}`}
                        onClick={() => alJustificar(una)}
                      >
                        {una.justificacion ? 'Cambiar justificación' : 'Justificar'}
                      </Button>
                    )}
                  </Stack>
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </TableContainer>
  );
}

export function Inconsistencias() {
  useTitulo('Panel de inconsistencias');
  const sesion = useSesionActual();
  const permisos = sesion.data?.permisos;
  const consultas = useQueryClient();
  const periodos = usePeriodos();
  const carreras = useCarreras();
  const [elegido, setElegido] = useState<string | null>(null);
  const [porJustificar, setPorJustificar] = useState<InconsistenciaDelPanel | null>(null);
  // Por omisión, el más reciente en Planificación; si no hay, el más reciente.
  const porOmision =
    periodos.data?.find((periodo) => periodo.estado === 'PLANIFICACION')?.codigo ??
    periodos.data?.[0]?.codigo;
  const periodo = elegido ?? porOmision;
  const panel = usePanel(periodo);

  const nombreDe = (una: InconsistenciaDelPanel) => {
    if (una.objeto.clase === 'CARRERA') {
      return (
        carreras.data?.find((carrera) => carrera.codigo === una.objeto.clave)?.nombre ??
        una.objeto.nombre
      );
    }
    return una.objeto.nombre;
  };

  const datos = panel.data;
  // #215, D-99: desde la revisión del Vicerrectorado ya no se justifica.
  const puedeJustificar =
    tieneAlguno(permisos, ESCRITURA_DEL_PANEL) && Boolean(datos?.modificable && !datos.enRevision);
  const abiertas = datos?.inconsistencias.filter((una) => una.estado === 'ABIERTA') ?? [];
  const justificadas = datos?.inconsistencias.filter((una) => una.estado === 'JUSTIFICADA') ?? [];

  const terminar = async () => {
    await consultas.invalidateQueries({ queryKey: CLAVE_PANEL });
    setPorJustificar(null);
  };

  const tabla = {
    nombreDe,
    puedeJustificar,
    permisos,
    alJustificar: setPorJustificar,
  };

  return (
    <>
      <Typography component="h1" variant="h4" gutterBottom>
        Panel de inconsistencias
      </Typography>
      {periodos.error && <AvisoDeError mensaje={traducirError(periodos.error)} />}
      {panel.error && <AvisoDeError mensaje={traducirError(panel.error)} />}
      {periodos.data && (
        <TextField
          select
          label="Periodo"
          value={periodo ?? ''}
          onChange={(evento) => setElegido(evento.target.value)}
          slotProps={SELECCION}
          sx={{ my: 1, minWidth: 280, display: 'block' }}
        >
          {periodos.data.length === 0 && <option value="">No hay periodos</option>}
          {periodos.data.map((uno) => (
            <option key={uno.codigo} value={uno.codigo}>
              {`${uno.codigo} · ${uno.nombre} · ${NOMBRE_DEL_ESTADO[uno.estado]}`}
            </option>
          ))}
        </TextField>
      )}
      {datos && !datos.creado && (
        <Alert severity="info" role="status" sx={{ my: 2 }}>
          Este periodo todavía no tiene distributivo: no hay nada que evaluar.
        </Alert>
      )}
      {datos?.creado && (
        <>
          {datos.enRevision && (
            <Alert severity="info" role="status" sx={{ my: 2 }}>
              El distributivo está en revisión del Vicerrectorado: el consolidado ya no cambia.
            </Alert>
          )}
          {abiertas.length === 0 ? (
            <Alert severity="success" role="status" sx={{ my: 2 }}>
              No hay inconsistencias abiertas.
            </Alert>
          ) : (
            <>
              <Typography component="h2" variant="h5" sx={{ mt: 2 }}>
                {totales(datos.abiertas.duras, datos.abiertas.blandas)}
              </Typography>
              <Tabla titulo="Inconsistencias abiertas" filas={abiertas} {...tabla} />
            </>
          )}
          {justificadas.length > 0 && (
            <>
              <Typography component="h2" variant="h5" sx={{ mt: 2 }}>
                Justificadas
              </Typography>
              <Tabla titulo="Inconsistencias justificadas" filas={justificadas} {...tabla} />
            </>
          )}
        </>
      )}
      {datos && (
        <Alert severity="info" role="status" sx={{ my: 2 }}>
          {`El panel todavía no evalúa ${datos.sinEvaluar.join(', ')}: puede salir vacío aunque alguna se incumpla.`}
        </Alert>
      )}
      {porJustificar && periodo && (
        <DialogoDeJustificacion
          periodo={periodo}
          inconsistencia={porJustificar}
          nombre={nombreDe(porJustificar)}
          alCerrar={() => setPorJustificar(null)}
          alGuardar={terminar}
        />
      )}
    </>
  );
}
