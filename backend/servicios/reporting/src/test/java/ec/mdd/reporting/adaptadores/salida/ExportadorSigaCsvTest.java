package ec.mdd.reporting.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.reporting.dominio.ArchivoSiga;
import ec.mdd.reporting.dominio.CampoDelLayout;
import ec.mdd.reporting.dominio.CargasDeLaVersion;
import ec.mdd.reporting.dominio.CargasDePrueba;
import ec.mdd.reporting.dominio.ColumnaDelLayout;
import ec.mdd.reporting.dominio.Componente;
import ec.mdd.reporting.dominio.ContenidoDelDocente;
import ec.mdd.reporting.dominio.DatosDelDocente;
import ec.mdd.reporting.dominio.Dedicacion;
import ec.mdd.reporting.dominio.DocentesDeLaVersion;
import ec.mdd.reporting.dominio.DocentesDePrueba;
import ec.mdd.reporting.dominio.FilaDeActividad;
import ec.mdd.reporting.dominio.FilaDeAsignatura;
import ec.mdd.reporting.dominio.FilasParaSiga;
import ec.mdd.reporting.dominio.HorasPorComponente;
import ec.mdd.reporting.dominio.LayoutExportacion;
import ec.mdd.reporting.dominio.Publicacion;
import ec.mdd.reporting.dominio.ReporteGeneral;
import ec.mdd.reporting.dominio.TipoDeIdentificacion;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * El adaptador del prototipo del puerto «Exportación a SIGA»: el CSV con el layout y el formato de
 * research §4 (F19-H01, D-119), sin dobles.
 */
class ExportadorSigaCsvTest {

    private static final String HASH = "a".repeat(64);
    private static final Publicacion PUBLICACION =
            new Publicacion(
                    "2026-I",
                    1,
                    HASH,
                    Instant.parse("2026-11-20T03:30:00Z"),
                    "secretaria.demo",
                    false);

    private final ExportadorSigaCsv exportador = new ExportadorSigaCsv();

    @Test
    void dadasLasFilas_cuandoSeExporta_entoncesEsRfc4180ConComaCrlfYSinBom() {
        ArchivoSiga archivo = exportador.exportar(filas(docente("PEREZ ANA")), layout());

        byte[] bytes = archivo.contenido();
        assertThat(bytes[0]).isNotEqualTo((byte) 0xEF);
        assertThat(texto(archivo))
                .startsWith("NUMERO_IDENTIFICACION,COMPONENTE,HORAS_SEMANALES,ASIGNATURA\r\n")
                .endsWith("\r\n")
                .doesNotContain(";");
        assertThat(texto(archivo).split("\r\n", -1)).hasSize(4);
        assertThat(archivo.nombre()).isEqualTo("siga-2026-I-v1.csv");
    }

    @Test
    void dadoUnValorConComaComillasOSaltoDeLinea_cuandoSeExporta_entoncesVaEntreComillas() {
        ArchivoSiga archivo =
                exportador.exportar(
                        filas(docente("PEREZ, \"ANA\"")),
                        layout(
                                new ColumnaDelLayout("DOCENTE", CampoDelLayout.DOCENTE, Map.of()),
                                new ColumnaDelLayout(
                                        "ID", CampoDelLayout.IDENTIFICACION, Map.of())));

        assertThat(texto(archivo).split("\r\n")[1]).isEqualTo("\"PEREZ, \"\"ANA\"\"\",0109990010");
    }

    @Test
    void dadoOtroOrdenYSinUnaColumna_cuandoSeExporta_entoncesSaleConEseLayout() {
        ArchivoSiga archivo =
                exportador.exportar(
                        filas(docente("PEREZ ANA")),
                        layout(
                                new ColumnaDelLayout("HORAS", CampoDelLayout.HORAS, Map.of()),
                                new ColumnaDelLayout("ID", CampoDelLayout.IDENTIFICACION, Map.of()),
                                new ColumnaDelLayout(
                                        "COMP",
                                        CampoDelLayout.COMPONENTE,
                                        Map.of(
                                                "CLASE", "01",
                                                "OTRAS_DOCENCIA", "02",
                                                "INVESTIGACION", "03",
                                                "VINCULACION", "04",
                                                "GESTION", "05"))));

        assertThat(texto(archivo).split("\r\n"))
                .containsExactly("HORAS,ID,COMP", "8.00,0109990010,01", "2.50,0109990010,03");
    }

