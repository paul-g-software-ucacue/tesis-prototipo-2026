// Pantalla de inicio (FR-005): el nombre del usuario, sus roles en español (F05-H01) y «Tus
// funciones», la unión de los permisos de todos sus roles, sin repetir (F05-H02, RF-03). Las
// historias siguientes agregan sus pantallas en el menú del marco.
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemText from '@mui/material/ListItemText';
import Typography from '@mui/material/Typography';
import { AvisoDeError } from '../errores/AvisoDeError';
import { traducirError } from '../errores/traducir';
import { useTitulo } from '../navegacion/useTitulo';
import { funcionesDelUsuario, type TipoDelUsuario } from '../sesion/permisos';
import { nombreDelRol } from '../sesion/roles';
import { useSesionActual } from '../sesion/useSesionActual';

export function Inicio() {
  useTitulo('Inicio');
  const sesion = useSesionActual();

  return (
    <>
      <Typography component="h1" variant="h4" gutterBottom>
        {sesion.data ? `Hola, ${sesion.data.nombre ?? sesion.data.usuario}` : 'Inicio'}
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
                <ListItemText primary={nombreDelRol(rol)} />
              </ListItem>
            ))}
          </List>
          <TusFunciones permisos={sesion.data.permisos} />
        </>
      )}
    </>
  );
}

function TusFunciones({ permisos }: { permisos?: string[] }) {
  const funciones = funcionesDelUsuario(permisos);
  return (
    <>
      <Typography component="h2" variant="h6">
        Tus funciones
      </Typography>
      {funciones.length === 0 ? (
        <Typography>Todavía no tienes funciones asignadas.</Typography>
      ) : (
        <List aria-label="Tus funciones">
          {funciones.map((funcion) => (
            <ListItem key={funcion.codigo}>
              <ListItemText primary={funcion.nombre} secondary={describir(funcion.tipos)} />
            </ListItem>
          ))}
        </List>
      )}
    </>
  );
}

/** «Lectura, escritura y aprobación · solo lo propio», o el alcance de cada tipo si se mezclan. */
function describir(tipos: TipoDelUsuario[]): string {
  const todosPropios = tipos.every((tipo) => tipo.propio);
  const nombres = tipos.map(({ nombre, propio }) =>
    propio && !todosPropios ? `${nombre} (solo lo propio)` : nombre,
  );
  const ultimo = nombres.at(-1) ?? '';
  const lista = nombres.length > 1 ? `${nombres.slice(0, -1).join(', ')} y ${ultimo}` : ultimo;
  const texto = lista.charAt(0).toUpperCase() + lista.slice(1);
  return todosPropios ? `${texto} · solo lo propio` : texto;
}
