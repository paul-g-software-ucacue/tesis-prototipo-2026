package ec.mdd.firma.puertos.salida;

import ec.mdd.firma.dominio.CertificadoDeLaFirma;
import ec.mdd.firma.dominio.ResultadoDeComprobacion;
import java.util.List;
import java.util.Optional;

/** Esqueleto (F14-H02, T008). */
public interface ValidadorDeFirmas {

    HallazgosDeLaFirma validar(byte[] vigente, byte[] subido);

    record HallazgosDeLaFirma(
            List<ResultadoDeComprobacion> evaluadas, Optional<CertificadoDeLaFirma> certificado) {}
}
