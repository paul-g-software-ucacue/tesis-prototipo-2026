// El marco común de las pantallas internas (FR-005, ADR-019): encabezado con el usuario y «Cerrar
// sesión», menú lateral y la región principal. Cada historia agrega su entrada en ENTRADAS.
import AppBar from '@mui/material/AppBar';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Drawer from '@mui/material/Drawer';
import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemText from '@mui/material/ListItemText';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import { NavLink, Outlet, useNavigate } from 'react-router';
import { cerrarSesion } from '../sesion/almacen';
import { useSesionActual } from '../sesion/useSesionActual';

const ANCHO_DEL_MENU = 240;

/** Las entradas del menú principal; las historias con pantallas agregan las suyas. */
const ENTRADAS = [{ ruta: '/', texto: 'Inicio' }];

export function Marco() {
  const navegar = useNavigate();
  const sesion = useSesionActual();

  const salir = () => {
    cerrarSesion();
    void navegar('/iniciar-sesion', { replace: true });
  };

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
            {ENTRADAS.map(({ ruta, texto }) => (
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
