// El marco común de las pantallas internas (FR-005, ADR-019): encabezado con el usuario y «Cerrar
// sesión», menú lateral y la región principal. Cada historia agrega su entrada en ENTRADAS, con el
// permiso que exige, o con varios alternativos (D-70); el menú solo muestra las que el usuario
// puede usar (ADR-021).
import AppBar from '@mui/material/AppBar';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Drawer from '@mui/material/Drawer';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemText from '@mui/material/ListItemText';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import { NavLink, Outlet } from 'react-router';
import { cerrarSesion } from '../sesion/almacen';
import {
  DESCARGA_DE_PLANTILLAS,
  LECTURA_DE_LA_ESTRUCTURA,
  LECTURA_DE_LA_OFERTA,
} from '../sesion/funcionalidades';
import { tieneAlguno, type PermisoRequerido } from '../sesion/permisos';
import { useSesionActual } from '../sesion/useSesionActual';

const ANCHO_DEL_MENU = 240;

interface Entrada {
  ruta: string;
  texto: string;
  /** El permiso que exige, o varios alternativos: basta con uno. */
  permiso?: PermisoRequerido | readonly PermisoRequerido[];
}

/** Las entradas del menú principal; las historias con pantallas agregan las suyas. */
const ENTRADAS: Entrada[] = [
  { ruta: '/', texto: 'Inicio' },
  { ruta: '/roles', texto: 'Roles', permiso: { funcionalidad: 'USUARIOS_ROLES', tipo: 'LECTURA' } },
  {
    ruta: '/normativa',
    texto: 'Normativa',
    permiso: { funcionalidad: 'PARAMETROS', tipo: 'LECTURA' },
  },
  {
    ruta: '/periodos',
    texto: 'Periodos',
    permiso: { funcionalidad: 'DISTRIBUTIVO', tipo: 'LECTURA' },
  },
  { ruta: '/estructura', texto: 'Estructura orgánica', permiso: LECTURA_DE_LA_ESTRUCTURA },
  { ruta: '/carreras', texto: 'Carreras y mallas', permiso: LECTURA_DE_LA_ESTRUCTURA },
  { ruta: '/oferta', texto: 'Oferta del periodo', permiso: LECTURA_DE_LA_OFERTA },
  { ruta: '/plantillas', texto: 'Plantillas de carga', permiso: DESCARGA_DE_PLANTILLAS },
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
                tieneAlguno(sesion.data?.permisos, Array.isArray(permiso) ? permiso : [permiso]),
            ).map(({ ruta, texto }) => (
              // Cada enlace en su <li>: un <ul> solo puede tener elementos de lista (WCAG 1.3.1).
              <ListItem key={ruta} disablePadding>
                <ListItemButton component={NavLink} to={ruta} end>
                  <ListItemText primary={texto} />
                </ListItemButton>
              </ListItem>
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
