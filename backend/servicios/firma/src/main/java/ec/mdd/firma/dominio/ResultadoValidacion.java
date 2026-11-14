package ec.mdd.firma.dominio;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Esqueleto (F14-H02, T006). */
public record ResultadoValidacion(
        UUID id,
        UUID documento,
        int orden,
        String subidoPor,
        Instant subidoEn,
        String sha256Subido,
        int tamano,
        List<ResultadoDeComprobacion> comprobaciones,
        Optional<CertificadoDeLaFirma> certificado) {

    public boolean aceptada() {
        throw new UnsupportedOperationException();
    }

    public Optional<Comprobacion> primerRechazo() {
        throw new UnsupportedOperationException();
    }
}
