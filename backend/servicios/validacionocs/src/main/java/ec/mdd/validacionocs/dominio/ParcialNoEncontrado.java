package ec.mdd.validacionocs.dominio;

/**
 * La carrera no tiene parcial en el distributivo: no tiene oferta en el periodo (F12-H02). Responde
 * {@code VALIDACIONOCS_PARCIAL_NO_ENCONTRADO}.
 */
public final class ParcialNoEncontrado extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Sin datos: el código del catálogo lo dice todo. */
    public ParcialNoEncontrado() {
        super("ParcialNoEncontrado");
    }
}
