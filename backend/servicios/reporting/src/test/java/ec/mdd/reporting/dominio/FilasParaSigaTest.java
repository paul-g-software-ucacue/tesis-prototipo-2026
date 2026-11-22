package ec.mdd.reporting.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Las filas del CSV para SIGA y su comprobación contra el reporte general (F19-H01, RF-52, research
 * §3 y §5, D-119), sin dobles.
 */
class FilasParaSigaTest {

    private static final String HASH = "a".repeat(64);
    private static final Instant AHORA = Instant.parse("2026-11-22T15:00:00Z");
    private static final Publicacion PUBLICACION =
            new Publicacion(
                    "2026-I",
                    1,
                    HASH,
                    Instant.parse("2026-11-20T03:30:00Z"),
                    "secretaria.demo",
                    false);

    private static final DatosDelDocente ANA =
            new DatosDelDocente(
                    "0109990010", "PÉREZ ANA", Dedicacion.TC, 40, TipoDeIdentificacion.CEDULA);
    private static final DatosDelDocente LUIS =
            new DatosDelDocente("0109990020", "ÁLVAREZ LUIS", null, null, null);

    @Test
    void dadoUnDocenteConDosParalelosYUnaInvestigacion_cuandoSeArma_entoncesTieneTresFilas() {
        ContenidoDelDocente ana =
                new ContenidoDelDocente(
                        HASH,
                        ANA,
                        List.of(asignatura("BD-301", "B", "4"), asignatura("BD-301", "A", "4")),
                        List.of(actividad(Componente.INVESTIGACION, "141-a", "2.50", "INV-01")),
                        horas("8", "0", "2.50", "0", "0"));

        FilasParaSiga filas = armar(ana);

        assertThat(filas.filas()).hasSize(3);
        FilaSiga primera = filas.filas().get(0);
        assertThat(primera.componente()).isEqualTo(Componente.CLASE);
        assertThat(primera.asignatura()).contains("BD-301");
        assertThat(primera.paralelo()).contains("A");
        assertThat(primera.actividad()).isEmpty();
        assertThat(primera.carrera()).isEqualTo("TDS");
        assertThat(filas.filas().get(1).paralelo()).contains("B");
        FilaSiga investigacion = filas.filas().get(2);
        assertThat(investigacion.componente()).isEqualTo(Componente.INVESTIGACION);
        assertThat(investigacion.actividad()).contains("141-a");
        assertThat(investigacion.asignatura()).isEmpty();
        assertThat(investigacion.paralelo()).isEmpty();
        assertThat(investigacion.proyecto()).contains("INV-01");
        assertThat(investigacion.horas()).isEqualByComparingTo("2.50");
    }

