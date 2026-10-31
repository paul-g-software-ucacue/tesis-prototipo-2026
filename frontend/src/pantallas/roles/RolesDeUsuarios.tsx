// La pestaña «Usuarios» de la pantalla «Roles» (RF-03): cada usuario con sus roles y, con
// escritura, un diálogo para dárselos y quitárselos. A un Coordinador de carrera, otro para indicar
// su carrera, de las vigentes (F06-H05, D-76). Los cambios valen desde la próxima renovación de la
// sesión de ese usuario (D-62).
import Button from '@mui/material/Button';
import Checkbox from '@mui/material/Checkbox';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import FormControlLabel from '@mui/material/FormControlLabel';
import FormGroup from '@mui/material/FormGroup';
import Stack from '@mui/material/Stack';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { api, apiRoles, llamar } from '../../api/cliente';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError, type MensajeDeError } from '../../errores/traducir';
import { nombreDelRol } from '../../sesion/roles';
import { useCarreras, type Carrera } from '../carreras/consultas';
import { SELECCION } from '../comun/formulario';
import { useRoles, type Rol } from './consultas';

const COORDINADOR = 'COORDINADOR_CARRERA';

/** Lo que está abierto: el diálogo de los roles o el de la carrera de un usuario. */
type Abierto = { que: 'roles' | 'carrera'; usuario: string } | null;

export function RolesDeUsuarios({ escritura }: { escritura: boolean }) {
  const usuarios = useQuery({
    queryKey: ['usuarios'],
    queryFn: () => llamar(api.GET('/api/usuarios/usuarios')),
  });
  const asignaciones = useQuery({
    queryKey: ['roles-de-usuarios'],
    queryFn: () => llamar(apiRoles.GET('/api/roles/usuarios')),
  });
  const roles = useRoles();
  // Sin permiso para leer las carreras, la tabla muestra el código.
  const carreras = useCarreras().data ?? [];
  const [abierto, setAbierto] = useState<Abierto>(null);

  const error = usuarios.error ?? asignaciones.error ?? roles.error;
  if (error) {
    return <AvisoDeError mensaje={traducirError(error)} />;
  }
  if (!usuarios.data || !asignaciones.data || !roles.data) {
    return null;
  }
  const todosLosRoles = roles.data;
  const rolesDe = (usuario: string) =>
    asignaciones.data.find((fila) => fila.usuario === usuario)?.roles ?? [];
  const carreraDe = (usuario: string) =>
    asignaciones.data.find((fila) => fila.usuario === usuario)?.carrera;
  const textoDeLaCarrera = (usuario: string) => {
    if (!rolesDe(usuario).includes(COORDINADOR)) {
      return '—';
    }
    const codigo = carreraDe(usuario);
    if (!codigo) {
      return 'Sin carrera';
    }
    return carreras.find((carrera) => carrera.codigo === codigo)?.nombre ?? codigo;
  };
  const nombre = (identificador: string) =>
    todosLosRoles.find((rol) => rol.identificador === identificador)?.nombre ??
    nombreDelRol(identificador);

  return (
    <>
      <Table aria-label="Usuarios y sus roles" size="small">
        <TableHead>
          <TableRow>
            <TableCell>Usuario</TableCell>
            <TableCell>Nombre</TableCell>
            <TableCell>Estado</TableCell>
            <TableCell>Roles</TableCell>
            <TableCell>Carrera</TableCell>
            {escritura && <TableCell>Acciones</TableCell>}
          </TableRow>
        </TableHead>
        <TableBody>
          {usuarios.data.map((usuario) => (
            <TableRow key={usuario.usuario}>
              <TableCell>{usuario.usuario}</TableCell>
              <TableCell>{usuario.nombre}</TableCell>
              <TableCell>{usuario.activo ? 'Activo' : 'Inactivo'}</TableCell>
              <TableCell>
                {rolesDe(usuario.usuario).map(nombre).join(', ') || 'Sin roles'}
              </TableCell>
              <TableCell>{textoDeLaCarrera(usuario.usuario)}</TableCell>
              {escritura && (
                <TableCell>
                  <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap' }}>
                    <Button
                      aria-label={`Cambiar roles de ${usuario.usuario}`}
                      onClick={() => setAbierto({ que: 'roles', usuario: usuario.usuario })}
                    >
                      Cambiar roles
                    </Button>
                    {rolesDe(usuario.usuario).includes(COORDINADOR) && (
                      <Button
                        aria-label={`Indicar la carrera de ${usuario.usuario}`}
                        onClick={() => setAbierto({ que: 'carrera', usuario: usuario.usuario })}
                      >
                        Indicar carrera
                      </Button>
                    )}
                  </Stack>
                </TableCell>
              )}
            </TableRow>
          ))}
        </TableBody>
      </Table>
      {abierto?.que === 'roles' && (
        <DialogoDeRoles
          usuario={abierto.usuario}
          actuales={rolesDe(abierto.usuario)}
          roles={todosLosRoles}
          alCerrar={() => setAbierto(null)}
        />
      )}
      {abierto?.que === 'carrera' && (
        <DialogoDeCarrera
          usuario={abierto.usuario}
          actual={carreraDe(abierto.usuario)}
          carreras={carreras.filter((carrera) => carrera.estado === 'VIGENTE')}
          alCerrar={() => setAbierto(null)}
        />
      )}
    </>
  );
}

