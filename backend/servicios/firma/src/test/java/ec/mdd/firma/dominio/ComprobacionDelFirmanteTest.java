package ec.mdd.firma.dominio;

import static ec.mdd.firma.dominio.Comprobacion.FIRMANTE;
import static ec.mdd.firma.dominio.Comprobacion.ORDEN;
import static ec.mdd.firma.dominio.Comprobacion.VIGENCIA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Las tres comprobaciones de F14-H03 (D-104): la vigencia al subir, el orden y el firmante por el
 * CN igual al nombre impreso del paso en turno.
 */
class ComprobacionDelFirmanteTest {

    private static final Instant SUBIDA = Instant.parse("2026-11-14T10:00:00Z");
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
                    SUBIDA.minusSeconds(3600));
    private static final List<PasoDeFirma> CADENA =
            List.of(
                    new PasoDeFirma(1, "Elaborado", "RESPONSABLE_CONSOLIDACION"),
                    new PasoDeFirma(2, "Revisado", "VICERRECTOR"),
                    new PasoDeFirma(3, "Aprobado", "RECTOR"));
    private static final List<FirmanteEsperado> FIRMANTES =
            List.of(
                    FirmanteEsperado.porUsuario(
                            1, "RESPONSABLE_CONSOLIDACION", "docente.demo", "Docente (demo)"),
                    FirmanteEsperado.porIdentificacion(
                            2, "VICERRECTOR", "0109990028", "CABRERA VAZQUEZ ANA GABRIELA"),
                    FirmanteEsperado.porIdentificacion(
                            3, "RECTOR", "0109990010", "Peralta Maldonado Juan Andrés"));

    private static FirmasDelDocumento conFirmas(int firmadas) {
        List<FirmaRegistrada> firmas =
                java.util.stream.IntStream.rangeClosed(1, firmadas)
                        .mapToObj(
                                orden ->
                                        new FirmaRegistrada(
                                                orden,
                                                String.valueOf((char) ('c' + orden)).repeat(64),
                                                2048,
                                                "alguien",
                                                SUBIDA.minusSeconds(60)))
                        .toList();
        return new FirmasDelDocumento(DOCUMENTO, CADENA, FIRMANTES, firmas);
    }

    private static CertificadoDeLaFirma certificado(String cn, Instant desde, Instant hasta) {
        return new CertificadoDeLaFirma(cn, "CN=CA de Prueba MDD Demo", "1a", desde, hasta);
    }

    private static CertificadoDeLaFirma vigente(String cn) {
        return certificado(cn, SUBIDA.minusSeconds(86_400), SUBIDA.plusSeconds(86_400));
    }

    private static List<org.assertj.core.groups.Tuple> resultados(
            List<ResultadoDeComprobacion> evaluadas) {
        return evaluadas.stream().map(una -> tuple(una.comprobacion(), una.resultado())).toList();
    }

    @Test
    void dadoElCnDelPasoEnTurnoYUnCertificadoVigente_cuandoSeEvalua_entoncesLasTresPasan() {
        assertThat(
                        resultados(
                                ComprobacionDelFirmante.evaluar(
                                        vigente("PERALTA MALDONADO JUAN ANDRES"),
                                        SUBIDA,
                                        conFirmas(2))))
                .containsExactly(
                        tuple(VIGENCIA, Resultado.APROBADA),
                        tuple(ORDEN, Resultado.APROBADA),
                        tuple(FIRMANTE, Resultado.APROBADA));
    }

    @Test
    void dadoElCnConOtrasMayusculasYTildes_cuandoSeEvalua_entoncesCoincide() {
        assertThat(
                        ComprobacionDelFirmante.evaluar(
                                        vigente("docente  (DEMO) "), SUBIDA, conFirmas(0))
                                .getLast()
                                .resultado())
                .isEqualTo(Resultado.APROBADA);
    }

    @Test
    void dadoUnCertificadoVencidoAlSubir_cuandoSeEvalua_entoncesNoEsVigenteYLoDemasNoSeEvalua() {
        CertificadoDeLaFirma vencido =
                certificado(
                        "Docente (demo)",
                        Instant.parse("2025-01-01T00:00:00Z"),
                        Instant.parse("2025-02-01T00:00:00Z"));

        List<ResultadoDeComprobacion> evaluadas =
                ComprobacionDelFirmante.evaluar(vencido, SUBIDA, conFirmas(0));

        assertThat(resultados(evaluadas)).containsExactly(tuple(VIGENCIA, Resultado.RECHAZADA));
        assertThat(evaluadas.getFirst().motivo()).isPresent();
    }

    @Test
    void dadoUnCertificadoTodaviaNoVigente_cuandoSeEvalua_entoncesNoEsVigente() {
        CertificadoDeLaFirma futuro =
                certificado("Docente (demo)", SUBIDA.plusSeconds(60), SUBIDA.plusSeconds(86_400));

        assertThat(resultados(ComprobacionDelFirmante.evaluar(futuro, SUBIDA, conFirmas(0))))
                .containsExactly(tuple(VIGENCIA, Resultado.RECHAZADA));
    }

    @Test
    void dadoElCnDeUnPasoPosterior_cuandoSeEvalua_entoncesEstaFueraDeOrden() {
        assertThat(
                        resultados(
                                ComprobacionDelFirmante.evaluar(
                                        vigente("Peralta Maldonado Juan Andres"),
                                        SUBIDA,
                                        conFirmas(1))))
                .containsExactly(
                        tuple(VIGENCIA, Resultado.APROBADA), tuple(ORDEN, Resultado.RECHAZADA));
    }

    @Test
    void dadoElCnDeUnPasoAnterior_cuandoSeEvalua_entoncesEsFirmanteEquivocado() {
        assertThat(
                        resultados(
                                ComprobacionDelFirmante.evaluar(
                                        vigente("Docente (demo)"), SUBIDA, conFirmas(1))))
                .containsExactly(
                        tuple(VIGENCIA, Resultado.APROBADA),
                        tuple(ORDEN, Resultado.APROBADA),
                        tuple(FIRMANTE, Resultado.RECHAZADA));
    }

    @Test
    void dadoOtroNombre_cuandoSeEvalua_entoncesEsFirmanteEquivocado() {
        assertThat(
                        ComprobacionDelFirmante.evaluar(
                                        vigente("Alguien Que No Firma"), SUBIDA, conFirmas(0))
                                .getLast())
                .satisfies(
                        una -> {
                            assertThat(una.comprobacion()).isEqualTo(FIRMANTE);
                            assertThat(una.resultado()).isEqualTo(Resultado.RECHAZADA);
                        });
    }

    @Test
    void dadoUnSujetoSinCn_cuandoSeEvalua_entoncesEsFirmanteEquivocado() {
        // Sin CN, el certificado trae el nombre completo del sujeto, que nunca es un nombre
        // impreso.
        assertThat(
                        ComprobacionDelFirmante.evaluar(
                                        vigente("O=MDD Demo,C=EC"), SUBIDA, conFirmas(0))
                                .getLast()
                                .resultado())
                .isEqualTo(Resultado.RECHAZADA);
    }

    @Test
    void dadaLaMismaPersonaEnLosTresPasos_cuandoFirmaElSegundo_entoncesNoEstaFueraDeOrden() {
        List<FirmanteEsperado> mismaPersona =
                List.of(
                        FirmanteEsperado.porUsuario(
                                1, "RESPONSABLE_CONSOLIDACION", "docente.demo", "Autor De Prueba"),
                        FirmanteEsperado.porIdentificacion(
                                2, "VICERRECTOR", "0109990028", "Autor De Prueba"),
                        FirmanteEsperado.porIdentificacion(
                                3, "RECTOR", "0109990010", "Autor De Prueba"));
        FirmasDelDocumento firmas =
                new FirmasDelDocumento(
                        DOCUMENTO,
                        CADENA,
                        mismaPersona,
                        List.of(
                                new FirmaRegistrada(
                                        1, "c".repeat(64), 2048, "docente.demo", SUBIDA)));

        assertThat(ComprobacionDelFirmante.evaluar(vigente("AUTOR DE PRUEBA"), SUBIDA, firmas))
                .extracting(ResultadoDeComprobacion::resultado)
                .containsOnly(Resultado.APROBADA)
                .hasSize(3);
    }
}
