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
 * consultar cada {@code intervalo} milisegundos, hasta {@link CONSULTAS_MAXIMAS} veces.
 */
export function useDocumentoFirmable(periodo: string, intervalo = 3000) {
  return useQuery({
    queryKey: [...CLAVE_DOCUMENTO, periodo],
    queryFn: () =>
      llamar(
        apiFirma.GET('/api/firma/periodos/{periodo}/documento', {
          params: { path: { periodo } },
        }),
      ),
    refetchInterval: (consulta) => {
      const estado = consulta.state.data?.estado;
      const esperando = estado === 'EN_CURSO' || estado === 'SIN_GENERAR';
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
