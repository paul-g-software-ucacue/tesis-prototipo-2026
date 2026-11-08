package ec.mdd.usuarios.puertos.entrada;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Una entrada de la bitácora tal como se registró (F17-H01), para la consulta (esquema {@code
 * EntradaConsultada}, FR-005). No se agrega ni se reconstruye nada: los datos personales de un
 * docente siguen sin valores (D-91).
 *
 * @param id el {@code idEvento} que la trajo
 * @param servicio el servicio de origen
 * @param usuario quién hizo el cambio
 * @param accion qué acción fue
 * @param momento cuándo ocurrió en su servicio
 * @param periodo el periodo del cambio, si pertenece a uno
 * @param objeto sobre qué se hizo
 * @param anterior los valores antes del cambio; vacío al crear
 * @param nuevo los valores después del cambio; vacío al quitar
 */
public record EntradaConsultada(
        UUID id,
        String servicio,
        String usuario,
        String accion,
        Instant momento,
        Optional<String> periodo,
        Map<String, Object> objeto,
        Optional<Map<String, Object>> anterior,
        Optional<Map<String, Object>> nuevo) {

    /** Exige los campos obligatorios. */
    public EntradaConsultada {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(servicio, "servicio");
        Objects.requireNonNull(usuario, "usuario");
        Objects.requireNonNull(accion, "accion");
        Objects.requireNonNull(momento, "momento");
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(objeto, "objeto");
        Objects.requireNonNull(anterior, "anterior");
        Objects.requireNonNull(nuevo, "nuevo");
    }
}
