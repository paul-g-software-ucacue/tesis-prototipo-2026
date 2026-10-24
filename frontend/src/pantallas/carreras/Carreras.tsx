// La pantalla «Carreras y mallas» (F03-H02, RF-12, RF-14): las carreras con su sede, su nivel, su
// modalidad, sus jornadas, su campo y su estado, y sus mallas con las horas semanales de clase. Al
// elegir una malla se ven sus asignaturas por ciclo, con sus horas y los datos para la afinidad
// (RN-19, RN-24). La ven quienes leen el distributivo o los catálogos; con «Catálogos y datos
// maestros: escritura» o «Distributivo del periodo: escritura» se registran, corrigen y cambian de
// estado (D-70). Nada se borra; el servidor valida cada operación.
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
import { useState } from 'react';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError, type MensajeDeError } from '../../errores/traducir';
import { useTitulo } from '../../navegacion/useTitulo';
import { ESCRITURA_DE_LAS_CARRERAS } from '../../sesion/funcionalidades';
import { tieneAlguno } from '../../sesion/permisos';
import { useSesionActual } from '../../sesion/useSesionActual';
import { useUnidades } from '../estructura/consultas';
import {
  CLAVE_CARRERAS,
  cambiarEstadoAsignatura,
  cambiarEstadoMalla,
  useAsignaturas,
  useCamposAmplios,
  useCarreras,
  type Asignatura,
  type CampoAmplio,
  type Carrera,
  type ResumenDeMalla,
} from './consultas';
import {
  DialogoConfirmar,
  DialogoDatosDeAsignatura,
  DialogoDatosDeCarrera,
  DialogoEstadoDeCarrera,
  DialogoMalla,
} from './DialogosDeCarrera';
import {
  NOMBRE_DE_LA_MODALIDAD,
  NOMBRE_DE_LA_UNIDAD,
  NOMBRE_DEL_ESTADO_DE_CARRERA,
  NOMBRE_DEL_ESTADO_DE_MALLA,
  NOMBRE_DEL_NIVEL,
  horas,
  jornadasEnTexto,
} from './nombres';

/** Lo que está abierto: un diálogo, sobre una carrera, una malla o una asignatura. */
type Abierto =
  | { que: 'registrar-carrera' }
  | { que: 'corregir-carrera' | 'estado-carrera' | 'registrar-malla'; carrera: Carrera }
  | { que: 'estado-malla'; malla: ResumenDeMalla }
  | { que: 'registrar-asignatura' }
  | { que: 'corregir-asignatura' | 'estado-asignatura'; asignatura: Asignatura }
  | null;

/** La malla elegida: su carrera y su código. */
interface Elegida {
  carrera: string;
  malla: string;
}