    @Test
    void dadasLasMismasFilasYElMismoLayout_cuandoSeExportaDosVeces_entoncesSonIdenticos() {
        FilasParaSiga filas = filas(docente("PEREZ ANA"));

        assertThat(exportador.exportar(filas, layout()).contenido())
                .isEqualTo(exportador.exportar(filas, layout()).contenido());
    }

    @Test
    void dadoElLayoutPorDefecto_cuandoSeExporta_entoncesSigueLasConvencionesDeSenescyt() {
        DatosDelDocente pena =
                new DatosDelDocente("0109990010", "Peña Álvarez José", null, null, null);

        String[] lineas = texto(exportador.exportar(filas(pena), porDefecto())).split("\r\n");

        assertThat(lineas[0])
                .isEqualTo(
                        "PERIODO,TIPO_IDENTIFICACION,NUMERO_IDENTIFICACION,APELLIDOS_NOMBRES,"
                                + "DEDICACION,COMPONENTE,CODIGO_ACTIVIDAD,CODIGO_CARRERA,"
                                + "CODIGO_ASIGNATURA,PARALELO,HORAS_SEMANALES,VERSION,"
                                + "FECHA_PUBLICACION");
        // Sin tipo de identificación ni dedicación: «NA» (Edge Cases). La cédula conserva su 0.
        // La fecha es la de Guayaquil: 03:30 UTC del 20 es el 19.
        assertThat(lineas[1])
                .isEqualTo(
                        "2026-I,NA,0109990010,PEÑA ÁLVAREZ JOSÉ,NA,CLASE,NA,TDS,BD-301,A,8.00,1,"
                                + "2026-11-19");
        // En una actividad, la asignatura y el paralelo no aplican; los códigos van en mayúsculas.
        assertThat(lineas[2])
                .isEqualTo(
                        "2026-I,NA,0109990010,PEÑA ÁLVAREZ JOSÉ,NA,INVESTIGACION,141-A,TDS,NA,NA,"
                                + "2.50,1,2026-11-19");
    }

    @Test
    void dadoUnDocenteConPasaporteYDedicacion_cuandoSeExporta_entoncesSalenSusCodigos() {
        DatosDelDocente conPasaporte =
                new DatosDelDocente(
                        "AB12345", "Smith John", Dedicacion.MT, 20, TipoDeIdentificacion.PASAPORTE);

        String linea =
                texto(exportador.exportar(filas(conPasaporte), porDefecto())).split("\r\n")[1];

        assertThat(linea).startsWith("2026-I,PASAPORTE,AB12345,SMITH JOHN,MT,CLASE,");
    }

    @Test
    void dadosDatosQueEmpiezanComoFormula_cuandoSeExporta_entoncesSeNeutralizan() {
        for (String peligroso : List.of("=1+1", "+57", "-3", "@SUMA", "\tA", "\rA")) {
            String csv =
                    texto(
                            exportador.exportar(
                                    filas(docente(peligroso)),
                                    layout(
                                            new ColumnaDelLayout(
                                                    "DOCENTE", CampoDelLayout.DOCENTE, Map.of()),
                                            new ColumnaDelLayout(
                                                    "ID",
                                                    CampoDelLayout.IDENTIFICACION,
                                                    Map.of()))));

            String celda = csv.substring(csv.indexOf("\r\n") + 2);
            assertThat(celda).as(peligroso).matches("(?s)\"?'.*");
        }
    }

