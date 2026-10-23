// La pantalla «Estructura orgánica» (F03-H01, RF-12): las unidades de la institución en su
// jerarquía, con su tipo, la gestión autorizada que representan y su estado. Las comisiones son un
// dato informativo, sin horas (C-12). La ven quienes leen el distributivo o los catálogos (D-70);
// con «Catálogos y datos maestros: escritura» (el Administrador) se registra, se corrige, se
// inactiva y se reactiva una unidad, con confirmación. El servidor valida cada operación.
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
import { tienePermiso } from '../../sesion/permisos';
import { useSesionActual } from '../../sesion/useSesionActual';
import {
  CLAVE_ESTRUCTURA,
  cambiarEstadoUnidad,
  useGestionesAutorizadas,
  useUnidades,
  type UnidadOrganica,
} from './consultas';
import { DialogoDatosDeUnidad, DialogoEstado } from './DialogosDeUnidad';
import { NOMBRE_DEL_TIPO, enArbol } from './jerarquia';

/** Lo que está abierto: un diálogo, sobre una unidad o para una nueva. */
type Abierto = { que: 'registrar' } | { que: 'corregir' | 'estado'; unidad: UnidadOrganica } | null;

export function Estructura() {
  useTitulo('Estructura orgánica');
  const sesion = useSesionActual();
  const escritura = tienePermiso(sesion.data?.permisos, 'CATALOGOS', 'ESCRITURA');
  const consultas = useQueryClient();
  const unidades = useUnidades();
  const gestiones = useGestionesAutorizadas();
  const nombreDeGestion = (codigo: string) =>
    gestiones.data?.find((gestion) => gestion.codigo === codigo)?.nombre;
  const [abierto, setAbierto] = useState<Abierto>(null);
  const [aviso, setAviso] = useState<MensajeDeError | null>(null);

  const terminar = async () => {
    await consultas.invalidateQueries({ queryKey: CLAVE_ESTRUCTURA });
    setAbierto(null);
  };

  const cambiarEstado = async (unidad: UnidadOrganica) => {
    setAviso(null);
    setAbierto(null);
    try {
      await cambiarEstadoUnidad(unidad.codigo, !unidad.activa);
      await consultas.invalidateQueries({ queryKey: CLAVE_ESTRUCTURA });
    } catch (fallo) {
      setAviso(traducirError(fallo));
    }
  };

  return (
    <>
      <Typography component="h1" variant="h4" gutterBottom>
        Estructura orgánica
      </Typography>
      {unidades.error && <AvisoDeError mensaje={traducirError(unidades.error)} />}
      {gestiones.error && <AvisoDeError mensaje={traducirError(gestiones.error)} />}
      {aviso && <AvisoDeError mensaje={aviso} />}
      {escritura && (
        <Button variant="outlined" sx={{ my: 1 }} onClick={() => setAbierto({ que: 'registrar' })}>
          Registrar unidad
        </Button>
      )}
      {unidades.data && (
        <TableContainer>
          <Table aria-label="Estructura orgánica" size="small">
            <TableHead>
              <TableRow>
                <TableCell>Código</TableCell>
                <TableCell>Unidad</TableCell>
                <TableCell>Tipo</TableCell>
                <TableCell>Gestión autorizada</TableCell>
                <TableCell>Estado</TableCell>
                {escritura && <TableCell>Acciones</TableCell>}
              </TableRow>
            </TableHead>
            <TableBody>
              {enArbol(unidades.data).map(({ unidad, nivel }) => (
                <TableRow key={unidad.codigo}>
                  <TableCell>{unidad.codigo}</TableCell>
                  {/* La sangría muestra la jerarquía; «Depende de» la dice también en texto. */}
                  <TableCell style={{ paddingLeft: 16 + nivel * 16 }}>
                    {unidad.nombre}
                    {unidad.padre && (
                      <Typography variant="body2" color="text.secondary">
                        {`Depende de ${unidad.padre}`}
                      </Typography>
                    )}
                  </TableCell>
                  <TableCell>
                    {NOMBRE_DEL_TIPO[unidad.tipo]}
                    {unidad.tipo === 'COMISION' && (
                      <Typography variant="body2" color="text.secondary">
                        {`Informativa, sin horas · ${
                          unidad.comisionPermanente ? 'Permanente' : 'No permanente'
                        }`}
                      </Typography>
                    )}
                  </TableCell>
                  <TableCell>
                    {unidad.gestionAutorizada &&
                      [unidad.gestionAutorizada, nombreDeGestion(unidad.gestionAutorizada)]
                        .filter(Boolean)
                        .join(' · ')}
                  </TableCell>
                  <TableCell>{unidad.activa ? 'Activa' : 'Inactiva'}</TableCell>
                  {escritura && (
                    <TableCell>
                      <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap' }}>
                        <Button
                          size="small"
                          aria-label={`Corregir ${unidad.codigo}`}
                          onClick={() => setAbierto({ que: 'corregir', unidad })}
                        >
                          Corregir
                        </Button>
                        <Button
                          size="small"
                          aria-label={`${unidad.activa ? 'Inactivar' : 'Reactivar'} ${unidad.codigo}`}
                          onClick={() => setAbierto({ que: 'estado', unidad })}
                        >
                          {unidad.activa ? 'Inactivar' : 'Reactivar'}
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
      {abierto?.que === 'registrar' && unidades.data && (
        <DialogoDatosDeUnidad
          unidades={unidades.data}
          gestiones={gestiones.data ?? []}
          alCerrar={() => setAbierto(null)}
          alGuardar={terminar}
        />
      )}
      {abierto?.que === 'corregir' && unidades.data && (
        <DialogoDatosDeUnidad
          unidad={abierto.unidad}
          unidades={unidades.data}
          gestiones={gestiones.data ?? []}
          alCerrar={() => setAbierto(null)}
          alGuardar={terminar}
        />
      )}
      {abierto?.que === 'estado' && (
        <DialogoEstado
          unidad={abierto.unidad}
          alCerrar={() => setAbierto(null)}
          alConfirmar={() => void cambiarEstado(abierto.unidad)}
        />
      )}
    </>
  );
}
