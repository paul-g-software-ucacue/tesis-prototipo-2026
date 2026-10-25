// La pantalla «Proyectos» (F03-H05, RF-18, RN-25): los proyectos aprobados por el OCS y, en el
// periodo elegido, si cada uno es asignable o por qué no (su estado o su vigencia). La ven quienes
// leen los docentes; con «Docentes», «Catálogos y datos maestros» o «Distributivo del periodo» de
// escritura se registran y se corrigen (D-70). Ninguno se borra: cambia de estado.
import Alert from '@mui/material/Alert';
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
import { traducirError } from '../../errores/traducir';
import { useTitulo } from '../../navegacion/useTitulo';
import { ESCRITURA_DE_LOS_DOCENTES } from '../../sesion/funcionalidades';
import { tieneAlguno } from '../../sesion/permisos';
import { useSesionActual } from '../../sesion/useSesionActual';
import { SELECCION } from '../comun/formulario';
import { useDocentes, useReferenciasDeDocentes } from '../docentes/consultas';
import {
  CLAVE_PROYECTOS,
  useProyectos,
  type PeriodoDeReferencia,
  type Proyecto,
} from './consultas';
import { DialogoProyecto } from './DialogoProyecto';
import {
  NOMBRE_DEL_ESTADO_DEL_PROYECTO,
  NOMBRE_DEL_TIPO_DE_PROYECTO,
  POR_QUE_NO_ES_ASIGNABLE,
  horas,
  proyectos,
} from './nombres';

/** Lo que está abierto: el diálogo de registrar o el de un proyecto. */
type Abierto = { que: 'registrar' } | { que: 'corregir'; proyecto: Proyecto } | null;

/** El periodo que empieza más tarde: el que se elige al abrir la pantalla. */
function masReciente(periodos: readonly PeriodoDeReferencia[]) {
  return [...periodos].sort((uno, otro) => otro.inicio.localeCompare(uno.inicio))[0]?.codigo ?? '';
}

/** «Asignable» o por qué no, en el periodo elegido. */
function asignabilidad(proyecto: Proyecto) {
  if (proyecto.asignable === undefined) {
    return '—';
  }
  if (proyecto.asignable) {
    return 'Asignable';
  }
  const motivos = (proyecto.motivos ?? []).map((motivo) => POR_QUE_NO_ES_ASIGNABLE[motivo]);
  return `No asignable ${motivos.join(' y ')}`;
}

export function Proyectos() {
  useTitulo('Proyectos');
  const sesion = useSesionActual();
  const escritura = tieneAlguno(sesion.data?.permisos, ESCRITURA_DE_LOS_DOCENTES);
  const consultas = useQueryClient();
  const referencias = useReferenciasDeDocentes();
  const docentes = useDocentes({ texto: '', carrera: '', estado: '' });
  const [elegido, setElegido] = useState<string | null>(null);
  const periodo = elegido ?? masReciente(referencias.data?.periodos ?? []);
  const lista = useProyectos(periodo);
  const [abierto, setAbierto] = useState<Abierto>(null);

  const terminar = async () => {
    await consultas.invalidateQueries({ queryKey: CLAVE_PROYECTOS });
    setAbierto(null);
  };

  return (
    <>
      <Typography component="h1" variant="h4" gutterBottom>
        Proyectos
      </Typography>
      {lista.error && <AvisoDeError mensaje={traducirError(lista.error)} />}
      {referencias.error && <AvisoDeError mensaje={traducirError(referencias.error)} />}
      {!escritura && sesion.data && (
        <Alert severity="info" sx={{ my: 1 }}>
          Solo lectura: tu rol no registra ni corrige proyectos.
        </Alert>
      )}
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ my: 2 }}>
        <TextField
          select
          label="Periodo"
          value={periodo}
          onChange={(evento) => setElegido(evento.target.value)}
          slotProps={SELECCION}
          helperText="Solo un proyecto aprobado o en ejecución, con su vigencia en el periodo, recibe horas."
          sx={{ minWidth: 280 }}
        >
          <option value="">Sin periodo</option>
          {(referencias.data?.periodos ?? []).map((uno) => (
            <option key={uno.codigo} value={uno.codigo}>
              {`${uno.codigo} · ${uno.inicio} a ${uno.fin}`}
            </option>
          ))}
        </TextField>
      </Stack>
      {escritura && (
        <Button variant="outlined" sx={{ mb: 2 }} onClick={() => setAbierto({ que: 'registrar' })}>
          Registrar proyecto
        </Button>
      )}
      {lista.data && (
        <>
          <Typography component="p" sx={{ mb: 1 }}>
            {proyectos(lista.data.length)}
          </Typography>
          <TableContainer>
            <Table aria-label="Proyectos" size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Código</TableCell>
                  <TableCell>Nombre</TableCell>
                  <TableCell>Tipo</TableCell>
                  <TableCell>Estado</TableCell>
                  <TableCell>Vigencia</TableCell>
                  <TableCell>Director</TableCell>
                  <TableCell>Horas aprobadas</TableCell>
                  <TableCell>{periodo ? `En ${periodo}` : 'Asignable'}</TableCell>
                  {escritura && <TableCell>Acciones</TableCell>}
                </TableRow>
              </TableHead>
              <TableBody>
                {lista.data.map((proyecto) => (
                  <TableRow key={proyecto.codigo}>
                    <TableCell>{proyecto.codigo}</TableCell>
                    <TableCell>{proyecto.nombre}</TableCell>
                    <TableCell>{NOMBRE_DEL_TIPO_DE_PROYECTO[proyecto.tipo]}</TableCell>
                    <TableCell>{NOMBRE_DEL_ESTADO_DEL_PROYECTO[proyecto.estado]}</TableCell>
                    <TableCell>{`${proyecto.vigenciaInicio} a ${proyecto.vigenciaFin}`}</TableCell>
                    <TableCell>{proyecto.nombreDelDirector ?? proyecto.director}</TableCell>
                    <TableCell>{horas(proyecto.horasAprobadas)}</TableCell>
                    <TableCell>{asignabilidad(proyecto)}</TableCell>
                    {escritura && (
                      <TableCell>
                        <Button
                          size="small"
                          aria-label={`Corregir el proyecto ${proyecto.codigo}`}
                          onClick={() => setAbierto({ que: 'corregir', proyecto })}
                        >
                          Corregir
                        </Button>
                      </TableCell>
                    )}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </>
      )}
      {abierto && (
        <DialogoProyecto
          proyecto={abierto.que === 'corregir' ? abierto.proyecto : undefined}
          docentes={docentes.data ?? []}
          referencias={referencias.data}
          alCerrar={() => setAbierto(null)}
          alGuardar={terminar}
        />
      )}
    </>
  );
}
