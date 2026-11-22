package ec.mdd.reporting.dominio;

import java.util.OptionalInt;

/**
 * El archivo de mapeo no se puede leer: no es UTF-8 ni YAML, o tiene anclas, alias o etiquetas
 * ({@code REPORTING_ARCHIVO_ILEGIBLE}). El mensaje del lector solo va al log; al usuario le llega,
 * si se conoce, la línea.
 */
public class ArchivoIlegible extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Integer linea;

    /**
     * @param detalle qué pasó, para el log
     * @param linea la línea, desde 1; {@code null} si no se conoce
     * @param causa la excepción del lector, si la hay
     */
    public ArchivoIlegible(String detalle, Integer linea, Throwable causa) {
        super(detalle, causa);
        this.linea = linea;
    }

    /** La línea del problema; vacía si no se conoce. */
    public OptionalInt linea() {
        return linea == null ? OptionalInt.empty() : OptionalInt.of(linea);
    }
}
