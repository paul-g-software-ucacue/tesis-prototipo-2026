package ec.mdd.cargas.dominio;

import static ec.mdd.cargas.dominio.DiccionarioReal.DICCIONARIO;
import static ec.mdd.cargas.dominio.DiccionarioReal.carga;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** El destino de cada {@code FK} del diccionario real (research §6). */
class ReferenciaDeColumnaTest {

    @ParameterizedTest(name = "{0}.{1} → {2}")
    @CsvSource({
        "estructura_organica, codigo_unidad_padre, UNIDAD, true, -1",
        "carreras, codigo_unidad_sede, UNIDAD, false, -1",
        "asignaturas_malla, codigo_carrera, CARRERA, false, -1",
        "oferta_periodo, codigo_periodo, PERIODO, false, -1",
        "oferta_periodo, codigo_asignatura, ASIGNATURA, false, -1",
        "docentes, codigo_carrera_base, CARRERA, false, -1",
        "docentes, asignaturas_afines_declaradas, ASIGNATURA, false, -1",
        "cargos_autoridades, numero_identificacion, DOCENTE, false, -1",
        "proyectos_practicas, codigos_carrera, CARRERA, false, -1",
        "proyectos_practicas, participantes, DOCENTE, false, 0",
        "matriculados_carrera, codigo_periodo, PERIODO, false, -1"
    })
    void cadaReferenciaDelDiccionario(
            String carga, String campo, TipoDeReferencia tipo, boolean mismoArchivo, int parte) {
        ReferenciaDeColumna referencia =
                DICCIONARIO.validador(carga(carga)).referencia(campo).orElseThrow();

        assertThat(referencia.tipo()).isEqualTo(tipo);
        assertThat(referencia.mismoArchivo()).isEqualTo(mismoArchivo);
        assertThat(referencia.parte()).isEqualTo(parte);
    }

    @Test
    void unaColumnaSinFkNoTieneReferencia() {
        assertThat(DICCIONARIO.validador(carga("docentes")).referencia("primer_nombre")).isEmpty();
    }

    @Test
    void dadaUnaFkSinDestinoConocido_cuandoSeCargaElDiccionario_entoncesFalla() {
        ColumnaDeCarga columna =
                new ColumnaDeCarga(
                        "codigo",
                        "Código",
                        "Texto",
                        20,
                        "Sí",
                        "",
                        "FK → 99.codigo_otro",
                        "",
                        "ABC",
                        null);
        List<Carga> cargas = DiccionarioDePrueba.diezCargas();
        cargas.set(0, DiccionarioDePrueba.carga(1, List.of(), columna));

        assertThatThrownBy(() -> DiccionarioDePrueba.diccionario(cargas))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99.codigo_otro");
    }
}
