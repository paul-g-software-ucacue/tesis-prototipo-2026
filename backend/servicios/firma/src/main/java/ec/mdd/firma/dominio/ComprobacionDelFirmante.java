package ec.mdd.firma.dominio;

import java.time.Instant;
import java.util.List;

/** Esqueleto (F14-H03, T006). */
public final class ComprobacionDelFirmante {

    private ComprobacionDelFirmante() {}

    public static List<ResultadoDeComprobacion> evaluar(
            CertificadoDeLaFirma certificado, Instant subida, FirmasDelDocumento firmas) {
        throw new UnsupportedOperationException();
    }
}
