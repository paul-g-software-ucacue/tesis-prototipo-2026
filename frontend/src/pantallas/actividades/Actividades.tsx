// La pantalla «Actividades no lectivas» (F06-H04, RF-26): por carrera y docente, las actividades del
// parcial con su suma por componente. La ven quienes leen la oferta; quien escribe la oferta asigna y
// quita en su carrera, salvo con el periodo cerrado. El panel completo de totales y la proporción
// 60/40 son de F07-H01.
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
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError, type MensajeDeError } from '../../errores/traducir';
import { useTitulo } from '../../navegacion/useTitulo';
import { ESCRITURA_DE_LA_PROPUESTA } from '../../sesion/funcionalidades';
import { tieneAlguno } from '../../sesion/permisos';
import { useSesionActual } from '../../sesion/useSesionActual';
import { useCarreras } from '../carreras/consultas';
import { DialogoConfirmar } from '../carreras/DialogosDeCarrera';
import { horas } from '../carreras/nombres';
import { CLAVE_CARGA_HORARIA, porComponente } from '../comun/cargaHoraria';
import { SELECCION } from '../comun/formulario';
import { PanelDeCarga } from '../comun/PanelDeCarga';
import { usePeriodos } from '../periodos/consultas';
import { NOMBRE_DEL_ESTADO } from '../periodos/estados';
import {
  CLAVE_ACTIVIDADES,
  literal,
  quitarActividad,
  useActividades,
  type ActividadAsignada,
  type ActividadesDeCarrera,
  type ActividadesDelDocente,
} from './consultas';
import { DialogoAsignar } from './DialogoAsignar';

/** Lo que está abierto: asignar en una carrera, o quitar una actividad. */
type Abierto =
  | { que: 'asignar'; carrera: string }
  | {
      que: 'quitar';
      carrera: string;
      docente: ActividadesDelDocente;
      actividad: ActividadAsignada;
    }
  | null;

/** El docente del panel: el último que se guardó o se quitó, en un periodo. */
interface DocenteALaVista {
  periodo: string;
  docente: string;
  guardada: boolean;
}

export function Actividades() {
  useTitulo('Actividades no lectivas');
  const sesion = useSesionActual();
  const consultas = useQueryClient();
  const periodos = usePeriodos();
  const carreras = useCarreras();
  const [elegido, setElegido] = useState<string | null>(null);
  const [abierto, setAbierto] = useState<Abierto>(null);
  const [fallo, setFallo] = useState<MensajeDeError | null>(null);
  const [aLaVista, setALaVista] = useState<DocenteALaVista | null>(null);
  // Por omisión, el más reciente en Planificación; si no hay, el más reciente.
  const porOmision =
    periodos.data?.find((periodo) => periodo.estado === 'PLANIFICACION')?.codigo ??
    periodos.data?.[0]?.codigo;
  const periodo = elegido ?? porOmision;
  const estado = periodos.data?.find((uno) => uno.codigo === periodo)?.estado;
  const actividades = useActividades(periodo);
  const puedeEscribir =
    tieneAlguno(sesion.data?.permisos, ESCRITURA_DE_LA_PROPUESTA) && estado !== 'CERRADO';

  const recargar = () =>
    Promise.all([
      consultas.invalidateQueries({ queryKey: CLAVE_ACTIVIDADES }),
      consultas.invalidateQueries({ queryKey: CLAVE_CARGA_HORARIA }),
    ]);

  const quitar = async (
    carrera: string,
    docente: ActividadesDelDocente,
    actividad: ActividadAsignada,
  ) => {
    setFallo(null);
    setAbierto(null);
    try {
      await quitarActividad(
        {
          periodo: periodo ?? '',
          carrera,
          docente: docente.docente,
          actividad: actividad.actividad,
        },
        actividad.proyecto,
      );
      setALaVista({ periodo: periodo ?? '', docente: docente.docente, guardada: false });
      await recargar();
    } catch (error) {
      setFallo(traducirError(error));
    }
  };

  const nombreDe = (carrera: string) =>
    carreras.data?.find((una) => una.codigo === carrera)?.nombre ?? carrera;

  return (
    <>
      <Typography component="h1" variant="h4" gutterBottom>
        Actividades no lectivas
      </Typography>
      {periodos.error && <AvisoDeError mensaje={traducirError(periodos.error)} />}
      {actividades.error && <AvisoDeError mensaje={traducirError(actividades.error)} />}
      {fallo && <AvisoDeError mensaje={fallo} />}
      {periodos.data && (
        <TextField
          select
          label="Periodo"
          value={periodo ?? ''}
          onChange={(evento) => {
            setFallo(null);
            setElegido(evento.target.value);
          }}
          slotProps={SELECCION}
          sx={{ my: 1, minWidth: 280 }}
        >
          {periodos.data.length === 0 && <option value="">No hay periodos</option>}
          {periodos.data.map((uno) => (
            <option key={uno.codigo} value={uno.codigo}>
              {`${uno.codigo} · ${uno.nombre} · ${NOMBRE_DEL_ESTADO[uno.estado]}`}
            </option>
          ))}
        </TextField>
      )}
      {aLaVista && aLaVista.periodo === periodo && (
        <Box sx={{ my: 1 }}>
          <PanelDeCarga
            periodo={aLaVista.periodo}
            docente={aLaVista.docente}
            guardada={aLaVista.guardada}
          />
        </Box>
      )}
      {actividades.data && !actividades.data.creado && (
        <Alert severity="info" sx={{ my: 1 }}>
          El periodo todavía no tiene distributivo. Pide al Vicerrector académico que lo cree.
        </Alert>
      )}
      {actividades.data?.creado &&
        actividades.data.carreras.map((carrera) => (
          <ActividadesDeLaCarrera
            key={carrera.carrera}
            carrera={carrera}
            nombre={nombreDe(carrera.carrera)}
            puedeEscribir={puedeEscribir}
            alAbrir={setAbierto}
          />
        ))}
      {abierto?.que === 'asignar' && periodo && (
        <DialogoAsignar
          periodo={periodo}
          carrera={abierto.carrera}
          alCerrar={() => setAbierto(null)}
          alGuardar={async (docente) => {
            setFallo(null);
            setALaVista({ periodo, docente, guardada: true });
            await recargar();
            setAbierto(null);
          }}
        />
      )}
      {abierto?.que === 'quitar' && (
        <DialogoConfirmar
          titulo={`Quitar ${abierto.actividad.actividad} de ${abierto.docente.nombreDocente}`}
          texto="La actividad deja de contar en las horas del docente, y el cambio queda registrado."
          accion="Quitar"
          alCerrar={() => setAbierto(null)}
          alConfirmar={() => void quitar(abierto.carrera, abierto.docente, abierto.actividad)}
        />
      )}
    </>
  );
}

