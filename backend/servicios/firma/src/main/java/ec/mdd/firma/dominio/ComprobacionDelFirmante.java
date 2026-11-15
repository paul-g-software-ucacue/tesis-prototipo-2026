package ec.mdd.firma.dominio;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Las tres comprobaciones de F14-H03 (RF-39, D-23, D-104), después de las cuatro de F14-H02:
 *
 * <ol>
 *   <li><b>Vigencia:</b> el certificado está vigente al subir. La confianza ya se comprobó a la
 *       fecha de la firma (D-103); esta comprueba la de la subida, sin sello de tiempo (ADR-007).
 *   <li><b>Orden:</b> si el CN es el nombre impreso de un paso posterior, y no el del paso en
 *       turno, la firma llegó antes de tiempo. Va antes que el firmante para dar el motivo más
 *       útil.
 *   <li><b>Firmante:</b> el CN es el nombre que el PDF imprime en el paso en turno.
 * </ol>
 *
 * <p>Los nombres se comparan normalizados ({@link NombreDeFirmante}). No distingue homónimos
 * (D-23). Con la misma persona en varios pasos, como en la instalación del autor (ADR-017), su CN
 * es el del paso en turno y no está fuera de orden. El paso no se reconoce por el campo de firma:
 * FirmaEC agrega los suyos (hallazgo de F14-H02).
 */
public final class ComprobacionDelFirmante {

    private ComprobacionDelFirmante() {}

    /**
     * Las comprobaciones en orden, hasta la primera no aprobada.
     *
     * @param certificado el de la firma nueva, que ya pasó la confianza
     * @param subida el momento de la subida
     * @param firmas el documento con su paso en turno, que tiene nombre impreso
     * @return de una a tres comprobaciones
     * @throws PasoSinNombre si el paso en turno no tiene nombre impreso
     */
    public static List<ResultadoDeComprobacion> evaluar(
            CertificadoDeLaFirma certificado, Instant subida, FirmasDelDocumento firmas) {
        Objects.requireNonNull(certificado, "certificado");
        Objects.requireNonNull(subida, "subida");
        FirmasDelDocumento.PasoEnTurno turno =
                firmas.pasoEnTurno()
                        .orElseThrow(
                                () -> new IllegalStateException("el documento no tiene turno"));
        String esperado =
                NombreDeFirmante.normalizar(
                        turno.firmante().nombre().orElseThrow(PasoSinNombre::new));
        String cn = NombreDeFirmante.normalizar(certificado.nombre());
        List<ResultadoDeComprobacion> evaluadas = new ArrayList<>();
        if (subida.isBefore(certificado.validoDesde())
                || subida.isAfter(certificado.validoHasta())) {
            evaluadas.add(
                    ResultadoDeComprobacion.rechazada(
                            Comprobacion.VIGENCIA,
                            "El certificado de la firma no está vigente al subir el PDF."));
            return List.copyOf(evaluadas);
        }
        evaluadas.add(ResultadoDeComprobacion.aprobada(Comprobacion.VIGENCIA));
        boolean dePasoPosterior =
                firmas.firmantes().stream()
                        .filter(uno -> uno.orden() > turno.paso().orden())
                        .flatMap(uno -> uno.nombre().stream())
                        .map(NombreDeFirmante::normalizar)
                        .anyMatch(cn::equals);
        if (!cn.equals(esperado) && dePasoPosterior) {
            evaluadas.add(
                    ResultadoDeComprobacion.rechazada(
                            Comprobacion.ORDEN,
                            "La firma es de la persona de un paso posterior: primero firma la"
                                    + " persona del paso en turno."));
            return List.copyOf(evaluadas);
        }
        evaluadas.add(ResultadoDeComprobacion.aprobada(Comprobacion.ORDEN));
        evaluadas.add(
                cn.equals(esperado)
                        ? ResultadoDeComprobacion.aprobada(Comprobacion.FIRMANTE)
                        : ResultadoDeComprobacion.rechazada(
                                Comprobacion.FIRMANTE,
                                "El certificado no es de la persona que el documento indica para"
                                        + " este paso."));
        return List.copyOf(evaluadas);
    }
}
