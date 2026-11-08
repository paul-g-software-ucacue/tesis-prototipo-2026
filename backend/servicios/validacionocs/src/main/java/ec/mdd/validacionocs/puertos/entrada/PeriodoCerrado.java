package ec.mdd.validacionocs.puertos.entrada;

/** Error del flujo del distributivo (F12-H02); lo traduce el catálogo de errores. */
public final class PeriodoCerrado extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Sin datos: el código del catálogo lo dice todo. */
    public PeriodoCerrado() {
        super("PeriodoCerrado");
    }
}
