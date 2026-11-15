package ec.mdd.firma.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FirmasDelDocumentoTest {

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
                    Instant.parse("2026-11-14T08:00:00Z"));

    private static final List<PasoDeFirma> CADENA =
            List.of(
                    new PasoDeFirma(3, "Aprobado", "RECTOR"),
                    new PasoDeFirma(1, "Elaborado", "RESPONSABLE_CONSOLIDACION"),
                    new PasoDeFirma(2, "Revisado", "VICERRECTOR"));

    private static final List<FirmanteEsperado> FIRMANTES =
            List.of(
                    FirmanteEsperado.porUsuario(1, "RESPONSABLE_CONSOLIDACION", "docente.demo"),
                    FirmanteEsperado.porIdentificacion(2, "VICERRECTOR", "0109990028"),
                    FirmanteEsperado.porIdentificacion(3, "RECTOR", "0109990010"));

    private final FirmasDelDocumento firmas =
            new FirmasDelDocumento(DOCUMENTO, CADENA, FIRMANTES, List.of());

    private static FirmaRegistrada firma(int orden, char hash) {
        return new FirmaRegistrada(
                orden,
                String.valueOf(hash).repeat(64),
                2048 + orden,
                "firmante" + orden,
                Instant.parse("2026-11-14T09:00:00Z").plusSeconds(orden));
    }

    private static FirmasDelDocumento conFirmas(FirmaRegistrada... registradas) {
        return new FirmasDelDocumento(DOCUMENTO, CADENA, FIRMANTES, List.of(registradas));
    }

    @Test
    void dadoUnDocumentoRechazado_cuandoSeBuscaElTurno_entoncesNadieEstaEnTurno() {
        RechazoDelFirmante rechazo =
                new RechazoDelFirmante(
                        2,
                        "firmante2",
                        Instant.parse("2026-11-15T10:00:00Z"),
                        "Falta la carga de Software.");
        FirmasDelDocumento rechazado =
                new FirmasDelDocumento(
                        DOCUMENTO, CADENA, FIRMANTES, List.of(firma(1, 'c')), Optional.of(rechazo));

        assertThat(rechazado.pasoEnTurno()).isEmpty();
        assertThat(rechazado.enTurno("firmante2", Optional.of("0109990028"))).isFalse();
        assertThat(rechazado.rechazo()).contains(rechazo);
        assertThat(rechazado.archivoVigente()).isEqualTo(firma(1, 'c').archivo());
    }

    @Test
    void dadoElConstructorSinRechazo_cuandoSeCrea_entoncesNoTieneRechazo() {
        assertThat(firmas.rechazo()).isEmpty();
    }

    // F12-H04, D-106: con las firmas anuladas, nadie está en turno, y las firmas se conservan.
    @Test
    void dadoUnDocumentoConLasFirmasAnuladas_cuandoSeBuscaElTurno_entoncesNadieEstaEnTurno() {
        AnulacionDeFirmas anulacion =
                new AnulacionDeFirmas(
                        "RECHAZO_EN_FIRMA", 1, "sistema", Instant.parse("2026-11-15T10:00:01Z"));
        FirmasDelDocumento anulado =
                new FirmasDelDocumento(
                        DOCUMENTO,
                        CADENA,
                        FIRMANTES,
                        List.of(firma(1, 'c')),
                        Optional.empty(),
                        Optional.of(anulacion));

        assertThat(anulado.pasoEnTurno()).isEmpty();
        assertThat(anulado.enTurno("firmante2", Optional.of("0109990028"))).isFalse();
        assertThat(anulado.anulacion()).contains(anulacion);
        assertThat(anulado.firmas()).containsExactly(firma(1, 'c'));
    }

    @Test
    void dadosLosConstructoresDeAntes_cuandoSeCrean_entoncesNoTienenAnulacion() {
        assertThat(firmas.anulacion()).isEmpty();
        assertThat(
                        new FirmasDelDocumento(
                                        DOCUMENTO, CADENA, FIRMANTES, List.of(), Optional.empty())
                                .anulacion())
                .isEmpty();
    }

    @Test
    void dadaUnaAnulacionInvalida_cuandoSeCrea_entoncesSeRechaza() {
        Instant momento = Instant.parse("2026-11-15T10:00:01Z");
        assertThatThrownBy(() -> new AnulacionDeFirmas("RECHAZO_EN_FIRMA", -1, "sistema", momento))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AnulacionDeFirmas(" ", 0, "sistema", momento))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dadoUnDocumentoSinFirmas_cuandoSeBuscaElTurno_entoncesEsElPrimerPaso() {
        assertThat(firmas.pasoEnTurno())
                .get()
                .satisfies(
                        turno -> {
                            assertThat(turno.paso().orden()).isEqualTo(1);
                            assertThat(turno.rotulo()).isEqualTo("Elaborado por");
                            assertThat(turno.firmante().rol())
                                    .isEqualTo("RESPONSABLE_CONSOLIDACION");
                        });
    }

    @Test
    void dadoElPrimerPasoFirmado_cuandoSeBuscaElTurno_entoncesEsElSegundo() {
        assertThat(conFirmas(firma(1, 'c')).pasoEnTurno())
                .get()
                .extracting(turno -> turno.paso().orden())
                .isEqualTo(2);
    }

    @Test
    void dadosTodosLosPasosFirmados_cuandoSeBuscaElTurno_entoncesNoHay() {
        assertThat(conFirmas(firma(1, 'c'), firma(2, 'd'), firma(3, 'e')).pasoEnTurno()).isEmpty();
    }

    @Test
    void dadoUnDocumentoSinFirmantesEsperados_cuandoSeBuscaElTurno_entoncesNoHay() {
        assertThat(new FirmasDelDocumento(DOCUMENTO, CADENA, List.of(), List.of()).pasoEnTurno())
                .isEmpty();
    }

    @Test
    void dadoElTurnoEnElPrimerPaso_cuandoPreguntanQuienesNoSonSuFirmante_entoncesNoEstanEnTurno() {
        assertThat(firmas.enTurno("docente.demo", Optional.empty())).isTrue();
        assertThat(firmas.enTurno("consolidacion.demo", Optional.empty())).isFalse();
        assertThat(firmas.enTurno("vicerrectorado.demo", Optional.of("0109990028"))).isFalse();
        assertThat(
                        conFirmas(firma(1, 'c'))
                                .enTurno("vicerrectorado.demo", Optional.of("0109990028")))
                .isTrue();
        assertThat(conFirmas(firma(1, 'c')).enTurno("docente.demo", Optional.empty())).isFalse();
    }

    @Test
    void dadoUnDocumentoSinFirmas_cuandoSePideElArchivoVigente_entoncesEsElOriginal() {
        assertThat(firmas.archivoVigente())
                .isEqualTo(
                        new ArchivoDelDocumento(DOCUMENTO.archivo(), DOCUMENTO.sha256Original()));
    }

    @Test
    void dadaLaPrimeraFirma_cuandoSePideElArchivoVigente_entoncesEsElDeEsaFirma() {
        assertThat(conFirmas(firma(1, 'c')).archivoVigente())
                .isEqualTo(
                        new ArchivoDelDocumento(
                                "documentos/" + "c".repeat(64) + ".pdf", "c".repeat(64)));
    }

    @Test
    void
            dadasDosFirmasEnCualquierOrden_cuandoSePideElArchivoVigente_entoncesEsElDeLaDeMayorOrden() {
        assertThat(conFirmas(firma(2, 'd'), firma(1, 'c')).archivoVigente().sha256())
                .isEqualTo("d".repeat(64));
        assertThat(conFirmas(firma(2, 'd'), firma(1, 'c')).pasoEnTurno())
                .get()
                .extracting(turno -> turno.paso().orden())
                .isEqualTo(3);
    }

    @Test
    void dadasLasTresFirmas_cuandoSePideElArchivoVigente_entoncesEsElDeLaTercera() {
        assertThat(conFirmas(firma(1, 'c'), firma(2, 'd'), firma(3, 'e')).archivoVigente().sha256())
                .isEqualTo("e".repeat(64));
    }

    @Test
    void dadasDosFirmasDelMismoPaso_cuandoSeCrea_entoncesSeRechaza() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> conFirmas(firma(1, 'c'), firma(1, 'd')))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dadaUnaFirmaDeUnPasoQueNoEstaEnLaCadena_cuandoSeCrea_entoncesSeRechaza() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> conFirmas(firma(4, 'c')))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
