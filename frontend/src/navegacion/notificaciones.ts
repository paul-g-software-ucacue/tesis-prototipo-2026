// La bandeja del usuario de la sesión (contratos/notificaciones, F16-H01): el número sin leer para
// la campana, la lista y marcar como leída. Cada usuario ve solo la suya (D-108).
import { useQuery } from '@tanstack/react-query';
import { apiNotificaciones, ENCABEZADO_AUTOMATICA, llamar } from '../api/cliente';
import type { components } from '../api/generado/notificaciones';

export type Notificacion = components['schemas']['Notificacion'];

/** Cada cuánto la campana vuelve a contar las sin leer (SC-004: menos de un minuto). */
export const INTERVALO_DE_LA_CAMPANA = 30_000;

/** La clave de la bandeja, para recargarla después de marcar una notificación. */
export const CLAVE_BANDEJA = ['notificaciones', 'bandeja'] as const;

/**
 * El número sin leer: al montar, cada 30 s y al volver el foco a la ventana. Es una consulta
 * automática: no cuenta como actividad del usuario para renovar la sesión (ADR-020).
 */
export function useSinLeer() {
  return useQuery({
    queryKey: [...CLAVE_BANDEJA, 'sin-leer'],
    queryFn: () =>
      llamar(
        apiNotificaciones.GET('/api/notificaciones/bandeja/sin-leer', {
          headers: { [ENCABEZADO_AUTOMATICA]: '1' },
        }),
      ),
    refetchInterval: INTERVALO_DE_LA_CAMPANA,
    refetchOnWindowFocus: true,
  });
}

/** Las notificaciones de la bandeja, de la más nueva a la más antigua; solo con la lista abierta. */
export function useBandeja(abierta: boolean) {
  return useQuery({
    queryKey: [...CLAVE_BANDEJA, 'lista'],
    queryFn: () =>
      llamar(
        apiNotificaciones.GET('/api/notificaciones/bandeja', { params: { query: { limite: 50 } } }),
      ),
    enabled: abierta,
  });
}

/** Marca una notificación como leída; si ya lo estaba, no cambia nada. */
export function marcarLeida(id: string) {
  return llamar(
    apiNotificaciones.POST('/api/notificaciones/bandeja/{id}/lectura', {
      params: { path: { id } },
    }),
  );
}
