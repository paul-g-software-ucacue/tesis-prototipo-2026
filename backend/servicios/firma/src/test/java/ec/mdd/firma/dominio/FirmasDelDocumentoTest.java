package ec.mdd.firma.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

    private final FirmasDelDocumento firmas = new FirmasDelDocumento(DOCUMENTO, CADENA, FIRMANTES);

    @Test
    void dadoUnDocumentoSinFirmas_cuandoSeBuscaElTurno_entoncesEsElPrimerPaso() {
        assertThat(firmas.pasoEnTurno(Set.of()))
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
        assertThat(firmas.pasoEnTurno(Set.of(1)))
                .get()
                .extracting(turno -> turno.paso().orden())
                .isEqualTo(2);
    }

    @Test
    void dadosTodosLosPasosFirmados_cuandoSeBuscaElTurno_entoncesNoHay() {
        assertThat(firmas.pasoEnTurno(Set.of(1, 2, 3))).isEmpty();
    }

    @Test
    void dadoUnDocumentoSinFirmantesEsperados_cuandoSeBuscaElTurno_entoncesNoHay() {
        assertThat(new FirmasDelDocumento(DOCUMENTO, CADENA, List.of()).pasoEnTurno(Set.of()))
                .isEmpty();
    }

    @Test
    void dadoElTurnoEnElPrimerPaso_cuandoPreguntanQuienesNoSonSuFirmante_entoncesNoEstanEnTurno() {
        assertThat(firmas.enTurno("docente.demo", Optional.empty(), Set.of())).isTrue();
        assertThat(firmas.enTurno("consolidacion.demo", Optional.empty(), Set.of())).isFalse();
        assertThat(firmas.enTurno("vicerrectorado.demo", Optional.of("0109990028"), Set.of()))
                .isFalse();
        assertThat(firmas.enTurno("vicerrectorado.demo", Optional.of("0109990028"), Set.of(1)))
                .isTrue();
    }

    @Test
    void dadoUnDocumentoSinFirmas_cuandoSePideElArchivoVigente_entoncesEsElOriginal() {
        assertThat(firmas.archivoVigente())
                .isEqualTo(
                        new ArchivoDelDocumento(DOCUMENTO.archivo(), DOCUMENTO.sha256Original()));
    }
}
