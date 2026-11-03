package ec.mdd.validacionocs.dominio;

import java.io.Serial;
import java.util.OptionalInt;

/** El archivo está vacío, no es YAML o trae anclas, alias o tipos. */
public final class ArchivoIlegible extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;

    private final int linea;

    /** Sin una línea que señalar. */
    public ArchivoIlegible() {
        this(0);
    }

    /**
     * @param linea la línea del problema, desde 1
     */
    public ArchivoIlegible(int linea) {
        this.linea = linea;
    }

    /** La línea del problema, si se sabe. */
    public OptionalInt linea() {
        return linea > 0 ? OptionalInt.of(linea) : OptionalInt.empty();
    }
}
