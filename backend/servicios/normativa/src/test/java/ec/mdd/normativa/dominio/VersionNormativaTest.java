package ec.mdd.normativa.dominio;

import static ec.mdd.normativa.dominio.FilaCargaHorariaTest.fila;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** La versión de normativa y sus reglas (RF-19, MD-08), sin dobles. */
class VersionNormativaTest {

    private static final TablaCargaHoraria TABLA =
            new TablaCargaHoraria(List.of(fila(null, "DOCENTE_TC", 22, 18, 24, 18, 0, null)));
    private static final Regla RN_02 =
            new Regla(
                    "RN-02", Severidad.DURA, Map.of("horas_maximas_docente", new BigDecimal("40")));
    private static final Regla RN_01 =
            new Regla(
                    "RN-01",
                    null,
                    parametros(
                            "dedicacion_tiempo_completo", "40", "dedicacion_medio_tiempo", "20"));

    private static Map<String, BigDecimal> parametros(String... pares) {
        Map<String, BigDecimal> mapa = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) {
            mapa.put(pares[i], new BigDecimal(pares[i + 1]));
        }
        return mapa;
    }

    private static VersionNormativa version(int numero, List<Regla> reglas) {
        return new VersionNormativa(
                numero,
                "SENESCYT-2024-03",
                "Política SENESCYT 2024",
                LocalDate.of(2024, 3, 11),
                TipoIst.T2,
                TABLA,
                reglas);
    }

    @Test
    void dadoUnaVersion_cuandoSeReconstruye_entoncesTieneSusDatosYReglasPorIdentificador() {
        VersionNormativa version = version(1, List.of(RN_02, RN_01));

        assertThat(version.numero()).isEqualTo(1);
        assertThat(version.tipoIst()).isEqualTo(TipoIst.T2);
        assertThat(version.reglas())
                .extracting(Regla::identificador)
                .containsExactly("RN-01", "RN-02");
        assertThat(version.regla("RN-02")).contains(RN_02);
        assertThat(version.regla("RN-26")).isEmpty();
    }

    @Test
    void dadoUnaReglaSinSeveridad_cuandoSeLee_entoncesSoloTieneParametros() {
        assertThat(RN_01.severidad()).isNull();
        assertThat(RN_01.parametros())
                .containsExactly(
                        Map.entry("dedicacion_tiempo_completo", new BigDecimal("40")),
                        Map.entry("dedicacion_medio_tiempo", new BigDecimal("20")));
        assertThat(RN_01.parametro("dedicacion_medio_tiempo")).contains(new BigDecimal("20"));
    }

    @Test
    void dadoDatosInvalidos_cuandoSeReconstruyeLaVersion_entoncesSeRechaza() {
        assertThatThrownBy(() -> version(0, List.of(RN_01)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> version(1, List.of(RN_01, RN_01)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RN-01");
    }

    @Test
    void dadoReglasInvalidas_cuandoSeCrean_entoncesSeRechazan() {
        assertThatThrownBy(() -> new Regla("RN-1", null, Map.of("x", BigDecimal.ONE)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Regla("RN-05", Severidad.BLANDA, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
