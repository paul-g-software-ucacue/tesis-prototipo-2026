package ec.mdd.reporting.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.reporting.dominio.Componente;
import ec.mdd.reporting.dominio.ContenidoDelDocente;
import ec.mdd.reporting.dominio.DatosDelDocente;
import ec.mdd.reporting.dominio.Dedicacion;
import ec.mdd.reporting.dominio.FilaDeActividad;
import ec.mdd.reporting.dominio.FilaDeAsignatura;
import ec.mdd.reporting.dominio.HorasPorComponente;
import ec.mdd.reporting.dominio.Publicacion;
import ec.mdd.reporting.dominio.ReporteIndividual;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

/**
 * El PDF/A-2b del reporte individual (F20-H01, RF-48, aclaración del 2026-11-21): conforme según
 * veraPDF, sin formulario ni anotaciones, con el pie de la versión y su hash en cada página.
 */
class GeneradorDeReportePdfaTest {

    private static final String HASH = "0123456789abcdef".repeat(4);
    private static final Publicacion PUBLICACION =
            new Publicacion(
                    "2026-I",
                    1,
                    HASH,
                    Instant.parse("2026-11-18T15:00:00Z"),
                    "secretaria.demo",
                    false);

    private final GeneradorDeReportePdfa generador = new GeneradorDeReportePdfa();

    @Test
    void dadoUnReporte_cuandoSeGenera_entoncesEsPdfaSinFormularioNiAnotaciones() throws Exception {
        byte[] pdf = generador.generar(reporte(2, "=SUMA(A1) <b>&</b>"));

        assertThat(VerificadorPdfa.reglasIncumplidas(pdf)).isEmpty();
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            assertThat(documento.getDocumentCatalog().getAcroForm()).isNull();
            assertThat(documento.isEncrypted()).isFalse();
            for (PDPage pagina : documento.getPages()) {
                assertThat(pagina.getAnnotations()).isEmpty();
            }
            String texto = new PDFTextStripper().getText(documento);
            assertThat(texto)
                    .contains("Reporte individual de distributivo")
                    .contains("PÉREZ ÑANDÚ ANA")
                    .contains("Tiempo completo")
                    .contains("=SUMA(A1) <b>&</b>")
                    .contains("Suelos andinos")
                    .contains("8,00")
                    .contains("Total")
                    .contains("rectorado.demo");
        }
    }

    @Test
    void dadoUnDocenteConCuarentaFilas_cuandoSeGenera_entoncesCadaPaginaTieneElPieConLaVersion()
            throws Exception {
        byte[] pdf = generador.generar(reporte(40, "Asignatura " + "muy larga ".repeat(20)));

        assertThat(VerificadorPdfa.reglasIncumplidas(pdf)).isEmpty();
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            int paginas = documento.getNumberOfPages();
            assertThat(paginas).isGreaterThan(1);
            for (int pagina = 1; pagina <= paginas; pagina++) {
                PDFTextStripper lector = new PDFTextStripper();
                lector.setStartPage(pagina);
                lector.setEndPage(pagina);
                String texto = lector.getText(documento);
                assertThat(texto)
                        .as("página %d", pagina)
                        .contains("2026-I · versión 1 · hash " + HASH)
                        .contains("Página " + pagina + " de " + paginas);
            }
            PDFTextStripper segunda = new PDFTextStripper();
            segunda.setStartPage(2);
            segunda.setEndPage(2);
            assertThat(segunda.getText(documento))
                    .as("el encabezado de la tabla se repite")
                    .contains("Paralelo");
        }
    }

    @Test
    void dadoUnDocenteSinAsignaturasNiActividades_cuandoSeGenera_entoncesLoDiceYEsPdfa()
            throws Exception {
        ReporteIndividual vacio =
                new ReporteIndividual(
                        PUBLICACION,
                        new DatosDelDocente("0109990010", "PEREZ ANA", null, null),
                        List.of(),
                        List.of(),
                        HorasPorComponente.NINGUNA,
                        "rectorado.demo",
                        Instant.parse("2026-11-21T15:00:00Z"));

        byte[] pdf = generador.generar(vacio);

        assertThat(VerificadorPdfa.reglasIncumplidas(pdf)).isEmpty();
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            assertThat(new PDFTextStripper().getText(documento))
                    .contains("No tiene asignaturas en esta versión.")
                    .contains("No tiene actividades en esta versión.")
                    .contains("sin dato");
        }
    }

    private static ReporteIndividual reporte(int asignaturas, String nombre) {
        List<FilaDeAsignatura> filas = new ArrayList<>();
        for (int i = 0; i < asignaturas; i++) {
            filas.add(
                    new FilaDeAsignatura(
                            "Software", "BD-%03d".formatted(i), nombre, "A", new BigDecimal("8")));
        }
        BigDecimal clase = new BigDecimal(8 * asignaturas);
        return ReporteIndividual.de(
                PUBLICACION,
                new ContenidoDelDocente(
                        HASH,
                        new DatosDelDocente("0109990010", "PÉREZ ÑANDÚ ANA", Dedicacion.TC, 40),
                        filas,
                        List.of(
                                new FilaDeActividad(
                                        Componente.INVESTIGACION,
                                        "141-a",
                                        "Proyecto",
                                        "Suelos andinos",
                                        new BigDecimal("2.5"))),
                        new HorasPorComponente(
                                clase,
                                BigDecimal.ZERO,
                                new BigDecimal("2.5"),
                                BigDecimal.ZERO,
                                BigDecimal.ZERO)),
                "rectorado.demo",
                Instant.parse("2026-11-21T15:00:00Z"));
    }
}
