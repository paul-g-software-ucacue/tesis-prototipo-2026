// El marco común de las pantallas internas (FR-005, ADR-019): encabezado con el usuario y «Cerrar
// sesión», menú lateral y la región principal. Cada historia agrega su entrada en ENTRADAS, con el
// permiso que exige; el menú solo muestra las que el usuario puede usar (ADR-021).
import AppBar from '@mui/material/AppBar';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Drawer from '@mui/material/Drawer';
import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemText from '@mui/material/ListItemText';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import { NavLink, Outlet } from 'react-router';
import { cerrarSesion } from '../sesion/almacen';
import type { TipoDePermiso } from '../sesion/funcionalidades';
import { tienePermiso } from '../sesion/permisos';
import { useSesionActual } from '../sesion/useSesionActual';

const ANCHO_DEL_MENU = 240;

interface Entrada {
  ruta: string;
  texto: string;
  permiso?: { funcionalidad: string; tipo: TipoDePermiso };
}

/** Las entradas del menú principal; las historias con pantallas agregan las suyas. */
const ENTRADAS: Entrada[] = [
  { ruta: '/', texto: 'Inicio' },
  { ruta: '/roles', texto: 'Roles', permiso: { funcionalidad: 'USUARIOS_ROLES', tipo: 'LECTURA' } },
];

export function Marco() {
  const sesion = useSesionActual();

  // La ruta protegida lleva al inicio de sesión sin destino ni aviso (motivo «cierre»).
  const salir = () => cerrarSesion('cierre');

  return (
    <Box sx={{ display: 'flex' }}>
      <AppBar position="fixed" sx={{ zIndex: (tema) => tema.zIndex.drawer + 1 }}>
        <Toolbar>
          <Typography component="p" variant="h6" sx={{ flexGrow: 1 }}>
            MDD · Distributivo docente
          </Typography>
          {sesion.data && (
            <Typography component="span" sx={{ mr: 2 }} data-usuario>
              {sesion.data.usuario}
            </Typography>
          )}
          <Button color="inherit" onClick={salir}>
            Cerrar sesión
          </Button>
        </Toolbar>
      </AppBar>
      <Drawer
        variant="permanent"
        sx={{
          width: ANCHO_DEL_MENU,
          flexShrink: 0,
          '& .MuiDrawer-paper': { width: ANCHO_DEL_MENU, boxSizing: 'border-box' },
        }}
      >
        <Toolbar />
        <nav aria-label="Menú principal">
          <List>
            {ENTRADAS.filter(
              ({ permiso }) =>
                !permiso ||
                tienePermiso(sesion.data?.permisos, permiso.funcionalidad, permiso.tipo),
            ).map(({ ruta, texto }) => (
              <ListItemButton key={ruta} component={NavLink} to={ruta} end>
                <ListItemText primary={texto} />
              </ListItemButton>
            ))}
          </List>
        </nav>
      </Drawer>
      <Box component="main" sx={{ flexGrow: 1, p: 3 }}>
        <Toolbar />
        <Outlet />
      </Box>
    </Box>
  );
}
