// Los tipos y la consulta de la pantalla «Preparación del periodo» (contratos/cargas, F02-H02): el
// tablero de preparación de un periodo y los periodos para elegir.
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { apiCargas, apiGeneracion, llamar } from '../../api/cliente';
import type { components } from '../../api/generado/cargas';
import { FalloDeApi } from '../../errores/traducir';
import { PERIODO } from '../plantillas/consultas';

export type PreparacionDelPeriodo = components['schemas']['PreparacionDelPeriodo'];
export type CargaDelPeriodo = components['schemas']['CargaDelPeriodo'];
export type LoQueFalta = components['schemas']['LoQueFalta'];
export type EstadoDeLaCarga = components['schemas']['EstadoDeLaCarga'];

/** La clave del distributivo de todos los periodos, para recargarlo después de crearlo. */
export const CLAVE_DISTRIBUTIVO = ['generacion', 'distributivo'] as const;

/** El distributivo del periodo, o null si todavía no tiene (F06-H03, RF-56). */
export function useDistributivo(periodo: string | undefined) {
  return useQuery({
    queryKey: [...CLAVE_DISTRIBUTIVO, periodo],
    queryFn: async () => {
      try {
        return await llamar(
          apiGeneracion.GET('/api/generacion/periodos/{periodo}/distributivo', {
            params: { path: { periodo: periodo ?? '' } },
          }),
        );
      } catch (fallo) {
        if (fallo instanceof FalloDeApi && fallo.estado === 404) {
          return null;
        }
        throw fallo;
      }
    },
    enabled: Boolean(periodo),
  });
}

/** Crea el distributivo: el servidor vuelve a mirar si el periodo está listo (RF-56). */
export function crearDistributivo(periodo: string) {
  return llamar(
    apiGeneracion.POST('/api/generacion/periodos/{periodo}/distributivo', {
      params: { path: { periodo } },
    }),
  );
}

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
