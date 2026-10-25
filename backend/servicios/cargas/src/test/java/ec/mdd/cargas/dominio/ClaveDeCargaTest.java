package ec.mdd.cargas.dominio;

import static ec.mdd.cargas.dominio.DiccionarioReal.carga;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/** La clave de cada carga, leída del diccionario real (research §7). */
class ClaveDeCargaTest {

    @Test
    void lasClavesSimples() {
        assertThat(ClaveDeCarga.de(carga("estructura_organica")).orElseThrow().campos())
                .containsExactly("codigo_unidad");
        assertThat(ClaveDeCarga.de(carga("docentes")).orElseThrow().campos())
                .containsExactly("numero_identificacion");
        assertThat(ClaveDeCarga.de(carga("proyectos_practicas")).orElseThrow().campos())
                .containsExactly("codigo_proyecto");
    }

    @Test
    void lasTresClavesCompuestas() {
        assertThat(ClaveDeCarga.de(carga("oferta_periodo")).orElseThrow().campos())
                .containsExactly("codigo_periodo", "codigo_asignatura", "paralelo");
        assertThat(ClaveDeCarga.de(carga("politica_carga_horaria")).orElseThrow().campos())
                .containsExactly("codigo_version_politica", "tipo_ist", "perfil");
        assertThat(ClaveDeCarga.de(carga("cargos_autoridades")).orElseThrow().campos())
                .containsExactly("numero_identificacion", "codigo_cargo", "fecha_designacion");
    }

    @Test
    void laCarga10NoTieneClave() {
        assertThat(ClaveDeCarga.de(carga("matriculados_carrera"))).isEmpty();
    }

    @Test
    void dadoUnNombreDesconocido_entoncesFalla() {
        ColumnaDeCarga columna =
                new ColumnaDeCarga(
                        "codigo",
                        "Código",
                        "Texto",
                        20,
                        "Sí",
                        "",
                        "PK compuesta: codigo + sede",
                        "",
                        "ABC",
                        null);
        Carga carga = DiccionarioDePrueba.carga(1, List.of(), columna);

        assertThatThrownBy(() -> ClaveDeCarga.de(carga))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sede");
    }
}
