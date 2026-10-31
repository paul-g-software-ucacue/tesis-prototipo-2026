// Los tipos y la consulta de la pantalla «Preparación del periodo» (contratos/cargas, F02-H02): el
// tablero de preparación de un periodo y los periodos para elegir.
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { apiCargas, llamar } from '../../api/cliente';
import type { components } from '../../api/generado/cargas';

export type PreparacionDelPeriodo = components['schemas']['PreparacionDelPeriodo'];
export type CargaDelPeriodo = components['schemas']['CargaDelPeriodo'];
export type LoQueFalta = components['schemas']['LoQueFalta'];
export type EstadoDeLaCarga = components['schemas']['EstadoDeLaCarga'];

/** El código de un periodo, como en la carga 3: «2026-I» o «2026-II» (contrato, principio XIV). */
export const PERIODO = /^[0-9]{4}-(I|II)$/;

/**
 * El tablero de un periodo; sin periodo, el del que empieza más tarde. Un periodo que no cumple su
 * formato no se pide.
 */
export function usePreparacion(periodo: string | null) {
  return useQuery({
    queryKey: ['cargas', 'preparacion', periodo],
    queryFn: () =>
      llamar(
        apiCargas.GET('/api/cargas/preparacion', {
          params: { query: periodo ? { periodo } : {} },
        }),
      ),
    enabled: periodo === null || PERIODO.test(periodo),
    placeholderData: keepPreviousData,
  });
}
