// La pantalla «Cargos» (F03-H05, carga 8): los cargos de gestión de los docentes, con su unidad o su
// carrera, sus fechas, su estado y lo que hay que revisar de cada uno (RN-20). La ven quienes leen
// los docentes; con «Docentes», «Catálogos y datos maestros» o «Distributivo del periodo» de
// escritura se registran, se corrigen y se finalizan (D-70). Ninguno se borra.
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
import { CLAVE_CARGOS, useCargos, type Cargo, type EstadoDeCargo } from './consultas';
import { DialogoCargo } from './DialogoCargo';
import { NOMBRE_DEL_CARGO, NOMBRE_DEL_ESTADO_DEL_CARGO, cargos } from './nombres';

/** Lo que está abierto: el diálogo de registrar o el de un cargo. */
type Abierto = { que: 'registrar' } | { que: 'corregir'; cargo: Cargo } | null;

export function Cargos() {
  useTitulo('Cargos');
  const sesion = useSesionActual();
  const escritura = tieneAlguno(sesion.data?.permisos, ESCRITURA_DE_LOS_DOCENTES);
  const consultas = useQueryClient();
  const [estado, setEstado] = useState<EstadoDeCargo | ''>('');
  const [docente, setDocente] = useState('');
  const lista = useCargos(estado, docente);
  const docentes = useDocentes({ texto: '', carrera: '', estado: '' });
  const referencias = useReferenciasDeDocentes();
  const [abierto, setAbierto] = useState<Abierto>(null);
  const [advertido, setAdvertido] = useState<Cargo | null>(null);

  const terminar = async (guardado: Cargo) => {
    await consultas.invalidateQueries({ queryKey: CLAVE_CARGOS });
    setAdvertido(guardado.advertencias.length ? guardado : null);
    setAbierto(null);
  };

  return (
    <>
      <Typography component="h1" variant="h4" gutterBottom>
        Cargos
      </Typography>
      {lista.error && <AvisoDeError mensaje={traducirError(lista.error)} />}
      {referencias.error && <AvisoDeError mensaje={traducirError(referencias.error)} />}
      {!escritura && sesion.data && (
        <Alert severity="info" sx={{ my: 1 }}>
          Solo lectura: tu rol no registra ni corrige cargos.
        </Alert>
      )}
      {advertido && (
        <Alert severity="warning" sx={{ my: 1 }} onClose={() => setAdvertido(null)}>
          {`El cargo de ${advertido.nombreDelDocente ?? advertido.docente} se guardó. `}
          {advertido.advertencias.map((una) => una.descripcion).join(' ')}
        </Alert>
      )}
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ my: 2 }}>
        <TextField
          select
          label="Estado"
          value={estado}
          onChange={(evento) => setEstado(evento.target.value as EstadoDeCargo | '')}
          slotProps={SELECCION}
          sx={{ minWidth: 180 }}
        >
          <option value="">Todos</option>
          {Object.entries(NOMBRE_DEL_ESTADO_DEL_CARGO).map(([codigo, nombre]) => (
            <option key={codigo} value={codigo}>
              {nombre}
            </option>
          ))}
        </TextField>
        <TextField
          select
          label="Docente"
          value={docente}
          onChange={(evento) => setDocente(evento.target.value)}
          slotProps={SELECCION}
          sx={{ minWidth: 280 }}
        >
          <option value="">Todos</option>
          {(docentes.data ?? []).map((uno) => (
            <option key={uno.identificacion} value={uno.identificacion}>
              {uno.nombreCompleto}
            </option>
          ))}
        </TextField>
      </Stack>
      {escritura && (
        <Button variant="outlined" sx={{ mb: 2 }} onClick={() => setAbierto({ que: 'registrar' })}>
          Registrar cargo
        </Button>
      )}
      {lista.data && (
        <>
          <Typography component="p" sx={{ mb: 1 }}>
            {cargos(lista.data.length)}
          </Typography>
          <TableContainer>
            <Table aria-label="Cargos" size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Docente</TableCell>
                  <TableCell>Cargo</TableCell>
                  <TableCell>Unidad o carrera</TableCell>
                  <TableCell>Designación</TableCell>
                  <TableCell>Fin</TableCell>
                  <TableCell>Estado</TableCell>
                  <TableCell>Advertencias</TableCell>
                  {escritura && <TableCell>Acciones</TableCell>}
                </TableRow>
              </TableHead>
              <TableBody>
                {lista.data.map((cargo) => {
                  const nombre = cargo.nombreDelDocente ?? cargo.docente;
                  return (
                    <TableRow key={`${cargo.docente}-${cargo.tipo}-${cargo.fechaDesignacion}`}>
                      <TableCell>{nombre}</TableCell>
                      <TableCell>{NOMBRE_DEL_CARGO[cargo.tipo]}</TableCell>
                      <TableCell>
                        {cargo.carrera
                          ? (cargo.nombreDeCarrera ?? cargo.carrera)
                          : (cargo.nombreDeUnidad ?? cargo.unidad)}
                      </TableCell>
                      <TableCell>{cargo.fechaDesignacion}</TableCell>
                      <TableCell>{cargo.fechaFin ?? '—'}</TableCell>
                      <TableCell>{NOMBRE_DEL_ESTADO_DEL_CARGO[cargo.estado]}</TableCell>
                      <TableCell>
                        {cargo.advertencias.length
                          ? cargo.advertencias.map((una) => (
                              <Typography key={una.codigo} variant="body2" color="warning.dark">
                                {una.descripcion}
                              </Typography>
                            ))
                          : '—'}
                      </TableCell>
                      {escritura && (
                        <TableCell>
                          <Button
                            size="small"
                            aria-label={`Corregir el cargo de ${nombre}: ${NOMBRE_DEL_CARGO[cargo.tipo]}`}
                            onClick={() => setAbierto({ que: 'corregir', cargo })}
                          >
                            {cargo.estado === 'VIGENTE' ? 'Corregir o finalizar' : 'Corregir'}
                          </Button>
                        </TableCell>
                      )}
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </TableContainer>
        </>
      )}
      {abierto && (
        <DialogoCargo
          cargo={abierto.que === 'corregir' ? abierto.cargo : undefined}
          docentes={docentes.data ?? []}
          referencias={referencias.data}
          alCerrar={() => setAbierto(null)}
          alGuardar={terminar}
        />
      )}
    </>
  );
}
