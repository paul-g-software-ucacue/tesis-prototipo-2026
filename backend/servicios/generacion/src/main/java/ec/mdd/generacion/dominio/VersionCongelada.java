package ec.mdd.generacion.dominio;

import java.io.Serial;

/**
 * Un cambio sobre una versión ya aprobada y congelada para la firma (F13-H01, D-100, RF-40). Se
 * comprueba antes que el parcial enviado y que la revisión, porque es la causa más fuerte: un
 * cambio exige una versión nueva (RF-59). Responde {@code GENERACION_VERSION_CONGELADA}, con el
 * número de la versión en los detalles.
 */
public final class VersionCongelada extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;

    private final int version;

    /** Recibe el número de la versión congelada. */
    public VersionCongelada(int version) {
        super("VersionCongelada");
        this.version = version;
    }

    /** El número de la versión congelada. */
    public int version() {
        return version;
    }
}
