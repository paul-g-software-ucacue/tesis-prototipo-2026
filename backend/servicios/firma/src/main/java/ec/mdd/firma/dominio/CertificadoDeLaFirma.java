package ec.mdd.firma.dominio;

import java.time.Instant;

/** Esqueleto (F14-H02, T006). */
public record CertificadoDeLaFirma(
        String nombre, String emisor, String serie, Instant validoDesde, Instant validoHasta) {}
