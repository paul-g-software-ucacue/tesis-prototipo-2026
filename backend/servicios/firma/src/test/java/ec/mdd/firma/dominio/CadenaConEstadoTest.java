package ec.mdd.firma.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.firma.dominio.PasoConEstado.Estado;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * La cadena de firmas con el estado de cada paso y el paso que sigue a una firma (F16-H02, D-110):
 * el paso en turno es siempre el de {@code pasoEnTurno()}.
 */
class CadenaConEstadoTest {

    private static final DocumentoFirmable DOCUMENTO =
            new DocumentoFirmable(
                    UUID.randomUUID(),
                    "2026-I",
                    1,
                    DocumentoFirmable.CONSOLIDADO,
                    "a".repeat(64),
                    "b".repeat(64),
                    "documentos/" + "b".repeat(64) + ".pdf",
                    1024,
                    "sistema",
                    Instant.parse("2026-11-18T08:00:00Z"));

    private static final List<PasoDeFirma> CADENA =
            List.of(
                    new PasoDeFirma(3, "Aprobado", "RECTOR"),
                    new PasoDeFirma(1, "Elaborado", "RESPONSABLE_CONSOLIDACION"),
                    new PasoDeFirma(2, "Revisado", "VICERRECTOR"));

    private static final List<FirmanteEsperado> FIRMANTES =
            List.of(
                    FirmanteEsperado.porUsuario(
                            1, "RESPONSABLE_CONSOLIDACION", "docente.demo", "Docente Uno"),
                    FirmanteEsperado.porIdentificacion(
                            2, "VICERRECTOR", "0109990028", "Vicerrector Dos"),
                    FirmanteEsperado.porIdentificacion(3, "RECTOR", "0109990010", "Rector Tres"));

    private static FirmaRegistrada firma(int orden) {
        return new FirmaRegistrada(
                orden,
                String.valueOf((char) ('c' + orden)).repeat(64),
                2048,
                "firmante" + orden,
                Instant.parse("2026-11-18T09:00:00Z").plusSeconds(orden));
    }

    private static FirmasDelDocumento con(List<FirmaRegistrada> firmas) {
        return new FirmasDelDocumento(DOCUMENTO, CADENA, FIRMANTES, firmas);
    }

    @Test
    void dadoUnDocumentoRecienGenerado_cuandoSePideLaCadena_entoncesF1EnTurnoYLasDemasPendientes() {
        FirmasDelDocumento firmas = con(List.of());

        assertThat(firmas.cadenaConEstado())
                .containsExactly(
                        new PasoConEstado(
                                1,
                                "Elaborado por",
                                Optional.of("Docente Uno"),
                                Estado.EN_TURNO,
                                Optional.empty()),
                        new PasoConEstado(
                                2,
                                "Revisado por",
                                Optional.of("Vicerrector Dos"),
                                Estado.PENDIENTE,
                                Optional.empty()),
                        new PasoConEstado(
                                3,
                                "Aprobado por",
                                Optional.of("Rector Tres"),
                                Estado.PENDIENTE,
                                Optional.empty()));
        assertThat(firmas.pasoEnTurno()).map(turno -> turno.paso().orden()).contains(1);
    }

    @Test
    void dadaF1Firmada_cuandoSePideLaCadena_entoncesF1ConSuFechaYF2EnTurno() {
        FirmasDelDocumento firmas = con(List.of(firma(1)));

        assertThat(firmas.cadenaConEstado())
                .extracting(PasoConEstado::estado)
                .containsExactly(Estado.FIRMADO, Estado.EN_TURNO, Estado.PENDIENTE);
        assertThat(firmas.cadenaConEstado().getFirst().firmadoEn()).contains(firma(1).subidoEn());
        assertThat(firmas.pasoEnTurno()).map(turno -> turno.paso().orden()).contains(2);
    }

    @Test
    void dadaLaCadenaCompleta_cuandoSePideLaCadena_entoncesTodasFirmadasYNingunaEnTurno() {
        FirmasDelDocumento firmas = con(List.of(firma(1), firma(2), firma(3)));

        assertThat(firmas.cadenaConEstado())
                .extracting(PasoConEstado::estado)
                .containsExactly(Estado.FIRMADO, Estado.FIRMADO, Estado.FIRMADO);
        assertThat(firmas.pasoEnTurno()).isEmpty();
    }

