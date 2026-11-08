// Los tipos y las consultas de la pantalla «Bitácora» (contratos/usuarios, F17-H02). Solo leen.
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { api, llamar } from '../../api/cliente';
import type { components, operations } from '../../api/generado/usuarios';

export type PaginaDeBitacora = components['schemas']['PaginaDeBitacora'];
export type EntradaConsultada = components['schemas']['EntradaConsultada'];
export type AccionDeBitacora = components['schemas']['AccionDeBitacora'];
export type ParametrosDeLaConsulta = NonNullable<
  operations['consultarBitacora']['parameters']['query']
>;

const CLAVE = ['bitacora'] as const;

/** La página pedida; mientras llega la siguiente, la anterior se queda en pantalla. */
export function useBitacora(parametros: ParametrosDeLaConsulta) {
  return useQuery({
    queryKey: [...CLAVE, 'consulta', parametros],
    queryFn: () => llamar(api.GET('/api/usuarios/bitacora', { params: { query: parametros } })),
    placeholderData: keepPreviousData,
  });
}

/** Las acciones que hay en la bitácora, para el filtro por tipo de cambio. */
export function useAccionesDeBitacora() {
  return useQuery({
    queryKey: [...CLAVE, 'acciones'],
    queryFn: () => llamar(api.GET('/api/usuarios/bitacora/acciones')),
  });
}
