package ec.mdd.firma.dominio;

import static ec.mdd.firma.dominio.Comprobacion.CONFIANZA;
import static ec.mdd.firma.dominio.Comprobacion.FIRMANTE;
import static ec.mdd.firma.dominio.Comprobacion.INTEGRIDAD;
import static ec.mdd.firma.dominio.Comprobacion.MISMO_DOCUMENTO;
import static ec.mdd.firma.dominio.Comprobacion.ORDEN;
import static ec.mdd.firma.dominio.Comprobacion.UNA_FIRMA_NUEVA;
import static ec.mdd.firma.dominio.Comprobacion.VIGENCIA;
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
                ResultadoDeComprobacion.aprobada(CONFIANZA),
                ResultadoDeComprobacion.aprobada(VIGENCIA),
                ResultadoDeComprobacion.aprobada(ORDEN),
                ResultadoDeComprobacion.aprobada(FIRMANTE));
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
                        ResultadoDeComprobacion.desde(
                                ResultadoDeComprobacion.aprobada(MISMO_DOCUMENTO),
                                ResultadoDeComprobacion.aprobada(UNA_FIRMA_NUEVA),
                                ResultadoDeComprobacion.rechazada(
                                        INTEGRIDAD, "La firma no corresponde al contenido.")),
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
                        org.assertj.core.groups.Tuple.tuple(CONFIANZA, Resultado.NO_EVALUADA),
                        org.assertj.core.groups.Tuple.tuple(VIGENCIA, Resultado.NO_EVALUADA),
                        org.assertj.core.groups.Tuple.tuple(ORDEN, Resultado.NO_EVALUADA),
                        org.assertj.core.groups.Tuple.tuple(FIRMANTE, Resultado.NO_EVALUADA));
        assertThat(resultado.primerRechazo()).contains(MISMO_DOCUMENTO);
    }

    @Test
    void dadasMenosDeCuatroComprobaciones_cuandoSeCrea_entoncesSeRechaza() {
        assertThatThrownBy(() -> con(todasAprobadas().subList(0, 6), Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dadasLasComprobacionesFueraDeOrden_cuandoSeCrea_entoncesSeRechaza() {
        List<ResultadoDeComprobacion> fueraDeOrden =
                List.of(
                        ResultadoDeComprobacion.aprobada(UNA_FIRMA_NUEVA),
                        ResultadoDeComprobacion.aprobada(MISMO_DOCUMENTO),
                        ResultadoDeComprobacion.aprobada(INTEGRIDAD),
                        ResultadoDeComprobacion.aprobada(CONFIANZA),
                        ResultadoDeComprobacion.aprobada(VIGENCIA),
                        ResultadoDeComprobacion.aprobada(ORDEN),
                        ResultadoDeComprobacion.aprobada(FIRMANTE));

        assertThatThrownBy(() -> con(fueraDeOrden, Optional.of(CERTIFICADO)))
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
                                CONFIANZA, "No hay una sola firma nueva."),
                        ResultadoDeComprobacion.noEvaluada(
                                VIGENCIA, "No hay una sola firma nueva."),
                        ResultadoDeComprobacion.noEvaluada(ORDEN, "No hay una sola firma nueva."),
                        ResultadoDeComprobacion.noEvaluada(
                                FIRMANTE, "No hay una sola firma nueva."));

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
                        ResultadoDeComprobacion.noEvaluada(CONFIANZA, "Sin motivo."),
                        ResultadoDeComprobacion.noEvaluada(VIGENCIA, "Sin motivo."),
                        ResultadoDeComprobacion.noEvaluada(ORDEN, "Sin motivo."),
                        ResultadoDeComprobacion.noEvaluada(FIRMANTE, "Sin motivo."));

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
                        ResultadoDeComprobacion.desde(
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

    @Test
    void dadaUnaAceptadaQueOtraSubidaGano_cuandoSeConvierte_entoncesEsUnRechazoPorNoSerLaVigente() {
        ResultadoValidacion aceptada = con(todasAprobadas(), Optional.of(CERTIFICADO));

        ResultadoValidacion rechazada = aceptada.ganadaPorOtraSubida();

        assertThat(rechazada.aceptada()).isFalse();
        assertThat(rechazada.primerRechazo()).contains(MISMO_DOCUMENTO);
        assertThat(rechazada.id()).isEqualTo(aceptada.id());
        assertThat(rechazada.sha256Subido()).isEqualTo(aceptada.sha256Subido());
        assertThat(rechazada.comprobaciones().getFirst().motivo()).isPresent();
        assertThat(rechazada.certificado()).isEqualTo(aceptada.certificado());
    }

    @Test
    void dadasLasComprobacionesDeF14H03_cuandoSeRechazan_entoncesCadaUnaTieneSuSubclase() {
        List<ResultadoDeComprobacion> previas =
                List.of(
                        ResultadoDeComprobacion.aprobada(MISMO_DOCUMENTO),
                        ResultadoDeComprobacion.aprobada(UNA_FIRMA_NUEVA),
                        ResultadoDeComprobacion.aprobada(INTEGRIDAD),
                        ResultadoDeComprobacion.aprobada(CONFIANZA));
        java.util.function.Function<List<ResultadoDeComprobacion>, ResultadoValidacion> armar =
                cola -> {
                    List<ResultadoDeComprobacion> todas = new java.util.ArrayList<>(previas);
                    todas.addAll(cola);
                    return con(
                            ResultadoDeComprobacion.desde(
                                    todas.toArray(ResultadoDeComprobacion[]::new)),
                            Optional.of(CERTIFICADO));
                };

        assertThat(
                        ArchivoFirmadoRechazado.de(
                                armar.apply(
                                        List.of(
                                                ResultadoDeComprobacion.rechazada(
                                                        VIGENCIA, "Vencido.")))))
                .isInstanceOf(ArchivoFirmadoRechazado.NoVigente.class);
        assertThat(
                        ArchivoFirmadoRechazado.de(
                                armar.apply(
                                        List.of(
                                                ResultadoDeComprobacion.aprobada(VIGENCIA),
                                                ResultadoDeComprobacion.rechazada(
                                                        ORDEN, "Fuera de orden.")))))
                .isInstanceOf(ArchivoFirmadoRechazado.FueraDeOrden.class);
        assertThat(
                        ArchivoFirmadoRechazado.de(
                                armar.apply(
                                        List.of(
                                                ResultadoDeComprobacion.aprobada(VIGENCIA),
                                                ResultadoDeComprobacion.aprobada(ORDEN),
                                                ResultadoDeComprobacion.rechazada(
                                                        FIRMANTE, "Otra persona.")))))
                .isInstanceOf(ArchivoFirmadoRechazado.FirmanteEquivocado.class);
    }
}