    @Test
    void dadoUnRechazoEnF2_cuandoSePideLaCadena_entoncesF2RechazadaYNingunaEnTurno() {
        RechazoDelFirmante rechazo =
                new RechazoDelFirmante(
                        2,
                        "vicerrectorado.demo",
                        Instant.parse("2026-11-18T10:00:00Z"),
                        "Falta la carga de Software.");
        FirmasDelDocumento firmas =
                new FirmasDelDocumento(
                        DOCUMENTO, CADENA, FIRMANTES, List.of(firma(1)), Optional.of(rechazo));

        assertThat(firmas.cadenaConEstado())
                .extracting(PasoConEstado::estado)
                .containsExactly(Estado.FIRMADO, Estado.RECHAZADO, Estado.PENDIENTE);
        assertThat(firmas.pasoEnTurno()).isEmpty();
    }

    @Test
    void dadasLasFirmasAnuladas_cuandoSePideLaCadena_entoncesNingunaEnTurno() {
        FirmasDelDocumento firmas =
                new FirmasDelDocumento(
                        DOCUMENTO,
                        CADENA,
                        FIRMANTES,
                        List.of(firma(1)),
                        Optional.empty(),
                        Optional.of(
                                new AnulacionDeFirmas(
                                        "RECHAZO_EN_FIRMA",
                                        1,
                                        "sistema",
                                        Instant.parse("2026-11-18T10:00:01Z"))));

        assertThat(firmas.cadenaConEstado())
                .extracting(PasoConEstado::estado)
                .doesNotContain(Estado.EN_TURNO);
        assertThat(firmas.pasoEnTurno()).isEmpty();
    }

    @Test
    void dadoUnDocumentoSinFirmantesEsperados_cuandoSePideLaCadena_entoncesSinTurnoNiNombres() {
        FirmasDelDocumento firmas = new FirmasDelDocumento(DOCUMENTO, CADENA, List.of(), List.of());

        assertThat(firmas.cadenaConEstado())
                .allSatisfy(
                        paso -> {
                            assertThat(paso.estado()).isEqualTo(Estado.PENDIENTE);
                            assertThat(paso.firmante()).isEmpty();
                        });
        assertThat(firmas.pasoEnTurno()).isEmpty();
    }

    @Test
    void dadoCadaPasoFirmado_cuandoSePideElSiguiente_entoncesF2TrasF1F3TrasF2YNadaTrasF3() {
        FirmasDelDocumento firmas = con(List.of());

        assertThat(firmas.siguienteTrasFirmar(1))
                .hasValueSatisfying(
                        siguiente -> {
                            assertThat(siguiente.paso().orden()).isEqualTo(2);
                            assertThat(siguiente.rotulo()).isEqualTo("Revisado por");
                            assertThat(siguiente.firmante().identificacion())
                                    .isEqualTo("0109990028");
                        });
        assertThat(firmas.siguienteTrasFirmar(2)).map(uno -> uno.paso().orden()).contains(3);
        assertThat(firmas.siguienteTrasFirmar(3)).isEmpty();
    }

    @Test
    void dadoUnSiguienteSinFirmanteEsperado_cuandoSePide_entoncesVacio() {
        FirmasDelDocumento firmas = new FirmasDelDocumento(DOCUMENTO, CADENA, List.of(), List.of());

        assertThat(firmas.siguienteTrasFirmar(1)).isEmpty();
    }

    // F19-H02 (D-120): la ruta interna informa la anulación aparte; la cadena no la oculta.
    @Test
    void dadasFirmasAnuladas_cuandoSePideLaCadena_entoncesConservaLosPasosYLaAnulacionVaAparte() {
        FirmasDelDocumento firmas =
                new FirmasDelDocumento(
                        DOCUMENTO,
                        CADENA,
                        FIRMANTES,
                        List.of(firma(1), firma(2), firma(3)),
                        Optional.empty(),
                        Optional.of(
                                new AnulacionDeFirmas(
                                        "Reforma del distributivo",
                                        3,
                                        "sistema",
                                        Instant.parse("2026-11-19T08:00:00Z"))));

        assertThat(firmas.cadenaConEstado())
                .extracting(PasoConEstado::estado)
                .containsExactly(Estado.FIRMADO, Estado.FIRMADO, Estado.FIRMADO);
        assertThat(firmas.anulacion()).isPresent();
    }
}
