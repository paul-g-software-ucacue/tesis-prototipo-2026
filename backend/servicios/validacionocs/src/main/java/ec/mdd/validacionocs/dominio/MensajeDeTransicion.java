package ec.mdd.validacionocs.dominio;

import java.util.Objects;

/**
 * Un mensaje de una transición para su destino, tal como lo guarda Notificaciones (F16-H01,
 * contracts/eventos.md).
 *
 * @param destino a quién toca
 * @param titulo qué pasó, de 1 a 200 caracteres
 * @param detalle hasta 1000 caracteres
 * @param enlace la ruta de la SPA donde se ve el hecho
 */
public record MensajeDeTransicion(
        DestinoDeNotificacion destino, String titulo, String detalle, String enlace) {

    /** Exige los datos. */
    public MensajeDeTransicion {
        Objects.requireNonNull(destino, "destino");
        Objects.requireNonNull(titulo, "titulo");
        Objects.requireNonNull(detalle, "detalle");
        Objects.requireNonNull(enlace, "enlace");
    }
}
