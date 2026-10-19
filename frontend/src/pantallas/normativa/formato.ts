// Formatos de la pantalla «Normativa», compartidos por la consulta y la edición.

export const SIN_VALOR = '—';

export const SEVERIDADES: Record<string, string> = { DURA: 'Dura', BLANDA: 'Blanda' };

/** «T2» → «Tipo 2»; «TODOS» → «Todos». */
export function tipoIst(valor: string): string {
  return valor === 'TODOS' ? 'Todos' : `Tipo ${valor.slice(1)}`;
}

/** «2024-03-11» → «11/03/2024», sin pasar por la zona horaria. */
export function fecha(iso: string): string {
  const [anio, mes, dia] = iso.split('-');
  return `${dia}/${mes}/${anio}`;
}

/** El campo de una fila en los detalles de un 400, como lo nombra el servidor (research §5). */
export function campoDeFila(fila: { perfil: string; aplicaA: string }): string {
  return `tablaCargaHoraria[${fila.perfil}/${fila.aplicaA}]`;
}

/** El campo de un parámetro en los detalles de un 400. */
export function campoDeParametro(regla: string, parametro: string): string {
  return `reglas[${regla}].${parametro}`;
}
