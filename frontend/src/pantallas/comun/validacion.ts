// Lo común de la validación en pantalla de los formularios (principio XIV, D-72): qué admite cada
// campo mientras se escribe y cuántos campos hay que corregir. Lo usan los formularios de los
// docentes (F03-H04), de los cargos y de los proyectos (F03-H05).

/** Qué caracteres admite un campo mientras se escribe. */
export type Filtro = 'digitos' | 'decimal' | 'letras' | 'codigo' | 'correo';

/** El texto sin lo que el campo no admite, y como mucho `largo` caracteres. */
export function filtrar(filtro: Filtro | undefined, texto: string, largo?: number) {
  let limpio = texto;
  if (filtro === 'digitos') {
    limpio = texto.replace(/\D/g, '');
  } else if (filtro === 'decimal') {
    // Dígitos y una sola coma o punto: «2,5» años.
    const [entera = '', ...resto] = texto.replace(/[^\d.,]/g, '').split(/[.,]/);
    limpio = resto.length ? `${entera},${resto.join('')}` : entera;
  } else if (filtro === 'letras') {
    limpio = texto.replace(/[^\p{L} '-]/gu, '');
  } else if (filtro === 'codigo') {
    limpio = texto.replace(/[^0-9A-Za-z-]/g, '').toUpperCase();
  } else if (filtro === 'correo') {
    limpio = texto.replace(/\s/g, '');
  }
  return largo === undefined ? limpio : limpio.slice(0, largo);
}

/** «1 campo», «3 campos». */
export function campos(cantidad: number) {
  return cantidad === 1 ? '1 campo' : `${String(cantidad)} campos`;
}
