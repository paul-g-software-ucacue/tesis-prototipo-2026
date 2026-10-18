// Los tipos y las consultas que comparten las piezas de la pantalla «Roles» (contratos/roles).
import { useQuery } from '@tanstack/react-query';
import { apiRoles, llamar } from '../../api/cliente';
import type { components } from '../../api/generado/roles';

export type Rol = components['schemas']['Rol'];
export type Funcionalidad = components['schemas']['Funcionalidad'];

/** Los roles, compartidos por las dos pestañas. */
export function useRoles() {
  return useQuery({ queryKey: ['roles'], queryFn: () => llamar(apiRoles.GET('/api/roles')) });
}
