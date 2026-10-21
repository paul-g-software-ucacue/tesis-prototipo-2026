package ec.mdd.academico.dominio;

/**
 * El periodo no tiene versión de normativa y por eso no puede salir de Planificación: primero se
 * abre (FR-004, F02-H01).
 */
public class PeriodoSinVersion extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param codigo el código del periodo
     */
    public PeriodoSinVersion(String codigo) {
        super("El periodo " + codigo + " no tiene versión de normativa");
    }
}
