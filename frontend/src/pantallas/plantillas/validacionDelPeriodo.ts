// El periodo de la confirmación «sin proyectos» (F01-H05): la regla del contrato, la de Académico,
// revisada antes de enviar, con un motivo que dice qué hacer (principio XIV). El servidor decide si
// el periodo existe y si tiene proyectos asignables.
import { PERIODO } from './consultas';

/** Por qué el periodo no se puede enviar todavía, o nada si cumple su regla. */
export function motivoDelPeriodo(periodo: string): string | null {
  if (periodo.trim() === '') {
    return 'Escribe el periodo.';
  }
  return PERIODO.test(periodo.trim())
    ? null
    : 'Escribe el código del periodo como está en «Periodos»: solo letras, números, punto, guion y guion bajo, hasta 20 caracteres.';
}
