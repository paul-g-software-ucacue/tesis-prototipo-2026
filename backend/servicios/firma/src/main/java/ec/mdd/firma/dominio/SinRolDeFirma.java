package ec.mdd.firma.dominio;

import java.io.Serial;

/**
 * Quien pide generar el documento no tiene el rol de la etapa que Aprobación entregó con la
 * solicitud (F13-H02, FR-012). El controlador la traduce al 403 común, sin decir qué falta, como
 * {@code RolDeOtraEtapa} en Validación OCS.
 */
public final class SinRolDeFirma extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;

    /** Sin datos. */
    public SinRolDeFirma() {
        super("SinRolDeFirma");
    }
}
