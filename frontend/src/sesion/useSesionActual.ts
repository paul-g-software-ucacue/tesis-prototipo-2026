// El usuario y los roles de la sesión actual (GET /api/usuarios/sesion, D-39), compartidos por el
// marco y la pantalla de inicio en la caché de TanStack Query.
import { useQuery } from '@tanstack/react-query';
import { api, llamar } from '../api/cliente';

export function useSesionActual() {
  return useQuery({
    queryKey: ['sesion'],
    queryFn: () => llamar(api.GET('/api/usuarios/sesion')),
  });
}