interface PropiedadesDelDialogo {
  usuario: string;
  actuales: string[];
  roles: Rol[];
  alCerrar: () => void;
}

function DialogoDeRoles({ usuario, actuales, roles, alCerrar }: PropiedadesDelDialogo) {
  const consultas = useQueryClient();
  const [elegidos, setElegidos] = useState(() => new Set(actuales));
  const [aviso, setAviso] = useState<MensajeDeError | null>(null);
  const [enviando, setEnviando] = useState(false);

  const alternar = (identificador: string) => {
    const nuevos = new Set(elegidos);
    if (nuevos.has(identificador)) {
      nuevos.delete(identificador);
    } else {
      nuevos.add(identificador);
    }
    setElegidos(nuevos);
  };

  const guardar = async () => {
    setAviso(null);
    setEnviando(true);
    try {
      await llamar(
        apiRoles.PUT('/api/roles/usuarios/{usuario}', {
          params: { path: { usuario } },
          body: { roles: [...elegidos].sort() },
        }),
      );
      await Promise.all([
        consultas.invalidateQueries({ queryKey: ['roles-de-usuarios'] }),
        consultas.invalidateQueries({ queryKey: ['roles'] }),
      ]);
      alCerrar();
    } catch (fallo) {
      setAviso(traducirError(fallo));
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Dialog open onClose={alCerrar} aria-labelledby="titulo-de-roles-del-usuario">
      <DialogTitle id="titulo-de-roles-del-usuario">Roles de {usuario}</DialogTitle>
      <DialogContent>
        <Stack spacing={2}>
          {aviso && <AvisoDeError mensaje={aviso} />}
          <Typography variant="body2">
            El cambio vale desde la próxima renovación de su sesión, como máximo en 30 minutos.
          </Typography>
          <FormGroup>
            {roles.map((rol) => (
              <FormControlLabel
                key={rol.identificador}
                control={
                  <Checkbox
                    checked={elegidos.has(rol.identificador)}
                    onChange={() => alternar(rol.identificador)}
                  />
                }
                label={rol.nombre}
              />
            ))}
          </FormGroup>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={alCerrar}>Cerrar</Button>
        <Button variant="contained" disabled={enviando} onClick={() => void guardar()}>
          Guardar
        </Button>
      </DialogActions>
    </Dialog>
  );
}

interface PropiedadesDeLaCarrera {
  usuario: string;
  actual: string | undefined;
  carreras: Carrera[];
  alCerrar: () => void;
}

/** La carrera de un coordinador: una sola, de las vigentes. */
function DialogoDeCarrera({ usuario, actual, carreras, alCerrar }: PropiedadesDeLaCarrera) {
  const consultas = useQueryClient();
  const [elegida, setElegida] = useState(actual ?? '');
  const [aviso, setAviso] = useState<MensajeDeError | null>(null);
  const [enviando, setEnviando] = useState(false);

  const enviar = async (cambio: () => Promise<unknown>) => {
    setAviso(null);
    setEnviando(true);
    try {
      await cambio();
      await consultas.invalidateQueries({ queryKey: ['roles-de-usuarios'] });
      alCerrar();
    } catch (fallo) {
      setAviso(traducirError(fallo));
    } finally {
      setEnviando(false);
    }
  };

  const guardar = () =>
    enviar(() =>
      llamar(
        apiRoles.PUT('/api/roles/usuarios/{usuario}/carrera', {
          params: { path: { usuario } },
          body: { carrera: elegida },
        }),
      ),
    );

  const quitar = () =>
    enviar(() =>
      llamar(
        apiRoles.DELETE('/api/roles/usuarios/{usuario}/carrera', {
          params: { path: { usuario } },
        }),
      ),
    );

  return (
    <Dialog open onClose={alCerrar} aria-labelledby="titulo-de-la-carrera" fullWidth maxWidth="sm">
      <DialogTitle id="titulo-de-la-carrera">Carrera de {usuario}</DialogTitle>
      <DialogContent>
        <Stack spacing={2}>
          {aviso && <AvisoDeError mensaje={aviso} />}
          <Typography variant="body2">
            El cambio vale desde la próxima renovación de su sesión, como máximo en 30 minutos.
          </Typography>
          <TextField
            select
            label="Carrera"
            value={elegida}
            onChange={(evento) => setElegida(evento.target.value)}
            slotProps={SELECCION}
            sx={{ mt: 1 }}
          >
            <option value="">Elige una carrera</option>
            {actual && !carreras.some((carrera) => carrera.codigo === actual) && (
              <option value={actual}>{actual}</option>
            )}
            {carreras.map((carrera) => (
              <option key={carrera.codigo} value={carrera.codigo}>
                {carrera.nombre}
              </option>
            ))}
          </TextField>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={alCerrar}>Cerrar</Button>
        {actual && (
          <Button disabled={enviando} onClick={() => void quitar()}>
            Quitar carrera
          </Button>
        )}
        <Button variant="contained" disabled={enviando || !elegida} onClick={() => void guardar()}>
          Guardar
        </Button>
      </DialogActions>
    </Dialog>
  );
}
