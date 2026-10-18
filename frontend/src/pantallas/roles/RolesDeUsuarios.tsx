// La pestaña «Usuarios» de la pantalla «Roles» (RF-03): cada usuario con sus roles y, con
// escritura, un diálogo para dárselos y quitárselos. El cambio vale desde la próxima renovación de
// la sesión de ese usuario (D-62).
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
import Typography from '@mui/material/Typography';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { api, apiRoles, llamar } from '../../api/cliente';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError, type MensajeDeError } from '../../errores/traducir';
import { nombreDelRol } from '../../sesion/roles';
import { useRoles, type Rol } from './consultas';

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
  const [abierto, setAbierto] = useState<string | null>(null);

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
              {escritura && (
                <TableCell>
                  <Button
                    aria-label={`Cambiar roles de ${usuario.usuario}`}
                    onClick={() => setAbierto(usuario.usuario)}
                  >
                    Cambiar roles
                  </Button>
                </TableCell>
              )}
            </TableRow>
          ))}
        </TableBody>
      </Table>
      {abierto && (
        <DialogoDeRoles
          usuario={abierto}
          actuales={rolesDe(abierto)}
          roles={todosLosRoles}
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
