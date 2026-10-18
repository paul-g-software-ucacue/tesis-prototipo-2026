package ec.mdd.plataforma.errores.nucleo;

import java.util.List;

/**
 * Una solicitud que cumple el contrato, pero no una regla de entrada que el contrato no puede
 * expresar (por ejemplo, un tipo de permiso que la funcionalidad no admite). Responde 400 {@code
 * COMUN_SOLICITUD_INVALIDA} con sus detalles, igual que la validación del contrato (ADR-016).
 *
 * <p>La lanza un adaptador de entrada al traducir un error de dominio o de aplicación: los detalles
 * van a la respuesta, así que nunca llevan datos sensibles ni textos técnicos.
 */
public class SolicitudInvalida extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient List<DetalleRespuesta> detalles;

    /**
     * @param detalles un detalle por campo, en español; al menos uno
     */
    public SolicitudInvalida(List<DetalleRespuesta> detalles) {
        super("Solicitud inválida: " + detalles.size() + " detalles");
        if (detalles.isEmpty()) {
            throw new IllegalArgumentException("Una solicitud inválida necesita un detalle");
        }
        this.detalles = List.copyOf(detalles);
    }

    /** Los detalles de la respuesta. */
    public List<DetalleRespuesta> detalles() {
        return detalles;
    }
}
