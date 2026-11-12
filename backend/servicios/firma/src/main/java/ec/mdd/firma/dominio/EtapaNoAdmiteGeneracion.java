package ec.mdd.firma.dominio;

import java.io.Serial;

/**
 * Aprobación no pidió el documento firmable de este periodo: el distributivo no entró en firma
 * (F13-H02, D-101). Responde {@code FIRMA_ETAPA_NO_ADMITE_GENERACION}.
 */
public final class EtapaNoAdmiteGeneracion extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;

    /** Sin datos: el código del catálogo lo dice todo. */
    public EtapaNoAdmiteGeneracion() {
        super("EtapaNoAdmiteGeneracion");
    }
}
