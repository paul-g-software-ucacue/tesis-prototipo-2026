package ec.mdd.firma.dominio;

import java.io.Serial;

/**
 * Un firmante de la cadena no tiene titular o nombre en el periodo (F13-H02, FR-008): sin él, el
 * bloque de firmas no se puede armar. La generación falla con este motivo, que nombra el cargo.
 */
public final class FirmanteSinNombre extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;

    private final String cargo;

    /** Recibe el cargo sin nombre. */
    public FirmanteSinNombre(String cargo) {
        super("Falta el nombre del " + cargo + ".");
        this.cargo = cargo;
    }

    /** El cargo sin nombre. */
    public String cargo() {
        return cargo;
    }
}
