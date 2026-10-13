package ec.mdd.plataforma.errores.nucleo;

/**
 * Un campo de la solicitud que no pasó la validación (esquema {@code DetalleError}).
 *
 * @param campo nombre del campo como en el JSON del contrato
 * @param descripcion qué le falta o qué tiene mal, en español
 */
public record DetalleRespuesta(String campo, String descripcion) {}
