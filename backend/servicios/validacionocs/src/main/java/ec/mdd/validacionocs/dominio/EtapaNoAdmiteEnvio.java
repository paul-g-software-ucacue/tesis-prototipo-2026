package ec.mdd.validacionocs.dominio;

/** Error del proceso de aprobación (F12-H02); lo traduce el catálogo de errores. */
public final class EtapaNoAdmiteEnvio extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Sin datos: el código del catálogo lo dice todo. */
    public EtapaNoAdmiteEnvio() {
        super("EtapaNoAdmiteEnvio");
    }
}