    @Test
    void dadoElCsvDeLosDatosDePrueba_cuandoSeValidaConElEsquema_entoncesPasaElCienPorCiento() {
        List<DocentesDeLaVersion.Docente> docentes = DocentesDePrueba.todos();
        FilasParaSiga filas =
                FilasParaSiga.de(
                        PUBLICACION,
                        CargasDePrueba.de(HASH, docentes),
                        ReporteGeneral.de(
                                PUBLICACION,
                                new DocentesDeLaVersion(HASH, docentes),
                                "secretaria.demo",
                                Instant.parse("2026-11-22T15:00:00Z")));

        String[] lineas = texto(exportador.exportar(filas, porDefecto())).split("\r\n");

        // SC-003: un validador por columna del layout por defecto, sobre cada fila.
        List<String> esquema =
                List.of(
                        "^[A-Z0-9._-]{1,20}$",
                        "^(CEDULA|PASAPORTE|NA)$",
                        "^[0-9]{10}$|^[0-9A-Z]{1,13}$",
                        "^[^a-zà-ÿ]+$",
                        "^(TC|MT|TP|NA)$",
                        "^(CLASE|OTRAS_DOCENCIA|INVESTIGACION|VINCULACION|GESTION)$",
                        "^([0-9]{3}-[A-Z]|NA)$",
                        "^[A-Z0-9-]+$",
                        "^([A-Z0-9-]+|NA)$",
                        "^([A-Z0-9]+|NA)$",
                        "^[0-9]+\\.[0-9]{2}$",
                        "^[0-9]+$",
                        "^[0-9]{4}-[0-9]{2}-[0-9]{2}$");
        assertThat(lineas.length).isGreaterThan(docentes.size());
        for (int i = 1; i < lineas.length; i++) {
            String[] celdas = lineas[i].split(",", -1);
            assertThat(celdas).as("fila %d", i).hasSize(esquema.size());
            for (int j = 0; j < celdas.length; j++) {
                assertThat(celdas[j]).as("fila %d, columna %d", i, j + 1).matches(esquema.get(j));
            }
        }
    }

    private static LayoutExportacion porDefecto() {
        try (java.io.InputStream entrada =
                ExportadorSigaCsvTest.class.getResourceAsStream(
                        "/reporting/layout-siga-por-defecto.yaml")) {
            var lectura = new LectorDeLayoutYaml().leer(entrada.readAllBytes());
            return LayoutExportacion.porDefecto(
                    lectura.texto(), LayoutExportacion.validar(lectura.borrador()).columnas());
        } catch (java.io.IOException error) {
            throw new IllegalStateException(error);
        }
    }

    private static String texto(ArchivoSiga archivo) {
        return new String(archivo.contenido(), StandardCharsets.UTF_8);
    }

    private static LayoutExportacion layout() {
        return layout(
                new ColumnaDelLayout(
                        "NUMERO_IDENTIFICACION", CampoDelLayout.IDENTIFICACION, Map.of()),
                new ColumnaDelLayout("COMPONENTE", CampoDelLayout.COMPONENTE, Map.of()),
                new ColumnaDelLayout("HORAS_SEMANALES", CampoDelLayout.HORAS, Map.of()),
                new ColumnaDelLayout("ASIGNATURA", CampoDelLayout.ASIGNATURA, Map.of()));
    }

    private static LayoutExportacion layout(ColumnaDelLayout... columnas) {
        return LayoutExportacion.porDefecto("columnas: []", List.of(columnas));
    }

    static DatosDelDocente docente(String nombre) {
        return new DatosDelDocente(
                "0109990010", nombre, Dedicacion.TC, 40, TipoDeIdentificacion.CEDULA);
    }

    static FilasParaSiga filas(DatosDelDocente docente) {
        HorasPorComponente horas =
                new HorasPorComponente(
                        new BigDecimal("8"),
                        BigDecimal.ZERO,
                        new BigDecimal("2.5"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO);
        ContenidoDelDocente carga =
                new ContenidoDelDocente(
                        HASH,
                        docente,
                        List.of(
                                new FilaDeAsignatura(
                                        "Software",
                                        "BD-301",
                                        "Bases de datos",
                                        "A",
                                        new BigDecimal("8"),
                                        "TDS")),
                        List.of(
                                new FilaDeActividad(
                                        Componente.INVESTIGACION,
                                        "141-a",
                                        "Proyecto",
                                        "Suelos",
                                        new BigDecimal("2.5"),
                                        "TDS",
                                        "INV-01")),
                        horas);
        return FilasParaSiga.de(
                PUBLICACION,
                new CargasDeLaVersion(HASH, List.of(carga)),
                ReporteGeneral.de(
                        PUBLICACION,
                        new DocentesDeLaVersion(
                                HASH,
                                List.of(
                                        new DocentesDeLaVersion.Docente(
                                                docente.identificacion(),
                                                docente.nombre(),
                                                Dedicacion.TC,
                                                horas))),
                        "secretaria.demo",
                        Instant.parse("2026-11-22T15:00:00Z")));
    }
}
