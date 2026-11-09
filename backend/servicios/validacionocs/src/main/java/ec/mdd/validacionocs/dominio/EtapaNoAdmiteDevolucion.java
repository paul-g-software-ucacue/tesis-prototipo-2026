package ec.mdd.validacionocs.dominio;

/**
 * Se devuelve solo desde EN_CONSOLIDACION (F12-H05, D-98). Responde {@code
 * VALIDACIONOCS_ETAPA_NO_ADMITE_DEVOLUCION}.
 */
public final class EtapaNoAdmiteDevolucion extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Sin datos: el código del catálogo lo dice todo. */
    public EtapaNoAdmiteDevolucion() {
        super("EtapaNoAdmiteDevolucion");
    }
}
