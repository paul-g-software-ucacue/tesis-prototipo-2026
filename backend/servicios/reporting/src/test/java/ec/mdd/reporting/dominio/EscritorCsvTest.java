package ec.mdd.reporting.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** El CSV del reporte individual (F20-H01, RF-48, RNF-SEG-04, research §5). */
class EscritorCsvTest {

    private static final String HASH = "a".repeat(64);
    private static final Publicacion PUBLICACION =
            new Publicacion(
                    "2026-I",
                    1,
                    HASH,
                    Instant.parse("2026-11-18T15:00:00Z"),
                    "secretaria.demo",
                    false);
    private static final String ENCABEZADO =
            "\"Periodo\";\"Versión\";\"Hash de contenido\";\"Publicada el\";\"Identificación\";"
                    + "\"Docente\";\"Dedicación\";\"Tipo\";\"Carrera o unidad\";\"Código\";"
                    + "\"Asignatura o actividad\";\"Paralelo\";\"Componente\";\"Horas\"";
    private static final String COMUN =
            "\"2026-I\";1;\""
                    + HASH
                    + "\";\"2026-11-18 10:00\";\"0109990010\";\"PÉREZ ÑANDÚ ANA\";"
                    + "\"Tiempo completo\";";

    @Test
    void dadoUnReporte_cuandoSeEscribe_entoncesTieneElBomLasFilasYLosSeisTotales() {
        String csv = texto(EscritorCsv.escribir(reporte("Bases de datos", "Suelos andinos")));

        assertThat(csv).startsWith("﻿");
        List<String> lineas = Arrays.asList(csv.substring(1).split("\r\n", -1));
        assertThat(lineas)
                .containsExactly(
                        ENCABEZADO,
                        COMUN
                                + "\"ASIGNATURA\";\"Software\";\"BD-301\";\"Bases de datos\";\"A\";"
                                + "\"Clase\";8,00",
                        COMUN
                                + "\"ACTIVIDAD\";\"Suelos andinos\";\"141-a\";\"Proyecto\";\"\";"
                                + "\"Investigación\";2,50",
                        COMUN + "\"TOTAL\";\"\";\"\";\"\";\"\";\"Clase\";8,00",
                        COMUN
                                + "\"TOTAL\";\"\";\"\";\"\";\"\";\"Otras actividades de docencia\";0,00",
                        COMUN + "\"TOTAL\";\"\";\"\";\"\";\"\";\"Investigación\";2,50",
                        COMUN + "\"TOTAL\";\"\";\"\";\"\";\"\";\"Vinculación\";0,00",
                        COMUN + "\"TOTAL\";\"\";\"\";\"\";\"\";\"Gestión\";0,00",
                        COMUN + "\"TOTAL\";\"\";\"\";\"\";\"\";\"Total\";10,50",
                        "");
    }

    @ParameterizedTest
    @ValueSource(strings = {"=HYPERLINK(\"x\")", "+1", "-2", "@SUMA(A1)", "\tX", "\rX"})
    void dadoUnTextoQueEmpiezaComoFormula_cuandoSeEscribe_entoncesLlevaUnApostrofoDelante(
            String peligroso) {
        String csv = texto(EscritorCsv.escribir(reporte(peligroso, "Suelos")));

        assertThat(csv)
                .contains(";\"'" + peligroso.replace("\"", "\"\"") + "\";")
                .doesNotContain(";\"" + peligroso.replace("\"", "\"\"") + "\";");
    }

    @Test
    void dadasComillasDentroDelTexto_cuandoSeEscribe_entoncesSeDuplican() {
        String csv = texto(EscritorCsv.escribir(reporte("Taller \"A\"", "Suelos")));

        assertThat(csv).contains("\"Taller \"\"A\"\"\"");
    }

    @Test
    void dadoElMismoReporte_cuandoSeEscribeDosVecesPorOtraPersona_entoncesLosBytesSonIguales() {
        ReporteIndividual uno = reporte("Bases de datos", "Suelos");
        ReporteIndividual otro =
                new ReporteIndividual(
                        uno.publicacion(),
                        uno.docente(),
                        uno.asignaturas(),
                        uno.actividades(),
                        uno.horas(),
                        "secretaria.demo",
                        Instant.parse("2026-12-01T10:00:00Z"));

        assertThat(EscritorCsv.escribir(otro)).isEqualTo(EscritorCsv.escribir(uno));
        assertThat(texto(EscritorCsv.escribir(uno))).doesNotContain("rectorado.demo");
    }

    @Test
    void dadoUnDocenteSinDedicacion_cuandoSeEscribe_entoncesDiceSinDato() {
        ReporteIndividual reporte =
                new ReporteIndividual(
                        PUBLICACION,
                        new DatosDelDocente("0109990010", "PEREZ ANA", null, null),
                        List.of(),
                        List.of(),
                        HorasPorComponente.NINGUNA,
                        "rectorado.demo",
                        Instant.now());

        assertThat(texto(EscritorCsv.escribir(reporte)))
                .contains("\"PEREZ ANA\";\"sin dato\";\"TOTAL\"");
    }

    private static ReporteIndividual reporte(String asignatura, String unidad) {
        return ReporteIndividual.de(
                PUBLICACION,
                new ContenidoDelDocente(
                        HASH,
                        new DatosDelDocente("0109990010", "PÉREZ ÑANDÚ ANA", Dedicacion.TC, 40),
                        List.of(
                                new FilaDeAsignatura(
                                        "Software",
                                        "BD-301",
                                        asignatura,
                                        "A",
                                        new BigDecimal("8"))),
                        List.of(
                                new FilaDeActividad(
                                        Componente.INVESTIGACION,
                                        "141-a",
                                        "Proyecto",
                                        unidad,
                                        new BigDecimal("2.5"))),
                        new HorasPorComponente(
                                new BigDecimal("8"),
                                BigDecimal.ZERO,
                                new BigDecimal("2.5"),
                                BigDecimal.ZERO,
                                BigDecimal.ZERO)),
                "rectorado.demo",
                Instant.parse("2026-11-21T15:00:00Z"));
    }

    private static String texto(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