/** Una carrera: una fila por docente, con sus actividades aquí y sus totales en todas las carreras. */
function ActividadesDeLaCarrera({
  carrera,
  nombre,
  puedeEscribir,
  alAbrir,
}: {
  carrera: ActividadesDeCarrera;
  nombre: string;
  puedeEscribir: boolean;
  alAbrir: (abierto: Abierto) => void;
}) {
  const idDelTitulo = `titulo-actividades-${carrera.carrera}`;
  return (
    <Box component="section" aria-labelledby={idDelTitulo} sx={{ mt: 3 }}>
      <Typography id={idDelTitulo} component="h2" variant="h5">
        {nombre}
      </Typography>
      {puedeEscribir && (
        <Button
          variant="outlined"
          sx={{ my: 1 }}
          onClick={() => alAbrir({ que: 'asignar', carrera: carrera.carrera })}
        >
          Asignar actividad
        </Button>
      )}
      {carrera.docentes.length === 0 ? (
        <Typography sx={{ mt: 1 }}>Todavía no hay actividades en este parcial.</Typography>
      ) : (
        <TableContainer>
          <Table aria-label={`Actividades de ${nombre}`} size="small">
            <TableHead>
              <TableRow>
                <TableCell>Docente</TableCell>
                <TableCell>Actividades</TableCell>
                <TableCell>Totales en todas las carreras</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {carrera.docentes.map((docente) => (
                <TableRow key={docente.docente}>
                  <TableCell>{docente.nombreDocente}</TableCell>
                  <TableCell>
                    <Stack spacing={1}>
                      {docente.actividades.map((actividad) => (
                        <Box key={`${actividad.actividad}-${actividad.proyecto ?? ''}`}>
                          <Typography variant="body2">
                            {`${literal(actividad.actividad)} ${actividad.nombre} · ${horas(actividad.horas)} h`}
                          </Typography>
                          {actividad.nombreProyecto && (
                            <Typography variant="body2" color="text.secondary">
                              {`Proyecto: ${actividad.nombreProyecto}`}
                            </Typography>
                          )}
                          {puedeEscribir && (
                            <Button
                              size="small"
                              aria-label={`Quitar ${actividad.actividad} de ${docente.nombreDocente}`}
                              onClick={() =>
                                alAbrir({
                                  que: 'quitar',
                                  carrera: carrera.carrera,
                                  docente,
                                  actividad,
                                })
                              }
                            >
                              Quitar
                            </Button>
                          )}
                        </Box>
                      ))}
                    </Stack>
                  </TableCell>
                  <TableCell>
                    {porComponente(docente.totales)
                      .filter(([, valor]) => valor > 0)
                      .map(([componente, valor]) => (
                        <Typography key={componente} variant="body2">
                          {`${componente}: ${horas(valor)} h`}
                        </Typography>
                      ))}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  );
}
