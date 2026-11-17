package ec.mdd.notificaciones.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Una notificación (glosario): un mensaje en la bandeja interna de un usuario por un hecho del
 * flujo (RF-41). Su contenido no cambia nunca; solo se marca como leída, una vez (principio VII,
 * FR-008, FR-009).
 *
 * @param id su identificador
 * @param idEvento el evento que la originó; con el destinatario y la carrera, la hace única
 * @param destinatario el usuario de la bandeja
 * @param carrera la carrera devuelta, solo en una devolución
 * @param periodo el periodo del hecho
 * @param titulo qué pasó
 * @param detalle el detalle; puede estar vacío
 * @param enlace la ruta de la SPA donde se ve el hecho
 * @param autor quién hizo la transición, o {@code sistema}
 * @param autorNombre su nombre para mostrar al recibirla, o «el sistema»
 * @param momento cuándo ocurrió el hecho
 * @param recibidaEn cuándo llegó a la bandeja
 * @param leidaEn cuándo se leyó por primera vez; vacío si no está leída
 */
public record Notificacion(
        UUID id,
        UUID idEvento,
        String destinatario,
        Optional<String> carrera,
        String periodo,
        String titulo,
        String detalle,
        String enlace,
        String autor,
        String autorNombre,
        Instant momento,
        Instant recibidaEn,
        Optional<Instant> leidaEn) {

    /** Exige los datos. */
    public Notificacion {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(idEvento, "idEvento");
        Objects.requireNonNull(destinatario, "destinatario");
        Objects.requireNonNull(carrera, "carrera");
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(titulo, "titulo");
        Objects.requireNonNull(detalle, "detalle");
        Objects.requireNonNull(enlace, "enlace");
        Objects.requireNonNull(autor, "autor");
        Objects.requireNonNull(autorNombre, "autorNombre");
        Objects.requireNonNull(momento, "momento");
        Objects.requireNonNull(recibidaEn, "recibidaEn");
        Objects.requireNonNull(leidaEn, "leidaEn");
    }

    /** Si ya se leyó. */
    public boolean leida() {
        return leidaEn.isPresent();
    }

    /**
     * La notificación leída en {@code ahora}. Si ya estaba leída, devuelve esta misma, con su
     * primera lectura: marcar dos veces no cambia nada (FR-008).
     *
     * @param ahora el momento de la lectura
     * @return la notificación leída
     */
    public Notificacion marcarLeida(Instant ahora) {
        Objects.requireNonNull(ahora, "ahora");
        if (leida()) {
            return this;
        }
        return new Notificacion(
                id,
                idEvento,
                destinatario,
                carrera,
                periodo,
                titulo,
                detalle,
                enlace,
                autor,
                autorNombre,
                momento,
                recibidaEn,
                Optional.of(ahora));
    }
}
