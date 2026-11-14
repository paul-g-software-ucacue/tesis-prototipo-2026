package ec.mdd.firma.dominio;

import java.time.Instant;

/** Esqueleto (F14-H02, T007). */
public record FirmaRegistrada(
        int orden, String sha256, int tamano, String subidoPor, Instant subidoEn) {}
