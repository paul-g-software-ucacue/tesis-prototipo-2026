// La pantalla «Roles» (F05-H02, RF-02, RF-03): la lista de roles con su matriz y los roles de cada
// usuario. Sin «Usuarios y roles: escritura» no aparecen las acciones de crear, editar, borrar ni
// asignar; el servidor igual las rechazaría con 403 (RNF-SEG-03).
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import Tab from '@mui/material/Tab';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Tabs from '@mui/material/Tabs';
import Typography from '@mui/material/Typography';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { apiRoles, llamar } from '../../api/cliente';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError } from '../../errores/traducir';
import { useTitulo } from '../../navegacion/useTitulo';
import { tienePermiso } from '../../sesion/permisos';
import { useSesionActual } from '../../sesion/useSesionActual';
import { useRoles, type Rol } from './consultas';
import { RolesDeUsuarios } from './RolesDeUsuarios';
import { RolFormulario } from './RolFormulario';

/** Qué rol está abierto en el diálogo: uno existente o uno nuevo. */
type Abierto = { rol: Rol } | { nuevo: true } | null;

export function Roles() {
  useTitulo('Roles');
  const sesion = useSesionActual();
  const escritura = tienePermiso(sesion.data?.permisos, 'USUARIOS_ROLES', 'ESCRITURA');
  const [pestana, setPestana] = useState<'roles' | 'usuarios'>('roles');

  return (
    <>
      <Typography component="h1" variant="h4" gutterBottom>
        Roles
      </Typography>
      <Tabs
        value={pestana}
        onChange={(_, valor: 'roles' | 'usuarios') => setPestana(valor)}
        sx={{ mb: 2 }}
      >
        <Tab value="roles" label="Roles" />
        <Tab value="usuarios" label="Usuarios" />
      </Tabs>
      {pestana === 'roles' ? (
        <ListaDeRoles escritura={escritura} />
      ) : (
        <RolesDeUsuarios escritura={escritura} />
      )}
    </>
  );
}

function ListaDeRoles({ escritura }: { escritura: boolean }) {
  const roles = useRoles();
  const funcionalidades = useQuery({
    queryKey: ['funcionalidades'],
    queryFn: () => llamar(apiRoles.GET('/api/roles/funcionalidades')),
  });
  const [abierto, setAbierto] = useState<Abierto>(null);

  const error = roles.error ?? funcionalidades.error;
  if (error) {
    return <AvisoDeError mensaje={traducirError(error)} />;
  }
  if (!roles.data || !funcionalidades.data) {
    return null;
  }

  return (
    <>
      {escritura && (
        <Box sx={{ mb: 2 }}>
          <Button variant="contained" onClick={() => setAbierto({ nuevo: true })}>
            Crear rol
          </Button>
        </Box>
      )}
      <Table aria-label="Roles" size="small">
        <TableHead>
          <TableRow>
            <TableCell>Nombre</TableCell>
            <TableCell>Descripción</TableCell>
            <TableCell>Origen</TableCell>
            <TableCell align="right">Usuarios</TableCell>
            <TableCell>
              <Box component="span" sx={visuallyHidden}>
                Acciones
              </Box>
            </TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {roles.data.map((rol) => (
            <TableRow key={rol.identificador}>
              <TableCell>{rol.nombre}</TableCell>
              <TableCell>{rol.descripcion}</TableCell>
              <TableCell>
                {rol.predefinido ? <Chip size="small" label="Matriz D-16" /> : 'Propio'}
              </TableCell>
              <TableCell align="right">{rol.usuariosAsignados}</TableCell>
              <TableCell>
                <Button aria-label={`Ver ${rol.nombre}`} onClick={() => setAbierto({ rol })}>
                  Ver
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
      {abierto && (
        <RolFormulario
          rol={'rol' in abierto ? abierto.rol : undefined}
          funcionalidades={funcionalidades.data}
          escritura={escritura}
          alCerrar={() => setAbierto(null)}
        />
      )}
    </>
  );
}

const visuallyHidden = {
  border: 0,
  clip: 'rect(0 0 0 0)',
  height: 1,
  margin: -1,
  overflow: 'hidden',
  padding: 0,
  position: 'absolute',
  whiteSpace: 'nowrap',
  width: 1,
} as const;
