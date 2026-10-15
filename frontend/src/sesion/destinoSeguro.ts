// Después de iniciar sesión se vuelve a la ruta pedida (D-50), pero solo si es una ruta interna de
// la SPA: así el parámetro destino no sirve para redirigir a otro sitio.

/** El destino si es una ruta interna (empieza con una sola /); si no, la pantalla de inicio. */
export function destinoSeguro(valor: string | null | undefined): string {
  if (!valor || !valor.startsWith('/') || valor.startsWith('//') || valor.startsWith('/\\')) {
    return '/';
  }
  return valor;
}
