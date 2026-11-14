package ec.mdd.firma.dominio;

import static ec.mdd.firma.dominio.Comprobacion.CONFIANZA;
import static ec.mdd.firma.dominio.Comprobacion.INTEGRIDAD;
import static ec.mdd.firma.dominio.Comprobacion.MISMO_DOCUMENTO;
import static ec.mdd.firma.dominio.Comprobacion.UNA_FIRMA_NUEVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResultadoValidacionTest {

    private static final CertificadoDeLaFirma CERTIFICADO =
            new CertificadoDeLaFirma(
                    "Firmante Uno Prueba (F1)",
                    "CN=CA de Prueba MDD Demo, O=MDD Demo, C=EC",
                    "1a2b",
                    Instant.parse("2026-10-13T10:00:00Z"),
                    Instant.parse("2029-10-12T10:00:00Z"));

    private static ResultadoValidacion con(
            List<ResultadoDeComprobacion> comprobaciones,
            Optional<CertificadoDeLaFirma> certificado) {
        return new ResultadoValidacion(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                "docente.demo",
                Instant.parse("2026-11-14T10:00:00Z"),
                "c".repeat(64),
                2048,
                comprobaciones,
                certificado);
    }

    private static List<ResultadoDeComprobacion> todasAprobadas() {
        return List.of(
                ResultadoDeComprobacion.aprobada(MISMO_DOCUMENTO),
                ResultadoDeComprobacion.aprobada(UNA_FIRMA_NUEVA),
                ResultadoDeComprobacion.aprobada(INTEGRIDAD),
                ResultadoDeComprobacion.aprobada(CONFIANZA));
    }

    @Test
    void dadasLasCuatroAprobadas_cuandoSeConsulta_entoncesEstaAceptadaSinRechazo() {
        ResultadoValidacion resultado = con(todasAprobadas(), Optional.of(CERTIFICADO));

        assertThat(resultado.aceptada()).isTrue();
        assertThat(resultado.primerRechazo()).isEmpty();
    }

    @Test
    void
            dadaLaIntegridadRechazada_cuandoSeConsulta_entoncesNoEstaAceptadaYElRechazoEsLaIntegridad() {
        ResultadoValidacion resultado =
                con(
                        List.of(
                                ResultadoDeComprobacion.aprobada(MISMO_DOCUMENTO),
                                ResultadoDeComprobacion.aprobada(UNA_FIRMA_NUEVA),
                                ResultadoDeComprobacion.rechazada(
                                        INTEGRIDAD, "La firma no corresponde al contenido."),
                                ResultadoDeComprobacion.noEvaluada(
                                        CONFIANZA, "La firma no es íntegra.")),
                        Optional.of(CERTIFICADO));

        assertThat(resultado.aceptada()).isFalse();
        assertThat(resultado.primerRechazo()).contains(INTEGRIDAD);
    }

    @Test
    void dadoElMismoDocumentoRechazado_cuandoSeArma_entoncesLasDemasQuedanNoEvaluadas() {
        ResultadoValidacion resultado =
                con(
                        ResultadoDeComprobacion.desde(
                                ResultadoDeComprobacion.rechazada(
                                        MISMO_DOCUMENTO, "No es la versión vigente.")),
                        Optional.empty());

        assertThat(resultado.comprobaciones())
                .extracting(
                        ResultadoDeComprobacion::comprobacion, ResultadoDeComprobacion::resultado)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(MISMO_DOCUMENTO, Resultado.RECHAZADA),
                        org.assertj.core.groups.Tuple.tuple(UNA_FIRMA_NUEVA, Resultado.NO_EVALUADA),
                        org.assertj.core.groups.Tuple.tuple(INTEGRIDAD, Resultado.NO_EVALUADA),
                        org.assertj.core.groups.Tuple.tuple(CONFIANZA, Resultado.NO_EVALUADA));
        assertThat(resultado.primerRechazo()).contains(MISMO_DOCUMENTO);
    }

    @Test
    void dadasMenosDeCuatroComprobaciones_cuandoSeCrea_entoncesSeRechaza() {
        assertThatThrownBy(() -> con(todasAprobadas().subList(0, 3), Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dadasLasComprobacionesFueraDeOrden_cuandoSeCrea_entoncesSeRechaza() {
        List<ResultadoDeComprobacion> fueraDeOrden =
                List.of(
                        ResultadoDeComprobacion.aprobada(UNA_FIRMA_NUEVA),
                        ResultadoDeComprobacion.aprobada(MISMO_DOCUMENTO),
                        ResultadoDeComprobacion.aprobada(INTEGRIDAD),
                        ResultadoDeComprobacion.aprobada(CONFIANZA));

        assertThatThrownBy(() -> con(fueraDeOrden, Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dadaUnaAprobadaDespuesDeUnaRechazada_cuandoSeCrea_entoncesSeRechaza() {
        List<ResultadoDeComprobacion> imposible =
                List.of(
                        ResultadoDeComprobacion.aprobada(MISMO_DOCUMENTO),
                        ResultadoDeComprobacion.rechazada(
                                UNA_FIRMA_NUEVA, "Tiene dos firmas nuevas."),
                        ResultadoDeComprobacion.aprobada(INTEGRIDAD),
                        ResultadoDeComprobacion.noEvaluada(
                                CONFIANZA, "No hay una sola firma nueva."));

        assertThatThrownBy(() -> con(imposible, Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dadaUnaNoEvaluadaSinRechazoAntes_cuandoSeCrea_entoncesSeRechaza() {
        List<ResultadoDeComprobacion> sinMotivo =
                List.of(
                        ResultadoDeComprobacion.aprobada(MISMO_DOCUMENTO),
                        ResultadoDeComprobacion.noEvaluada(UNA_FIRMA_NUEVA, "Sin motivo."),
                        ResultadoDeComprobacion.noEvaluada(INTEGRIDAD, "Sin motivo."),
                        ResultadoDeComprobacion.noEvaluada(CONFIANZA, "Sin motivo."));

        assertThatThrownBy(() -> con(sinMotivo, Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dadaUnaAceptadaSinCertificado_cuandoSeCrea_entoncesSeRechaza() {
        assertThatThrownBy(() -> con(todasAprobadas(), Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dadoUnMotivoDeMasDe200Caracteres_cuandoSeCrea_entoncesSeRechaza() {
        assertThatThrownBy(() -> ResultadoDeComprobacion.rechazada(INTEGRIDAD, "x".repeat(201)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dadoUnRechazoSinMotivo_cuandoSeCrea_entoncesSeRechaza() {
        assertThatThrownBy(() -> ResultadoDeComprobacion.rechazada(INTEGRIDAD, " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dadoUnResultadoRechazado_cuandoSeLanzaElRechazo_entoncesEsLaExcepcionDeSuPrimerRechazo() {
        ResultadoValidacion sinConfianza =
                con(
                        List.of(
                                ResultadoDeComprobacion.aprobada(MISMO_DOCUMENTO),
                                ResultadoDeComprobacion.aprobada(UNA_FIRMA_NUEVA),
                                ResultadoDeComprobacion.aprobada(INTEGRIDAD),
                                ResultadoDeComprobacion.rechazada(
                                        CONFIANZA, "El certificado no llega a una raíz.")),
                        Optional.of(CERTIFICADO));

        ArchivoFirmadoRechazado rechazo = ArchivoFirmadoRechazado.de(sinConfianza);

        assertThat(rechazo).isInstanceOf(ArchivoFirmadoRechazado.SinConfianza.class);
        assertThat(rechazo.resultado()).isSameAs(sinConfianza);
    }

    @Test
    void dadoCadaPrimerRechazo_cuandoSeLanzaElRechazo_entoncesTieneSuSubclase() {
        assertThat(
                        ArchivoFirmadoRechazado.de(
                                con(
                                        ResultadoDeComprobacion.desde(
                                                ResultadoDeComprobacion.rechazada(
                                                        MISMO_DOCUMENTO, "Otro documento.")),
                                        Optional.empty())))
                .isInstanceOf(ArchivoFirmadoRechazado.NoEsLaVersionVigente.class);
        assertThat(
                        ArchivoFirmadoRechazado.de(
                                con(
                                        ResultadoDeComprobacion.desde(
                                                ResultadoDeComprobacion.aprobada(MISMO_DOCUMENTO),
                                                ResultadoDeComprobacion.rechazada(
                                                        UNA_FIRMA_NUEVA, "Sin firma nueva.")),
                                        Optional.empty())))
                .isInstanceOf(ArchivoFirmadoRechazado.SinUnaFirmaNueva.class);
        assertThat(
                        ArchivoFirmadoRechazado.de(
                                con(
                                        ResultadoDeComprobacion.desde(
                                                ResultadoDeComprobacion.aprobada(MISMO_DOCUMENTO),
                                                ResultadoDeComprobacion.aprobada(UNA_FIRMA_NUEVA),
                                                ResultadoDeComprobacion.rechazada(
                                                        INTEGRIDAD, "No íntegra.")),
                                        Optional.empty())))
                .isInstanceOf(ArchivoFirmadoRechazado.NoIntegra.class);
    }

    @Test
    void dadoUnResultadoAceptado_cuandoSeLanzaElRechazo_entoncesSeRechaza() {
        ResultadoValidacion aceptado = con(todasAprobadas(), Optional.of(CERTIFICADO));

        assertThatThrownBy(() -> ArchivoFirmadoRechazado.de(aceptado))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
