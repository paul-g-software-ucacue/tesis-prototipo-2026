// El periodo de una pantalla con selector de periodo (F16-H01, D-108). Una notificación lleva a su
// pantalla con ?periodo=: si ese periodo existe, se abre ese; si no, el más reciente en
// Planificación, y si no hay, el más reciente. Elegir otro lo deja en la dirección.
import { useSearchParams } from 'react-router';
import type { PeriodoAcademico } from './consultas';

/**
 * @param periodos los periodos, más reciente primero; undefined mientras llegan
 * @returns el periodo que se muestra y cómo elegir otro
 */
export function usePeriodoElegido(periodos: readonly PeriodoAcademico[] | undefined) {
  const [parametros, setParametros] = useSearchParams();
  const pedido = parametros.get('periodo');
  const periodo =
    periodos?.find((uno) => uno.codigo === pedido)?.codigo ??
    periodos?.find((uno) => uno.estado === 'PLANIFICACION')?.codigo ??
    periodos?.[0]?.codigo;
  const elegir = (codigo: string) => setParametros({ periodo: codigo }, { replace: true });
  return [periodo, elegir] as const;
}
