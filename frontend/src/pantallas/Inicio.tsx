// Pantalla de inicio (FR-005): el usuario y sus roles de la sesión actual. Las historias siguientes
// agregan sus pantallas en el menú del marco.
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemText from '@mui/material/ListItemText';
import Typography from '@mui/material/Typography';
import { AvisoDeError } from '../errores/AvisoDeError';
import { traducirError } from '../errores/traducir';
import { useTitulo } from '../navegacion/useTitulo';
import { useSesionActual } from '../sesion/useSesionActual';

export function Inicio() {
  useTitulo('Inicio');
  const sesion = useSesionActual();

  return (
    <>
      <Typography component="h1" variant="h4" gutterBottom>
        {sesion.data ? `Hola, ${sesion.data.usuario}` : 'Inicio'}
      </Typography>
      {sesion.isError && <AvisoDeError mensaje={traducirError(sesion.error)} />}
      {sesion.data && (
        <>
          <Typography component="h2" variant="h6">
            Tus roles
          </Typography>
          <List aria-label="Tus roles">
            {sesion.data.roles.map((rol) => (
              <ListItem key={rol}>
                <ListItemText primary={rol} />
              </ListItem>
            ))}
          </List>
        </>
      )}
    </>
  );
}
