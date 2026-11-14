package ec.mdd.firma.dominio;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * El resultado de la validación básica de un PDF subido por el firmante en turno: el informe de
 * validación (D-23, D-103). Hay uno por subida, aceptada o rechazada; el aceptado es la firma de su
 * paso.
 *
 * <p>Lleva las cuatro comprobaciones de {@link Comprobacion}, en su orden. Cada una depende de la
 * anterior, así que después de una no aprobada las siguientes están no evaluadas, y una no evaluada
 * solo puede seguir a una no aprobada. Se acepta solo con las cuatro aprobadas, y entonces lleva el
 * certificado de la firma nueva.
 *
 * @param id su identificador
 * @param documento el documento firmable
 * @param orden el paso en turno al subir
 * @param subidoPor quién subió el archivo
 * @param subidoEn cuándo
 * @param sha256Subido el SHA-256 del archivo subido
 * @param tamano su tamaño en bytes
 * @param comprobaciones las cuatro, en orden
 * @param certificado el de la firma nueva, si había una sola legible
 */
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

    /** Exige los datos, las cuatro comprobaciones en su orden y el certificado si se acepta. */
    public ResultadoValidacion {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(documento, "documento");
        Objects.requireNonNull(subidoPor, "subidoPor");
        Objects.requireNonNull(subidoEn, "subidoEn");
        Objects.requireNonNull(sha256Subido, "sha256Subido");
        Objects.requireNonNull(certificado, "certificado");
        if (orden < 1 || tamano < 1) {
            throw new IllegalArgumentException("el orden y el tamaño son positivos");
        }
        comprobaciones = List.copyOf(comprobaciones);
        exigirOrden(comprobaciones);
        if (comprobaciones.stream().allMatch(una -> una.resultado() == Resultado.APROBADA)
                && certificado.isEmpty()) {
            throw new IllegalArgumentException("una validación aceptada lleva el certificado");
        }
    }

    private static void exigirOrden(List<ResultadoDeComprobacion> comprobaciones) {
        Comprobacion[] orden = Comprobacion.values();
        if (comprobaciones.size() != orden.length) {
            throw new IllegalArgumentException("son cuatro comprobaciones");
        }
        boolean despuesDeUnaNoAprobada = false;
        for (int i = 0; i < orden.length; i++) {
            ResultadoDeComprobacion una = comprobaciones.get(i);
            if (una.comprobacion() != orden[i]) {
                throw new IllegalArgumentException("las comprobaciones van en su orden");
            }
            if (despuesDeUnaNoAprobada != (una.resultado() == Resultado.NO_EVALUADA)) {
                throw new IllegalArgumentException(
                        "después de una no aprobada, las siguientes quedan no evaluadas, y solo ellas");
            }
            despuesDeUnaNoAprobada |= una.resultado() != Resultado.APROBADA;
        }
    }

    /** Si las cuatro comprobaciones se aprobaron: la firma se registra. */
    public boolean aceptada() {
        return primerRechazo().isEmpty();
    }

    /** La primera comprobación rechazada, que da el código del 422; vacío si se aceptó. */
    public Optional<Comprobacion> primerRechazo() {
        return comprobaciones.stream()
                .filter(una -> una.resultado() == Resultado.RECHAZADA)
                .map(ResultadoDeComprobacion::comprobacion)
                .findFirst();
    }

    /** Esqueleto (F14-H02, T010). */
    public ResultadoValidacion ganadaPorOtraSubida() {
        throw new UnsupportedOperationException();
    }
}
