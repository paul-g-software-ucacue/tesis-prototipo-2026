// La pantalla «Oferta del periodo» (F03-H03, RF-15): la oferta del periodo elegido, por carrera,
// con las horas a cubrir de cada asignatura (la suma de sus paralelos ofertados), de cada carrera y
// del periodo. La ven quienes leen la oferta, el distributivo o los catálogos; con «Catálogos y
// datos maestros: escritura» o «Distributivo del periodo: escritura», y solo con el periodo en
// Planificación, se oferta, se corrige, se retira y se vuelve a ofertar (D-70). Nada se borra; el
// servidor valida cada operación. Quien puede leer un docente ve, en cada fila ofertada y en
// cualquier estado del periodo, la sugerencia de docentes (F06-H02). Cada fila muestra su propuesta,
// y cada carrera cuántas lleva de cuántas; quien escribe la oferta propone y quita (F06-H03).
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
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
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError, type MensajeDeError } from '../../errores/traducir';
import { useTitulo } from '../../navegacion/useTitulo';
import {
  ESCRITURA_DE_LAS_CARRERAS,
  ESCRITURA_DE_LA_PROPUESTA,
  LECTURA_DE_LOS_DOCENTES,
} from '../../sesion/funcionalidades';
import { tieneAlguno } from '../../sesion/permisos';
import { useSesionActual } from '../../sesion/useSesionActual';
import { useCarreras } from '../carreras/consultas';
import { DialogoConfirmar } from '../carreras/DialogosDeCarrera';
import { NOMBRE_DE_LA_JORNADA, NOMBRE_DE_LA_MODALIDAD, horas } from '../carreras/nombres';
import { SELECCION } from '../comun/formulario';
import { useUnidades } from '../estructura/consultas';
import { usePeriodos } from '../periodos/consultas';
import { NOMBRE_DEL_ESTADO } from '../periodos/estados';
import {
  CLAVE_OFERTA,
  NOMBRE_DE_LA_JUSTIFICACION,
  cambiarEstadoAsignaturaParalelo,
  quitarPropuesta,
  useOferta,
  useParciales,
  type AsignaturaParalelo,
  type OfertaDelPeriodo,
  type ParcialDeCarrera,
  type Propuesta,
} from './consultas';
import { DialogoCorregir, DialogoOfertar } from './DialogosDeOferta';
import { SugerenciaDelParalelo } from './SugerenciaDelParalelo';

/** Lo que está abierto: un diálogo, para ofertar o sobre una asignatura-paralelo. */
type Abierto =
  | { que: 'ofertar' }
  | {
      que: 'corregir' | 'retirar' | 'sugerencia' | 'quitar';
      asignaturaParalelo: AsignaturaParalelo;
    }
  | null;

function paralelos(cantidad: number) {
  return cantidad === 1 ? '1 paralelo' : `${String(cantidad)} paralelos`;
}

