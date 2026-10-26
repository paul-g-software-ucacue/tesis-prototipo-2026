package ec.mdd.plataforma.cargas;

/**
 * Un campo que cambia: su valor de ahora y el del archivo.
 *
 * @param campo el encabezado de la columna
 * @param ahora el valor que tiene el dueño; vacío si no tiene
 * @param archivo el valor del archivo; vacío si no tiene
 */
public record CambioDeCampo(String campo, String ahora, String archivo) {}
