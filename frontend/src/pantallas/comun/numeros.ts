// Cómo se leen los números de la afinidad: con coma decimal y hasta dos decimales.
const NUMERO = new Intl.NumberFormat('es-EC', { maximumFractionDigits: 2 });

/** Un número con coma decimal y hasta dos decimales, sin redondear más. */
export function numero(valor: number) {
  return NUMERO.format(valor);
}

export function puntos(valor: number) {
  return `${numero(valor)} ${valor === 1 ? 'punto' : 'puntos'}`;
}
