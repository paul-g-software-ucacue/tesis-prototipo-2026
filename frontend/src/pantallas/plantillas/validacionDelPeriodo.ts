// El periodo de la confirmación «sin proyectos» (F01-H05): la regla del contrato, la de la carga 3,
// revisada antes de enviar, con un motivo que dice qué hacer (principio XIV). El servidor decide si
// el periodo existe y si tiene proyectos asignables.
import { PERIODO } from './consultas';

/** Por qué el periodo no se puede enviar todavía, o nada si cumple su regla. */
export function motivoDelPeriodo(periodo: string): string | null {
  if (periodo.trim() === '') {
    return 'Escribe el periodo.';
  }
  return PERIODO.test(periodo.trim()) ? null : 'Escribe el periodo como 2026-I o 2026-II.';
}