export function Oferta() {
  useTitulo('Oferta del periodo');
  const sesion = useSesionActual();
  const escritura = tieneAlguno(sesion.data?.permisos, ESCRITURA_DE_LAS_CARRERAS);
  const puedeVerSugerencia = tieneAlguno(sesion.data?.permisos, LECTURA_DE_LOS_DOCENTES);
  const consultas = useQueryClient();
  const periodos = usePeriodos();
  const [elegido, setElegido] = useState<string | null>(null);
  // Por omisión, el más reciente en Planificación; si no hay, el más reciente.
  const porOmision =
    periodos.data?.find((periodo) => periodo.estado === 'PLANIFICACION')?.codigo ??
    periodos.data?.[0]?.codigo;
  const periodo = elegido ?? porOmision;
  const oferta = useOferta(periodo);
  const parciales = useParciales(periodo);
  const carreras = useCarreras();
  const unidades = useUnidades();
  const [abierto, setAbierto] = useState<Abierto>(null);
  const [aviso, setAviso] = useState<MensajeDeError | null>(null);
  const puedeCambiar = escritura && Boolean(oferta.data?.modificable);
  const puedeProponer =
    tieneAlguno(sesion.data?.permisos, ESCRITURA_DE_LA_PROPUESTA) &&
    oferta.data?.estadoDelPeriodo !== 'CERRADO';

  const terminar = async () => {
    await consultas.invalidateQueries({ queryKey: CLAVE_OFERTA });
    setAbierto(null);
  };

  const cambiarEstado = async (
    asignaturaParalelo: AsignaturaParalelo,
    estado: AsignaturaParalelo['estado'],
  ) => {
    setAviso(null);
    setAbierto(null);
    try {
      await cambiarEstadoAsignaturaParalelo(asignaturaParalelo, estado);
      await consultas.invalidateQueries({ queryKey: CLAVE_OFERTA });
    } catch (fallo) {
      setAviso(traducirError(fallo));
    }
  };

  const quitar = async (asignaturaParalelo: AsignaturaParalelo) => {
    setAviso(null);
    setAbierto(null);
    try {
      await quitarPropuesta(asignaturaParalelo);
      await consultas.invalidateQueries({ queryKey: CLAVE_OFERTA });
    } catch (fallo) {
      setAviso(traducirError(fallo));
    }
  };

  return (
    <>
      <Typography component="h1" variant="h4" gutterBottom>
        Oferta del periodo
      </Typography>
      {periodos.error && <AvisoDeError mensaje={traducirError(periodos.error)} />}
      {oferta.error && <AvisoDeError mensaje={traducirError(oferta.error)} />}
      {parciales.error && !oferta.error && (
        <AvisoDeError mensaje={traducirError(parciales.error)} />
      )}
      {aviso && <AvisoDeError mensaje={aviso} />}
      {periodos.data && (
        <TextField
          select
          label="Periodo"
          value={periodo ?? ''}
          onChange={(evento) => setElegido(evento.target.value)}
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
      {oferta.data && (
        <>
          <Typography variant="h6" component="p" sx={{ mt: 1 }}>
            {`Horas a cubrir: ${horas(oferta.data.horasACubrir)} h`}
          </Typography>
          {!oferta.data.modificable && (
            <Alert severity="info" sx={{ my: 1 }}>
              {`El periodo está en ${NOMBRE_DEL_ESTADO[oferta.data.estadoDelPeriodo]}: la oferta solo cambia en Planificación.`}
            </Alert>
          )}
          {oferta.data.modificable && !escritura && (
            <Alert severity="info" sx={{ my: 1 }}>
              Solo lectura: tu rol no cambia la oferta.
            </Alert>
          )}
          {puedeCambiar && (
            <Button
              variant="outlined"
              sx={{ my: 1 }}
              onClick={() => setAbierto({ que: 'ofertar' })}
            >
              Ofertar asignatura
            </Button>
          )}
          {oferta.data.carreras.length === 0 && (
            <Typography sx={{ mt: 2 }}>No hay oferta para mostrar en este periodo.</Typography>
          )}
          {oferta.data.carreras.map((carrera) => (
            <OfertaDeCarrera
              key={carrera.carrera}
              carrera={carrera}
              oferta={oferta.data}
              parcial={parciales.data?.parciales.find((uno) => uno.carrera === carrera.carrera)}
              puedeCambiar={puedeCambiar}
              puedeVerSugerencia={puedeVerSugerencia}
              puedeProponer={puedeProponer}
              alAbrir={setAbierto}
              alVolverAOfertar={(una) => void cambiarEstado(una, 'OFERTADA')}
            />
          ))}
        </>
      )}
      {abierto?.que === 'ofertar' && periodo && (
        <DialogoOfertar
          periodo={periodo}
          carreras={carreras.data ?? []}
          unidades={unidades.data ?? []}
          alCerrar={() => setAbierto(null)}
          alGuardar={terminar}
        />
      )}
      {abierto?.que === 'corregir' && (
        <DialogoCorregir
          asignaturaParalelo={abierto.asignaturaParalelo}
          carrera={carreras.data?.find(
            (carrera) => carrera.codigo === abierto.asignaturaParalelo.carrera,
          )}
          unidades={unidades.data ?? []}
          alCerrar={() => setAbierto(null)}
          alGuardar={terminar}
        />
      )}
      {abierto?.que === 'sugerencia' && periodo && (
        <SugerenciaDelParalelo
          periodo={periodo}
          asignaturaParalelo={abierto.asignaturaParalelo}
          puedeProponer={puedeProponer}
          alCerrar={() => setAbierto(null)}
          alProponer={terminar}
        />
      )}
      {abierto?.que === 'quitar' && (
        <DialogoConfirmar
          titulo={`Quitar la propuesta de ${abierto.asignaturaParalelo.asignatura} ${abierto.asignaturaParalelo.paralelo}`}
          texto="La asignatura-paralelo queda sin docente propuesto, y el cambio queda registrado. Puedes volver a proponer."
          accion="Quitar"
          alCerrar={() => setAbierto(null)}
          alConfirmar={() => void quitar(abierto.asignaturaParalelo)}
        />
      )}
      {abierto?.que === 'retirar' && (
        <DialogoConfirmar
          titulo={`Retirar ${abierto.asignaturaParalelo.asignatura} de ${abierto.asignaturaParalelo.paralelo}`}
          texto="La asignatura-paralelo se sigue viendo, pero no suma horas a cubrir ni recibe propuestas. No se borra, y se puede volver a ofertar mientras el periodo esté en Planificación."
          accion="Retirar"
          alCerrar={() => setAbierto(null)}
          alConfirmar={() => void cambiarEstado(abierto.asignaturaParalelo, 'RETIRADA')}
        />
      )}
    </>
  );
}

/** Una carrera de la oferta: una fila por asignatura, con sus paralelos y sus horas a cubrir. */
function OfertaDeCarrera({
  carrera,
  oferta,
  parcial,
  puedeCambiar,
  puedeVerSugerencia,
  puedeProponer,
  alAbrir,
  alVolverAOfertar,
}: {
  carrera: OfertaDelPeriodo['carreras'][number];
  oferta: OfertaDelPeriodo;
  parcial: ParcialDeCarrera | undefined;
  puedeCambiar: boolean;
  puedeVerSugerencia: boolean;
  puedeProponer: boolean;
  alAbrir: (abierto: Abierto) => void;
  alVolverAOfertar: (asignaturaParalelo: AsignaturaParalelo) => void;
}) {
  const idDelTitulo = `titulo-oferta-${carrera.carrera}`;
  return (
    <Box component="section" aria-labelledby={idDelTitulo} sx={{ mt: 3 }}>
      <Typography id={idDelTitulo} component="h2" variant="h5">
        {`${carrera.nombre} · ${horas(carrera.horasACubrir)} h a cubrir`}
      </Typography>
      {parcial && (
        <Stack direction="row" spacing={1} sx={{ alignItems: 'center', my: 0.5 }}>
          <Typography>
            {`${String(parcial.conPropuesta)} de ${String(parcial.ofertadas)} propuestas`}
          </Typography>
          {parcial.completo && (
            <Chip size="small" color="success" variant="outlined" label="Parcial completo" />
          )}
        </Stack>
      )}
      <TableContainer>
        <Table aria-label={`Oferta de ${carrera.nombre}`} size="small">
          <TableHead>
            <TableRow>
              <TableCell>Ciclo</TableCell>
              <TableCell>Código</TableCell>
              <TableCell>Asignatura</TableCell>
              <TableCell>Paralelos</TableCell>
              <TableCell>Horas a cubrir</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {carrera.asignaturas.map((asignatura) => (
              <TableRow key={asignatura.asignatura}>
                <TableCell>{asignatura.ciclo}</TableCell>
                <TableCell>{asignatura.asignatura}</TableCell>
                <TableCell>{asignatura.nombre}</TableCell>
                <TableCell>
                  <Stack spacing={0.5}>
                    {oferta.asignaturasParalelo
                      .filter((una) => una.asignatura === asignatura.asignatura)
                      .map((una) => (
                        <Paralelo
                          key={una.paralelo}
                          asignaturaParalelo={una}
                          propuesta={parcial?.propuestas.find(
                            (propuesta) =>
                              propuesta.asignatura === una.asignatura &&
                              propuesta.paralelo === una.paralelo,
                          )}
                          puedeCambiar={puedeCambiar}
                          puedeVerSugerencia={puedeVerSugerencia}
                          puedeProponer={puedeProponer}
                          alAbrir={alAbrir}
                          alVolverAOfertar={alVolverAOfertar}
                        />
                      ))}
                  </Stack>
                </TableCell>
                <TableCell>
                  {`${horas(asignatura.horasACubrir)} h`}
                  <Typography variant="body2" color="text.secondary">
                    {paralelos(asignatura.paralelos)}
                  </Typography>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  );
}

/** La propuesta de una fila, con su justificación y sus avisos. */
function PropuestaDeLaFila({ propuesta }: { propuesta: Propuesta }) {
  return (
    <>
      <Typography variant="body2">
        {`Propuesto: ${propuesta.nombreDocente} · ${NOMBRE_DE_LA_JUSTIFICACION[propuesta.justificacion]}`}
      </Typography>
      {propuesta.texto && (
        <Typography variant="body2" color="text.secondary">
          {propuesta.texto}
        </Typography>
      )}
      {propuesta.retirada && (
        <Typography variant="body2" color="error">
          Paralelo retirado: no cuenta para el parcial.
        </Typography>
      )}
      {!propuesta.retirada && !propuesta.horasSuficientes && (
        <Typography variant="body2" color="error">
          Sin horas suficientes
        </Typography>
      )}
    </>
  );
}

function Paralelo({
  asignaturaParalelo: una,
  propuesta,
  puedeCambiar,
  puedeVerSugerencia,
  puedeProponer,
  alAbrir,
  alVolverAOfertar,
}: {
  asignaturaParalelo: AsignaturaParalelo;
  propuesta: Propuesta | undefined;
  puedeCambiar: boolean;
  puedeVerSugerencia: boolean;
  puedeProponer: boolean;
  alAbrir: (abierto: Abierto) => void;
  alVolverAOfertar: (asignaturaParalelo: AsignaturaParalelo) => void;
}) {
  const retirada = una.estado === 'RETIRADA';
  const puedeQuitar = puedeProponer && Boolean(propuesta);
  const texto = [
    una.paralelo,
    NOMBRE_DE_LA_JORNADA[una.jornada],
    NOMBRE_DE_LA_MODALIDAD[una.modalidad],
    `${String(una.estudiantes)} estudiantes`,
    `${horas(una.horasSemanales)} h`,
    ...(una.sedeIndicada ? [`sede ${una.sede}`] : []),
    ...(retirada ? ['Retirada'] : []),
  ].join(' · ');
  return (
    <Box>
      <Typography
        variant="body2"
        component="span"
        color={retirada ? 'text.secondary' : 'text.primary'}
      >
        {texto}
      </Typography>
      {una.porEncimaDeLaAsignatura && (
        <Typography variant="body2" color="error">
          {`Pasa las ${horas(una.horasDeLaAsignatura)} h de la asignatura: corrígela.`}
        </Typography>
      )}
      {propuesta && <PropuestaDeLaFila propuesta={propuesta} />}
      {(puedeCambiar || puedeQuitar || (puedeVerSugerencia && !retirada)) && (
        <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap' }}>
          {puedeVerSugerencia && !retirada && (
            <Button
              size="small"
              aria-label={`Sugerencia de ${una.asignatura} ${una.paralelo}`}
              onClick={() => alAbrir({ que: 'sugerencia', asignaturaParalelo: una })}
            >
              Sugerencia
            </Button>
          )}
          {puedeQuitar && (
            <Button
              size="small"
              aria-label={`Quitar propuesta de ${una.asignatura} ${una.paralelo}`}
              onClick={() => alAbrir({ que: 'quitar', asignaturaParalelo: una })}
            >
              Quitar propuesta
            </Button>
          )}
          {puedeCambiar && retirada && (
            <Button
              size="small"
              aria-label={`Volver a ofertar ${una.asignatura} ${una.paralelo}`}
              onClick={() => alVolverAOfertar(una)}
            >
              Volver a ofertar
            </Button>
          )}
          {puedeCambiar && !retirada && (
            <>
              <Button
                size="small"
                aria-label={`Corregir ${una.asignatura} ${una.paralelo}`}
                onClick={() => alAbrir({ que: 'corregir', asignaturaParalelo: una })}
              >
                Corregir
              </Button>
              <Button
                size="small"
                aria-label={`Retirar ${una.asignatura} ${una.paralelo}`}
                onClick={() => alAbrir({ que: 'retirar', asignaturaParalelo: una })}
              >
                Retirar
              </Button>
            </>
          )}
        </Stack>
      )}
    </Box>
  );
}
