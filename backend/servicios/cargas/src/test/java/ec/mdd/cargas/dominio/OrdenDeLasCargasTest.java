package ec.mdd.cargas.dominio;

import static ec.mdd.cargas.dominio.DiccionarioReal.DICCIONARIO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * El orden de las 10 cargas con el diccionario real, sin dobles (F01-H05, RF-56, ERS §8.3): qué
 * cargas anteriores le faltan a cada una y su estado.
 */
class OrdenDeLasCargasTest {

    private static OrdenDeLasCargas con(String... conDatos) {
        Set<String> cargadas = Set.of(conDatos);
        return new OrdenDeLasCargas(DICCIONARIO, cargadas::contains);
    }

    private static List<String> codigos(List<CargaQueFalta> faltan) {
        return faltan.stream().map(CargaQueFalta::codigo).toList();
    }

    private static Map<String, EstadoDeCarga> porCodigo(List<EstadoDeCarga> estados) {
        return estados.stream()
                .collect(Collectors.toMap(EstadoDeCarga::codigo, Function.identity()));
    }

    @Test
    void sinDatos_aAsignaturasLeFaltanCarreras_yLasQueNoDependenDeNadieSePuedenSubir() {
        OrdenDeLasCargas orden = con();

        assertThat(orden.faltantes("asignaturas_malla"))
                .extracting(CargaQueFalta::numero, CargaQueFalta::codigo, CargaQueFalta::nombre)
                .containsExactly(tuple(2, "carreras", "Carreras"));
        assertThat(orden.faltantes("estructura_organica")).isEmpty();
        assertThat(orden.faltantes("ciclos_academicos")).isEmpty();
        assertThat(orden.faltantes("politica_carga_horaria")).isEmpty();
    }

    @Test
    void cargosConEstructuraYCarrerasPeroSinDocentes_soloLeFaltanDocentes() {
        OrdenDeLasCargas orden = con("estructura_organica", "carreras");

        assertThat(codigos(orden.faltantes("cargos_autoridades"))).containsExactly("docentes");
    }

    @Test
    void lasQueFaltanVanEnElOrdenDeLaErs() {
        assertThat(codigos(con().faltantes("proyectos_practicas")))
                .containsExactly("carreras", "ciclos_academicos", "docentes");
    }

    @Test
    void unaCargaQueNoEstaEnElDiccionario_esUnErrorDeProgramacion() {
        assertThatThrownBy(() -> con().faltantes("aulas"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("aulas");
    }

    @Test
    void elEstado_daLasDiezEnSuOrden_cargadasOPendientesConLoQueLesFalta() {
        List<EstadoDeCarga> estados =
                con("estructura_organica", "carreras", "politica_carga_horaria").estados(List.of());

        assertThat(estados)
                .extracting(EstadoDeCarga::numero)
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Map<String, EstadoDeCarga> porCodigo = porCodigo(estados);
        assertThat(porCodigo.get("carreras").estado()).isEqualTo(EstadoDeLaCarga.CARGADA);
        assertThat(porCodigo.get("carreras").faltan()).isEmpty();
        assertThat(porCodigo.get("asignaturas_malla").estado())
                .isEqualTo(EstadoDeLaCarga.PENDIENTE);
        assertThat(porCodigo.get("asignaturas_malla").faltan()).isEmpty();
        assertThat(codigos(porCodigo.get("oferta_periodo").faltan()))
                .containsExactly("ciclos_academicos", "asignaturas_malla");
        assertThat(porCodigo.get("politica_carga_horaria").estado())
                .isEqualTo(EstadoDeLaCarga.CARGADA);
        assertThat(estados)
                .allSatisfy(estado -> assertThat(estado.periodosSinProyectos()).isEmpty());
    }

    @Test
    void proyectosSinDatosYConPeriodosSinProyectos_estaSinProyectosConEsosPeriodos() {
        EstadoDeCarga proyectos =
                porCodigo(con().estados(List.of("2026-II", "2026-I"))).get("proyectos_practicas");

        assertThat(proyectos.estado()).isEqualTo(EstadoDeLaCarga.SIN_PROYECTOS);
        assertThat(proyectos.periodosSinProyectos()).containsExactly("2026-I", "2026-II");
    }

    @Test
    void proyectosConDatos_estaCargadaAunqueHayaPeriodosSinProyectos() {
        EstadoDeCarga proyectos =
                porCodigo(con("proyectos_practicas").estados(List.of("2026-II")))
                        .get("proyectos_practicas");

        assertThat(proyectos.estado()).isEqualTo(EstadoDeLaCarga.CARGADA);
        assertThat(proyectos.periodosSinProyectos()).containsExactly("2026-II");
    }

    @Test
    void sinProyectos_noHabilitaANingunaCarga_niSeMuestraEnLasDemas() {
        Map<String, EstadoDeCarga> estados = porCodigo(con().estados(List.of("2026-II")));

        assertThat(estados.values())
                .filteredOn(estado -> !estado.codigo().equals("proyectos_practicas"))
                .allSatisfy(
                        estado -> {
                            assertThat(estado.estado()).isEqualTo(EstadoDeLaCarga.PENDIENTE);
                            assertThat(estado.periodosSinProyectos()).isEmpty();
                            assertThat(codigos(estado.faltan()))
                                    .doesNotContain("proyectos_practicas");
                        });
    }

    @Test
    void preguntaUnaSolaVezPorCadaCarga() {
        List<String> preguntas = new ArrayList<>();
        OrdenDeLasCargas orden =
                new OrdenDeLasCargas(
                        DICCIONARIO,
                        carga -> {
                            preguntas.add(carga);
                            return false;
                        });

        orden.estados(List.of());
        orden.faltantes("cargos_autoridades");

        assertThat(preguntas).doesNotHaveDuplicates().hasSize(10);
    }
}