    @Test
    void dadosVariosDocentes_cuandoSeArma_entoncesOrdenaPorDocenteComponenteYCodigo() {
        ContenidoDelDocente ana =
                new ContenidoDelDocente(
                        HASH,
                        ANA,
                        List.of(asignatura("SOC-101", "A", "3"), asignatura("BD-301", "A", "4")),
                        List.of(
                                actividad(Componente.GESTION, "143-a", "1", null),
                                actividad(Componente.OTRAS_DOCENCIA, "140-g", "1", null),
                                actividad(Componente.OTRAS_DOCENCIA, "140-b", "1", null)),
                        horas("7", "2", "0", "0", "1"));
        ContenidoDelDocente luis =
                new ContenidoDelDocente(
                        HASH,
                        LUIS,
                        List.of(),
                        List.of(actividad(Componente.GESTION, "143-a", "3", null)),
                        horas("0", "0", "0", "0", "3"));

        FilasParaSiga filas = armar(ana, luis);

        // «ÁLVAREZ» va antes que «PÉREZ» en el orden alfabético del español.
        assertThat(filas.filas())
                .extracting(
                        una -> una.docente().nombre(),
                        FilaSiga::componente,
                        una -> una.asignatura().or(una::actividad).orElseThrow())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                "ÁLVAREZ LUIS", Componente.GESTION, "143-a"),
                        org.assertj.core.groups.Tuple.tuple(
                                "PÉREZ ANA", Componente.CLASE, "BD-301"),
                        org.assertj.core.groups.Tuple.tuple(
                                "PÉREZ ANA", Componente.CLASE, "SOC-101"),
                        org.assertj.core.groups.Tuple.tuple(
                                "PÉREZ ANA", Componente.OTRAS_DOCENCIA, "140-b"),
                        org.assertj.core.groups.Tuple.tuple(
                                "PÉREZ ANA", Componente.OTRAS_DOCENCIA, "140-g"),
                        org.assertj.core.groups.Tuple.tuple(
                                "PÉREZ ANA", Componente.GESTION, "143-a"));
    }

    @Test
    void dadasDosAsignacionesDeLaMismaActividad_cuandoSeArma_entoncesSonDosFilasQueSuman() {
        ContenidoDelDocente ana =
                new ContenidoDelDocente(
                        HASH,
                        ANA,
                        List.of(),
                        List.of(
                                actividad(Componente.INVESTIGACION, "141-a", "2.25", "INV-02"),
                                actividad(Componente.INVESTIGACION, "141-a", "1.50", "INV-01")),
                        horas("0", "0", "3.75", "0", "0"));

        FilasParaSiga filas = armar(ana);

        assertThat(filas.filas())
                .extracting(una -> una.proyecto().orElseThrow())
                .containsExactly("INV-01", "INV-02");
    }

    @Test
    void dadoUnDocenteConTotalesDistintosDelReporteGeneral_cuandoSeArma_entoncesEsInconsistente() {
        ContenidoDelDocente ana =
                new ContenidoDelDocente(
                        HASH,
                        ANA,
                        List.of(asignatura("BD-301", "A", "8")),
                        List.of(),
                        horas("8", "0", "0", "0", "0"));
        ReporteGeneral conOtrasHoras =
                general(
                        new DocentesDeLaVersion.Docente(
                                ANA.identificacion(),
                                ANA.nombre(),
                                Dedicacion.TC,
                                horas("7.99", "0", "0", "0", "0")));

        assertThatThrownBy(
                        () ->
                                FilasParaSiga.de(
                                        PUBLICACION,
                                        new CargasDeLaVersion(HASH, List.of(ana)),
                                        conOtrasHoras))
                .isInstanceOf(ContenidoInconsistente.class)
                .hasMessageContaining("0109990010")
                .hasMessageContaining("CLASE");
    }

    @Test
    void dadoUnDocenteQueFaltaOSobra_cuandoSeArma_entoncesEsInconsistente() {
        ContenidoDelDocente ana = soloClase(ANA, "8");
        ContenidoDelDocente luis = soloClase(LUIS, "4");

        assertThatThrownBy(
                        () ->
                                FilasParaSiga.de(
                                        PUBLICACION,
                                        new CargasDeLaVersion(HASH, List.of(ana)),
                                        general(docente(ana), docente(luis))))
                .isInstanceOf(ContenidoInconsistente.class);
        assertThatThrownBy(
                        () ->
                                FilasParaSiga.de(
                                        PUBLICACION,
                                        new CargasDeLaVersion(HASH, List.of(ana, luis)),
                                        general(docente(ana))))
                .isInstanceOf(ContenidoInconsistente.class);
    }

    @Test
    void dadoUnHashDistinto_oUnDocenteRepetido_cuandoSeArma_entoncesEsInconsistente() {
        ContenidoDelDocente ana = soloClase(ANA, "8");
        ContenidoDelDocente deOtraVersion =
                new ContenidoDelDocente(
                        "b".repeat(64), ANA, ana.asignaturas(), List.of(), ana.horas());

        assertThatThrownBy(
                        () ->
                                FilasParaSiga.de(
                                        PUBLICACION,
                                        new CargasDeLaVersion("b".repeat(64), List.of()),
                                        general()))
                .isInstanceOf(ContenidoInconsistente.class);
        assertThatThrownBy(
                        () ->
                                FilasParaSiga.de(
                                        PUBLICACION,
                                        new CargasDeLaVersion(HASH, List.of(deOtraVersion)),
                                        general(docente(ana))))
                .isInstanceOf(ContenidoInconsistente.class);
        assertThatThrownBy(
                        () ->
                                FilasParaSiga.de(
                                        PUBLICACION,
                                        new CargasDeLaVersion(HASH, List.of(ana, ana)),
                                        general(docente(ana))))
                .isInstanceOf(ContenidoInconsistente.class);
    }

    @Test
    void dadosLosDatosDePrueba_cuandoSeArma_entoncesCuadraConElGeneralSinRedondear() {
        List<DocentesDeLaVersion.Docente> docentes = DocentesDePrueba.todos();
        CargasDeLaVersion cargas = CargasDePrueba.de(HASH, docentes);

        FilasParaSiga filas =
                FilasParaSiga.de(
                        PUBLICACION,
                        cargas,
                        ReporteGeneral.de(
                                PUBLICACION,
                                new DocentesDeLaVersion(HASH, docentes),
                                "secretaria.demo",
                                AHORA));

        BigDecimal suma =
                filas.filas().stream()
                        .map(FilaSiga::horas)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal esperada =
                docentes.stream()
                        .map(una -> una.horas().total())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(suma).isEqualByComparingTo(esperada);
    }

    @Test
    void cadaColumna_daElValorTipadoDeSuCampo() {
        FilaSiga clase =
                armar(
                                new ContenidoDelDocente(
                                        HASH,
                                        ANA,
                                        List.of(asignatura("BD-301", "A", "8")),
                                        List.of(),
                                        horas("8", "0", "0", "0", "0")))
                        .filas()
                        .get(0);

        assertThat(valor(CampoDelLayout.PERIODO, clase))
                .isEqualTo(new ValorDeColumna.Texto("2026-I"));
        assertThat(valor(CampoDelLayout.VERSION, clase)).isEqualTo(new ValorDeColumna.Entero(1));
        // 03:30 UTC del 20 es el 19 en Guayaquil.
        assertThat(valor(CampoDelLayout.FECHA_PUBLICACION, clase))
                .isEqualTo(new ValorDeColumna.Fecha(LocalDate.of(2026, 11, 19)));
        assertThat(valor(CampoDelLayout.HASH_VERSION, clase))
                .isEqualTo(new ValorDeColumna.Texto(HASH));
        assertThat(valor(CampoDelLayout.TIPO_IDENTIFICACION, clase))
                .isEqualTo(new ValorDeColumna.Texto("CEDULA"));
        assertThat(valor(CampoDelLayout.IDENTIFICACION, clase))
                .isEqualTo(new ValorDeColumna.Texto("0109990010"));
        assertThat(valor(CampoDelLayout.DOCENTE, clase))
                .isEqualTo(new ValorDeColumna.Texto("PÉREZ ANA"));
        assertThat(valor(CampoDelLayout.DEDICACION, clase))
                .isEqualTo(new ValorDeColumna.Texto("TC"));
        assertThat(valor(CampoDelLayout.COMPONENTE, clase))
                .isEqualTo(new ValorDeColumna.Texto("CLASE"));
        assertThat(valor(CampoDelLayout.ACTIVIDAD, clase)).isEqualTo(ValorDeColumna.NO_APLICA);
        assertThat(valor(CampoDelLayout.CARRERA, clase)).isEqualTo(new ValorDeColumna.Texto("TDS"));
        assertThat(valor(CampoDelLayout.ASIGNATURA, clase))
                .isEqualTo(new ValorDeColumna.Texto("BD-301"));
        assertThat(valor(CampoDelLayout.PARALELO, clase)).isEqualTo(new ValorDeColumna.Texto("A"));
        assertThat(valor(CampoDelLayout.PROYECTO, clase)).isEqualTo(ValorDeColumna.NO_APLICA);
        assertThat(valor(CampoDelLayout.HORAS, clase))
                .isEqualTo(new ValorDeColumna.Horas(new BigDecimal("8")));

        FilaSiga sinDatos = armar(soloClase(LUIS, "4")).filas().get(0);
        assertThat(valor(CampoDelLayout.TIPO_IDENTIFICACION, sinDatos))
                .isEqualTo(ValorDeColumna.NO_APLICA);
        assertThat(valor(CampoDelLayout.DEDICACION, sinDatos)).isEqualTo(ValorDeColumna.NO_APLICA);
    }

    @Test
    void unaColumnaTraducida_daElValorDeSiga() {
        FilaSiga clase = armar(soloClase(ANA, "8")).filas().get(0);
        ColumnaDelLayout componente =
                new ColumnaDelLayout(
                        "COMPONENTE",
                        CampoDelLayout.COMPONENTE,
                        Map.of(
                                "CLASE", "01",
                                "OTRAS_DOCENCIA", "02",
                                "INVESTIGACION", "03",
                                "VINCULACION", "04",
                                "GESTION", "05"));

        assertThat(componente.valor(clase)).isEqualTo(new ValorDeColumna.Texto("01"));
    }

    private static ValorDeColumna valor(CampoDelLayout campo, FilaSiga fila) {
        return new ColumnaDelLayout("X", campo, Map.of()).valor(fila);
    }

    private static FilasParaSiga armar(ContenidoDelDocente... cargas) {
        List<DocentesDeLaVersion.Docente> docentes = new ArrayList<>();
        for (ContenidoDelDocente una : cargas) {
            docentes.add(docente(una));
        }
        return FilasParaSiga.de(
                PUBLICACION,
                new CargasDeLaVersion(HASH, List.of(cargas)),
                general(docentes.toArray(DocentesDeLaVersion.Docente[]::new)));
    }

    private static ReporteGeneral general(DocentesDeLaVersion.Docente... docentes) {
        return ReporteGeneral.de(
                PUBLICACION,
                new DocentesDeLaVersion(HASH, List.of(docentes)),
                "secretaria.demo",
                AHORA);
    }

    private static DocentesDeLaVersion.Docente docente(ContenidoDelDocente carga) {
        return new DocentesDeLaVersion.Docente(
                carga.docente().identificacion(),
                carga.docente().nombre(),
                carga.docente().dedicacion().orElse(null),
                carga.horas());
    }

    private static ContenidoDelDocente soloClase(DatosDelDocente docente, String horas) {
        return new ContenidoDelDocente(
                HASH,
                docente,
                List.of(asignatura("BD-301", "A", horas)),
                List.of(),
                horas(horas, "0", "0", "0", "0"));
    }

    private static FilaDeAsignatura asignatura(String codigo, String paralelo, String horas) {
        return new FilaDeAsignatura(
                "Software", codigo, "Asignatura", paralelo, new BigDecimal(horas), "TDS");
    }

    private static FilaDeActividad actividad(
            Componente componente, String codigo, String horas, String proyecto) {
        return new FilaDeActividad(
                componente, codigo, "Actividad", "TDS", new BigDecimal(horas), "TDS", proyecto);
    }

    private static HorasPorComponente horas(
            String clase, String otras, String investigacion, String vinculacion, String gestion) {
        return new HorasPorComponente(
                new BigDecimal(clase),
                new BigDecimal(otras),
                new BigDecimal(investigacion),
                new BigDecimal(vinculacion),
                new BigDecimal(gestion));
    }
}
