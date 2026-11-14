package ec.mdd.firma.puertos.salida;

import ec.mdd.firma.dominio.CertificadoDeLaFirma;
import ec.mdd.firma.dominio.Comprobacion;
import ec.mdd.firma.dominio.Resultado;
import ec.mdd.firma.dominio.ResultadoDeComprobacion;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Lo que necesita leer el PDF de la validación básica (D-23, D-103): una sola firma nueva, su
 * integridad y su confianza. Detrás de este puerto quedan las bibliotecas de PDF y de criptografía.
 * La primera comprobación, el mismo documento, la hace el dominio antes de llamarlo.
 */
public interface ValidadorDeFirmas {

    /**
     * Revisa la firma nueva del archivo subido.
     *
     * <p>Nunca lanza por un archivo malo: un PDF ilegible es un rechazo con su motivo. Los motivos
     * son textos del sistema; los mensajes de las bibliotecas solo van al registro.
     *
     * @param vigente el archivo vigente, cuyo SHA-256 ya se verificó
     * @param subido el archivo subido, que empieza con los bytes del vigente
     * @return lo que se encontró
     */
    HallazgosDeLaFirma validar(byte[] vigente, byte[] subido);

    /**
     * Lo que se encontró en la firma nueva.
     *
     * @param evaluadas las comprobaciones desde {@link Comprobacion#UNA_FIRMA_NUEVA}, en orden,
     *     hasta la primera no aprobada
     * @param certificado el de la firma nueva, si había una sola legible
     */
    record HallazgosDeLaFirma(
            List<ResultadoDeComprobacion> evaluadas, Optional<CertificadoDeLaFirma> certificado) {

        /**
         * Exige de una a tres comprobaciones, en su orden, que solo terminan antes si no se aprobó.
         */
        public HallazgosDeLaFirma {
            evaluadas = List.copyOf(evaluadas);
            Objects.requireNonNull(certificado, "certificado");
            List<Comprobacion> orden =
                    List.of(
                            Comprobacion.UNA_FIRMA_NUEVA,
                            Comprobacion.INTEGRIDAD,
                            Comprobacion.CONFIANZA);
            if (evaluadas.isEmpty() || evaluadas.size() > orden.size()) {
                throw new IllegalArgumentException("de una a tres comprobaciones");
            }
            for (int i = 0; i < evaluadas.size(); i++) {
                ResultadoDeComprobacion una = evaluadas.get(i);
                boolean ultima = i == evaluadas.size() - 1;
                if (una.comprobacion() != orden.get(i)
                        || una.resultado() == Resultado.NO_EVALUADA
                        || (!ultima && una.resultado() != Resultado.APROBADA)) {
                    throw new IllegalArgumentException(
                            "las comprobaciones van en su orden hasta la primera no aprobada");
                }
            }
        }
    }
}
