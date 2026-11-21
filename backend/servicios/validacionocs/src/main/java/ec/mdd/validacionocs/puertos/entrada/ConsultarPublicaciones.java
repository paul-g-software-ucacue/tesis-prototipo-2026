package ec.mdd.validacionocs.puertos.entrada;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * La publicación vigente de cada periodo, para Reporting (F20-H01, D-115, ADR-023). Es la última
 * transición a PUBLICADO (D-111); durante una reforma, la anterior, que sigue siendo la oficial
 * hasta que se publique la nueva (D-113).
 */
public interface ConsultarPublicaciones {

    /**
     * @return una por periodo con distributivo publicado, ordenadas por periodo
     */
    List<PublicacionVigente> vigentes();

    /**
     * La versión publicada vigente de un periodo.
     *
     * @param periodo el código del periodo
     * @param version la versión publicada
     * @param hash su hash de contenido, el de la transición
     * @param publicadaPor quién publicó
     * @param publicadaEn cuándo
     * @param enReforma si hay una reforma en curso
     */
    record PublicacionVigente(
            String periodo,
            int version,
            String hash,
            String publicadaPor,
            Instant publicadaEn,
            boolean enReforma) {

        /** Exige los datos. */
        public PublicacionVigente {
            Objects.requireNonNull(periodo, "periodo");
            Objects.requireNonNull(hash, "hash");
            Objects.requireNonNull(publicadaPor, "publicadaPor");
            Objects.requireNonNull(publicadaEn, "publicadaEn");
        }
    }
}
