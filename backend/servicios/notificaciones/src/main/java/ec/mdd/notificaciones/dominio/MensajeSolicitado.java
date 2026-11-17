package ec.mdd.notificaciones.dominio;

import java.util.Objects;

/**
 * Un mensaje de una solicitud, con su destino. Notificaciones no interpreta el título ni el
 * detalle: los guarda tal como llegan (D-108).
 *
 * @param destino a quién toca
 * @param titulo qué pasó, de 1 a 200 caracteres
 * @param detalle hasta 1000 caracteres; puede estar vacío
 * @param enlace la ruta de la SPA donde se ve el hecho
 */
public record MensajeSolicitado(
        DestinoDelMensaje destino, String titulo, String detalle, String enlace) {

    /** Exige los datos. */
    public MensajeSolicitado {
        Objects.requireNonNull(destino, "destino");
        Objects.requireNonNull(titulo, "titulo");
        Objects.requireNonNull(detalle, "detalle");
        Objects.requireNonNull(enlace, "enlace");
    }
}
