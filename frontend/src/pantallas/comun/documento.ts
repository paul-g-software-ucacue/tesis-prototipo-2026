// El documento firmable de la versión congelada (contratos/firma, F13-H02): su estado y el
// reintento cuando no se pudo generar.
import { useQuery } from '@tanstack/react-query';
import { apiFirma, llamar } from '../../api/cliente';
import type { components } from '../../api/generado/firma';

export type EstadoDelDocumento = components['schemas']['EstadoDelDocumento'];

/** La clave del documento, para recargarlo después de generarlo. */
export const CLAVE_DOCUMENTO = ['firma', 'documento'] as const;

// Mientras se genera, se vuelve a consultar; hasta 1 minuto con el intervalo por defecto.
const CONSULTAS_MAXIMAS = 20;

/**
 * El estado del documento firmable del periodo. Mientras está en curso o sin generar, vuelve a
 * consultar cada {@code intervalo} milisegundos, hasta {@link CONSULTAS_MAXIMAS} veces. Con
 * {@code version}, también mientras el documento sea de otra versión (F12-H04): al aprobar la
 * versión nueva, Firma responde con el de la anulada hasta que empieza a generar el suyo.
 */
export function useDocumentoFirmable(periodo: string, intervalo = 3000, version?: number) {
  return useQuery({
    queryKey: [...CLAVE_DOCUMENTO, periodo],
    queryFn: () =>
      llamar(
        apiFirma.GET('/api/firma/periodos/{periodo}/documento', {
          params: { path: { periodo } },
        }),
      ),
    refetchInterval: (consulta) => {
      const datos = consulta.state.data;
      const esperando =
        datos?.estado === 'EN_CURSO' ||
        datos?.estado === 'SIN_GENERAR' ||
        (version !== undefined && datos !== undefined && datos.version !== version);
      return esperando && consulta.state.dataUpdateCount < CONSULTAS_MAXIMAS ? intervalo : false;
    },
  });
}

/** Genera el documento que no se pudo generar al entrar en firma (FR-012). */
export function generarDocumentoFirmable(periodo: string) {
  return llamar(
    apiFirma.POST('/api/firma/periodos/{periodo}/documento', {
      params: { path: { periodo } },
    }),
  );
}
