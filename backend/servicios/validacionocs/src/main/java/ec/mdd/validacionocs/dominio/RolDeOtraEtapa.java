package ec.mdd.validacionocs.dominio;

/**
 * Quien intenta mover el distributivo no puede (F12-H02, FR-009, D-64). El adaptador de entrada lo
 * responde con el 403 común, sin decir qué falta (ADR-021).
 */
public final class RolDeOtraEtapa extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Sin datos: el 403 no dice qué falta. */
    public RolDeOtraEtapa() {
        super("RolDeOtraEtapa");
    }
}