export function Carreras() {
  useTitulo('Carreras y mallas');
  const sesion = useSesionActual();
  const escritura = tieneAlguno(sesion.data?.permisos, ESCRITURA_DE_LAS_CARRERAS);
  const consultas = useQueryClient();
  const carreras = useCarreras();
  const campos = useCamposAmplios();
  const unidades = useUnidades();
  const [elegida, setElegida] = useState<Elegida | null>(null);
  const [abierto, setAbierto] = useState<Abierto>(null);
  const [aviso, setAviso] = useState<MensajeDeError | null>(null);
  const carreraElegida = carreras.data?.find((carrera) => carrera.codigo === elegida?.carrera);
  const mallaElegida = carreraElegida?.mallas.find((malla) => malla.codigo === elegida?.malla);
  const nombreDelCampo = (codigo: string) =>
    campos.data?.find((campo) => campo.codigo === codigo)?.nombre;

  const terminar = async () => {
    await consultas.invalidateQueries({ queryKey: CLAVE_CARRERAS });
    setAbierto(null);
  };

  const cambiar = async (operacion: () => Promise<unknown>) => {
    setAviso(null);
    setAbierto(null);
    try {
      await operacion();
      await consultas.invalidateQueries({ queryKey: CLAVE_CARRERAS });
    } catch (fallo) {
      setAviso(traducirError(fallo));
    }
  };

  return (
    <>
      <Typography component="h1" variant="h4" gutterBottom>
        Carreras y mallas
      </Typography>
      {carreras.error && <AvisoDeError mensaje={traducirError(carreras.error)} />}
      {campos.error && <AvisoDeError mensaje={traducirError(campos.error)} />}
      {aviso && <AvisoDeError mensaje={aviso} />}
      {escritura && (
        <Button
          variant="outlined"
          sx={{ my: 1 }}
          onClick={() => setAbierto({ que: 'registrar-carrera' })}
        >
          Registrar carrera
        </Button>
      )}
      {carreras.data && (
        <TableContainer>
          <Table aria-label="Carreras" size="small">
            <TableHead>
              <TableRow>
                <TableCell>Código</TableCell>
                <TableCell>Carrera</TableCell>
                <TableCell>Sede</TableCell>
                <TableCell>Formación</TableCell>
                <TableCell>Campo</TableCell>
                <TableCell>Estado</TableCell>
                <TableCell>Mallas</TableCell>
                {escritura && <TableCell>Acciones</TableCell>}
              </TableRow>
            </TableHead>
            <TableBody>
              {carreras.data.map((carrera) => (
                <TableRow key={carrera.codigo}>
                  <TableCell>{carrera.codigo}</TableCell>
                  <TableCell>
                    {carrera.nombre}
                    <Typography variant="body2" color="text.secondary">
                      {`${carrera.siglas} · ${String(carrera.numeroCiclos)} ciclos`}
                    </Typography>
                  </TableCell>
                  <TableCell>{carrera.sede}</TableCell>
                  <TableCell>
                    {NOMBRE_DEL_NIVEL[carrera.nivelFormacion]}
                    <Typography variant="body2" color="text.secondary">
                      {`${NOMBRE_DE_LA_MODALIDAD[carrera.modalidad]} · ${jornadasEnTexto(carrera.jornadas)}`}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    {[carrera.campoAmplio, nombreDelCampo(carrera.campoAmplio)]
                      .filter(Boolean)
                      .join(' · ')}
                    <Typography variant="body2" color="text.secondary">
                      {[carrera.campoEspecifico, carrera.campoDetallado]
                        .filter(Boolean)
                        .join(' · ')}
                    </Typography>
                  </TableCell>
                  <TableCell>{NOMBRE_DEL_ESTADO_DE_CARRERA[carrera.estado]}</TableCell>
                  <TableCell>
                    {carrera.mallas.length === 0 && 'Sin mallas'}
                    {carrera.mallas.map((malla) => (
                      <Box key={malla.codigo} sx={{ whiteSpace: 'nowrap' }}>
                        {`${malla.codigo} · ${NOMBRE_DEL_ESTADO_DE_MALLA[malla.estado]} · ${horas(malla.horasSemanalesClase)} h`}
                        <Button
                          size="small"
                          aria-label={`Ver la malla ${malla.codigo}`}
                          aria-pressed={elegida?.malla === malla.codigo}
                          onClick={() =>
                            setElegida({ carrera: carrera.codigo, malla: malla.codigo })
                          }
                        >
                          Ver
                        </Button>
                      </Box>
                    ))}
                  </TableCell>
                  {escritura && (
                    <TableCell>
                      <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap' }}>
                        <Button
                          size="small"
                          aria-label={`Corregir ${carrera.codigo}`}
                          onClick={() => setAbierto({ que: 'corregir-carrera', carrera })}
                        >
                          Corregir
                        </Button>
                        <Button
                          size="small"
                          aria-label={`Cambiar el estado de ${carrera.nombre}`}
                          onClick={() => setAbierto({ que: 'estado-carrera', carrera })}
                        >
                          Estado
                        </Button>
                        <Button
                          size="small"
                          aria-label={`Registrar una malla de ${carrera.nombre}`}
                          onClick={() => setAbierto({ que: 'registrar-malla', carrera })}
                        >
                          Malla
                        </Button>
                      </Stack>
                    </TableCell>
                  )}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
      {carreraElegida && mallaElegida && (
        <DetalleDeMalla
          carrera={carreraElegida}
          malla={mallaElegida}
          escritura={escritura}
          alAbrir={setAbierto}
        />
      )}
      {abierto?.que === 'registrar-carrera' && (
        <DialogoDatosDeCarrera
          unidades={unidades.data ?? []}
          campos={campos.data ?? []}
          alCerrar={() => setAbierto(null)}
          alGuardar={terminar}
        />
      )}
      {abierto?.que === 'corregir-carrera' && (
        <DialogoDatosDeCarrera
          carrera={abierto.carrera}
          unidades={unidades.data ?? []}
          campos={campos.data ?? []}
          alCerrar={() => setAbierto(null)}
          alGuardar={terminar}
        />
      )}
      {abierto?.que === 'estado-carrera' && (
        <DialogoEstadoDeCarrera
          carrera={abierto.carrera}
          alCerrar={() => setAbierto(null)}
          alGuardar={terminar}
        />
      )}
      {abierto?.que === 'registrar-malla' && (
        <DialogoMalla
          carrera={abierto.carrera}
          alCerrar={() => setAbierto(null)}
          alGuardar={terminar}
        />
      )}
      {abierto?.que === 'estado-malla' && (
        <DialogoConfirmar
          {...(abierto.malla.estado === 'VIGENTE'
            ? {
                titulo: `Derogar la malla ${abierto.malla.codigo}`,
                texto:
                  'La malla conserva sus asignaturas, pero no admite nuevas. No se borra, y puede volver a estar vigente.',
                accion: 'Derogar',
              }
            : {
                titulo: `Poner vigente la malla ${abierto.malla.codigo}`,
                texto: 'La malla vuelve a admitir asignaturas nuevas.',
                accion: 'Poner vigente',
              })}
          alCerrar={() => setAbierto(null)}
          alConfirmar={() => {
            const { codigo, estado } = abierto.malla;
            void cambiar(() =>
              cambiarEstadoMalla(codigo, estado === 'VIGENTE' ? 'DEROGADA' : 'VIGENTE'),
            );
          }}
        />
      )}
      {abierto?.que === 'registrar-asignatura' && carreraElegida && mallaElegida && (
        <DialogoDatosDeAsignatura
          carrera={carreraElegida}
          malla={mallaElegida.codigo}
          campos={campos.data ?? []}
          alCerrar={() => setAbierto(null)}
          alGuardar={terminar}
        />
      )}
      {abierto?.que === 'corregir-asignatura' && carreraElegida && (
        <DialogoDatosDeAsignatura
          asignatura={abierto.asignatura}
          carrera={carreraElegida}
          malla={abierto.asignatura.malla}
          campos={campos.data ?? []}
          alCerrar={() => setAbierto(null)}
          alGuardar={terminar}
        />
      )}
      {abierto?.que === 'estado-asignatura' && (
        <DialogoConfirmar
          {...(abierto.asignatura.activa
            ? {
                titulo: `Inactivar la asignatura ${abierto.asignatura.codigo}`,
                texto:
                  'La asignatura queda en su malla como inactiva y no se ofrece para lo nuevo. No se borra, y se puede reactivar.',
                accion: 'Inactivar',
              }
            : {
                titulo: `Reactivar la asignatura ${abierto.asignatura.codigo}`,
                texto: 'La asignatura vuelve a estar activa. Su malla tiene que estar vigente.',
                accion: 'Reactivar',
              })}
          alCerrar={() => setAbierto(null)}
          alConfirmar={() => {
            const { codigo, activa } = abierto.asignatura;
            void cambiar(() => cambiarEstadoAsignatura(codigo, !activa));
          }}
        />
      )}
    </>
  );
}

/** Las asignaturas de la malla elegida, por ciclo, con las horas de las activas. */
function DetalleDeMalla({
  carrera,
  malla,
  escritura,
  alAbrir,
}: {
  carrera: Carrera;
  malla: ResumenDeMalla;
  escritura: boolean;
  alAbrir: (abierto: Abierto) => void;
}) {
  const asignaturas = useAsignaturas(carrera.codigo);
  const deLaMalla = (asignaturas.data ?? []).filter(
    (asignatura) => asignatura.malla === malla.codigo,
  );
  const ciclos = [...new Set(deLaMalla.map((asignatura) => asignatura.ciclo))].sort(
    (a, b) => a - b,
  );
  const idDelTitulo = `titulo-malla-${malla.codigo}`;

  return (
    <Box component="section" aria-labelledby={idDelTitulo} sx={{ mt: 4 }}>
      <Typography id={idDelTitulo} component="h2" variant="h5">
        {`Malla ${malla.codigo}`}
      </Typography>
      <Typography color="text.secondary">
        {`${carrera.nombre} · ${NOMBRE_DEL_ESTADO_DE_MALLA[malla.estado]} · ${String(malla.asignaturas)} asignaturas · ${horas(malla.horasSemanalesClase)} horas semanales de clase`}
      </Typography>
      {asignaturas.error && <AvisoDeError mensaje={traducirError(asignaturas.error)} />}
      {escritura && (
        <Stack direction="row" spacing={1} sx={{ my: 1 }}>
          {malla.estado === 'VIGENTE' && (
            <Button
              variant="outlined"
              aria-label={`Registrar asignatura en ${malla.codigo}`}
              onClick={() => alAbrir({ que: 'registrar-asignatura' })}
            >
              Registrar asignatura
            </Button>
          )}
          <Button
            aria-label={`${malla.estado === 'VIGENTE' ? 'Derogar' : 'Poner vigente'} ${malla.codigo}`}
            onClick={() => alAbrir({ que: 'estado-malla', malla })}
          >
            {malla.estado === 'VIGENTE' ? 'Derogar' : 'Poner vigente'}
          </Button>
        </Stack>
      )}
      {asignaturas.data && deLaMalla.length === 0 && (
        <Typography sx={{ mt: 2 }}>La malla todavía no tiene asignaturas.</Typography>
      )}
      {ciclos.map((ciclo) => (
        <CicloDeMalla
          key={ciclo}
          ciclo={ciclo}
          asignaturas={deLaMalla.filter((asignatura) => asignatura.ciclo === ciclo)}
          escritura={escritura}
          alAbrir={alAbrir}
        />
      ))}
    </Box>
  );
}

function CicloDeMalla({
  ciclo,
  asignaturas,
  escritura,
  alAbrir,
}: {
  ciclo: number;
  asignaturas: Asignatura[];
  escritura: boolean;
  alAbrir: (abierto: Abierto) => void;
}) {
  const horasDelCiclo = asignaturas
    .filter((asignatura) => asignatura.activa)
    .reduce((suma, asignatura) => suma + asignatura.horasSemanalesClase, 0);
  const campo = ({ codigo, nombre }: CampoAmplio) => `${codigo} · ${nombre}`;

  return (
    <>
      <Typography component="h3" variant="h6" sx={{ mt: 3 }}>
        {`Ciclo ${String(ciclo)} · ${horas(horasDelCiclo)} h`}
      </Typography>
      <TableContainer>
        <Table aria-label={`Ciclo ${String(ciclo)}`} size="small">
          <TableHead>
            <TableRow>
              <TableCell>Código</TableCell>
              <TableCell>Asignatura</TableCell>
              <TableCell>Horas</TableCell>
              <TableCell>Campo</TableCell>
              <TableCell>Palabras clave</TableCell>
              <TableCell>Estado</TableCell>
              {escritura && <TableCell>Acciones</TableCell>}
            </TableRow>
          </TableHead>
          <TableBody>
            {asignaturas.map((asignatura) => (
              <TableRow key={asignatura.codigo}>
                <TableCell>{asignatura.codigo}</TableCell>
                <TableCell>
                  {asignatura.nombre}
                  <Typography variant="body2" color="text.secondary">
                    {[
                      NOMBRE_DE_LA_UNIDAD[asignatura.unidadOrganizacionCurricular],
                      asignatura.troncoComun && 'Tronco común',
                    ]
                      .filter(Boolean)
                      .join(' · ')}
                  </Typography>
                </TableCell>
                <TableCell>
                  {`${horas(asignatura.horasSemanalesClase)} de clase`}
                  {asignatura.horasPracticasSemanales !== undefined && (
                    <Typography variant="body2" color="text.secondary">
                      {`${horas(asignatura.horasPracticasSemanales)} prácticas`}
                    </Typography>
                  )}
                </TableCell>
                <TableCell>
                  {campo(asignatura.campoAmplio)}
                  <Typography variant="body2" color="text.secondary">
                    {asignatura.campoEspecifico}
                  </Typography>
                </TableCell>
                <TableCell>{asignatura.palabrasClave.join('; ')}</TableCell>
                <TableCell>{asignatura.activa ? 'Activa' : 'Inactiva'}</TableCell>
                {escritura && (
                  <TableCell>
                    <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap' }}>
                      <Button
                        size="small"
                        aria-label={`Corregir ${asignatura.codigo}`}
                        onClick={() => alAbrir({ que: 'corregir-asignatura', asignatura })}
                      >
                        Corregir
                      </Button>
                      <Button
                        size="small"
                        aria-label={`${asignatura.activa ? 'Inactivar' : 'Reactivar'} ${asignatura.codigo}`}
                        onClick={() => alAbrir({ que: 'estado-asignatura', asignatura })}
                      >
                        {asignatura.activa ? 'Inactivar' : 'Reactivar'}
                      </Button>
                    </Stack>
                  </TableCell>
                )}
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </>
  );
}
