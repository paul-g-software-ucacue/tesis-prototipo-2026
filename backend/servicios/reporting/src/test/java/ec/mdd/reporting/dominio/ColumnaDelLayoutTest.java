package ec.mdd.reporting.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Las invariantes de una columna del layout de exportación (D-119). */
class ColumnaDelLayoutTest {

    @Test
    void dadoUnCampoNoTraducible_cuandoLlevaTraduccion_entoncesSeRechaza() {
        assertThatThrownBy(
                        () ->
                                new ColumnaDelLayout(
                                        "ASIGNATURA", CampoDelLayout.ASIGNATURA, Map.of("X", "1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dadaUnaTraduccionQueNoCubreElCatalogo_cuandoSeArma_entoncesSeRechaza() {
        assertThatThrownBy(
                        () ->
                                new ColumnaDelLayout(
                                        "DEDICACION",
                                        CampoDelLayout.DEDICACION,
                                        Map.of("TC", "1", "MT", "2")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dadoUnNombreInvalido_cuandoSeArma_entoncesSeRechaza() {
        assertThatThrownBy(() -> new ColumnaDelLayout("1A", CampoDelLayout.PERIODO, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dadaUnaTraduccionCompleta_cuandoSeArma_entoncesQuedaEnElOrdenDelCatalogo() {
        Map<String, String> traduccion = new LinkedHashMap<>();
        traduccion.put("TP", "3");
        traduccion.put("TC", "1");
        traduccion.put("MT", "2");

        ColumnaDelLayout columna =
                new ColumnaDelLayout("DEDICACION", CampoDelLayout.DEDICACION, traduccion);

        assertThat(columna.traduccion().keySet()).containsExactly("TC", "MT", "TP");
        assertThat(columna.traducida("MT")).isEqualTo("2");
    }

    @Test
    void dadoUnCampoSinTraduccion_cuandoSeTraduce_entoncesQuedaElCodigoInterno() {
        ColumnaDelLayout columna =
                new ColumnaDelLayout("COMPONENTE", CampoDelLayout.COMPONENTE, Map.of());

        assertThat(columna.traducida("CLASE")).isEqualTo("CLASE");
    }

    @Test
    void cadaCampo_tieneSuClaveYSusReglas() {
        assertThat(CampoDelLayout.values()).hasSize(15);
        assertThat(CampoDelLayout.deClave("fecha_publicacion"))
                .contains(CampoDelLayout.FECHA_PUBLICACION);
        assertThat(CampoDelLayout.deClave("Periodo")).isEmpty();
        assertThat(CampoDelLayout.COMPONENTE.codigos())
                .containsExactly(
                        "CLASE", "OTRAS_DOCENCIA", "INVESTIGACION", "VINCULACION", "GESTION");
        assertThat(CampoDelLayout.TIPO_IDENTIFICACION.codigos())
                .containsExactly("CEDULA", "PASAPORTE");
        assertThat(CampoDelLayout.DEDICACION.codigos()).containsExactly("TC", "MT", "TP");
        assertThat(CampoDelLayout.ACTIVIDAD.traducible()).isFalse();
        assertThat(CampoDelLayout.values())
                .filteredOn(CampoDelLayout::obligatorio)
                .containsExactly(
                        CampoDelLayout.IDENTIFICACION,
                        CampoDelLayout.COMPONENTE,
                        CampoDelLayout.HORAS);
    }
}
