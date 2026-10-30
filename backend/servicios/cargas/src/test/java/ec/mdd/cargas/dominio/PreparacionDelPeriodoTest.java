package ec.mdd.cargas.dominio;

import static ec.mdd.cargas.dominio.DiccionarioReal.DICCIONARIO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * El tablero de preparación de un periodo con el diccionario real, sin dobles (F02-H02, RF-56,
 * glosario «Tablero de preparación»).
 */
class PreparacionDelPeriodoTest {

    private static final List<String> LAS_DIEZ =
            DICCIONARIO.cargas().stream().map(Carga::codigo).toList();

    private final Set<String> conDatos = new HashSet<>(LAS_DIEZ);
    private final Map<String, MotivoDeRechazo> rechazos = new HashMap<>();
    private boolean abierto = true;
    private boolean cerrado;
    private boolean sinProyectos;

    private PreparacionDelPeriodo tablero() {
        return PreparacionDelPeriodo.de(
                DICCIONARIO,
                "2026-I",
                abierto,
                cerrado,
                conDatos::contains,
                sinProyectos,
                carga -> Optional.ofNullable(rechazos.get(carga)));
    }

    private static CargaDelPeriodo carga(PreparacionDelPeriodo tablero, String codigo) {
        return tablero.cargas().stream()
                .filter(una -> una.codigo().equals(codigo))
                .findFirst()
                .orElseThrow();
    }

    private static List<String> queFalta(PreparacionDelPeriodo tablero) {
        return tablero.faltan().stream()
                .map(falta -> falta.carga().map(CargaQueFalta::codigo).orElse(falta.tipo().name()))
                .toList();
    }

    @Test
    void conTodoYElPeriodoAbierto_estaListoYNoFaltaNada() {
        PreparacionDelPeriodo tablero = tablero();

        assertThat(tablero.listo()).isTrue();
        assertThat(tablero.faltan()).isEmpty();
        assertThat(tablero.cargas())
                .extracting(CargaDelPeriodo::numero)
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(tablero.cargas())
                .allSatisfy(una -> assertThat(una.estado()).isEqualTo(EstadoDeLaCarga.CARGADA));
        assertThat(tablero.periodo()).contains("2026-I");
    }

    @Test
    void conDocentesPendiente_faltaDocentesYNoEstaListo() {
        conDatos.remove("docentes");

        PreparacionDelPeriodo tablero = tablero();

        assertThat(tablero.listo()).isFalse();
        assertThat(carga(tablero, "docentes").estado()).isEqualTo(EstadoDeLaCarga.PENDIENTE);
        assertThat(tablero.faltan())
                .extracting(falta -> falta.carga().orElseThrow().nombre(), LoQueFalta::estado)
                .containsExactly(
                        tuple(
                                "Docentes contratados (con ficha académica)",
                                Optional.of(EstadoDeLaCarga.PENDIENTE)));
    }

    @Test
    void sinMatriculados_siguiendoListo_porqueSonRecomendados() {
        conDatos.remove("matriculados_carrera");

        PreparacionDelPeriodo tablero = tablero();

        assertThat(carga(tablero, "matriculados_carrera").estado())
                .isEqualTo(EstadoDeLaCarga.PENDIENTE);
        assertThat(tablero.listo()).isTrue();
    }

    @Test
    void unaCargaSinDatosConSuUltimoIntentoRechazado_estaConErroresYCuentaComoLoQueFalta() {
        conDatos.remove("docentes");
        rechazos.put("docentes", MotivoDeRechazo.CON_ERRORES);

        PreparacionDelPeriodo tablero = tablero();

        CargaDelPeriodo docentes = carga(tablero, "docentes");
        assertThat(docentes.estado()).isEqualTo(EstadoDeLaCarga.CON_ERRORES);
        assertThat(docentes.motivo()).contains(MotivoDeRechazo.CON_ERRORES);
        assertThat(tablero.faltan().getFirst().estado()).contains(EstadoDeLaCarga.CON_ERRORES);
        assertThat(tablero.listo()).isFalse();
    }

    @Test
    void unaCargaConDatosYUnRechazo_estaCargada() {
        rechazos.put("carreras", MotivoDeRechazo.RECHAZADA_POR_EL_DUENO);

        CargaDelPeriodo carreras = carga(tablero(), "carreras");

        assertThat(carreras.estado()).isEqualTo(EstadoDeLaCarga.CARGADA);
        assertThat(carreras.motivo()).isEmpty();
    }

    @Test
    void proyectosSinProyectosAsignablesPeroConfirmadoSinProyectos_noFalta() {
        conDatos.remove("proyectos_practicas");
        rechazos.put("proyectos_practicas", MotivoDeRechazo.CON_ERRORES);
        sinProyectos = true;

        PreparacionDelPeriodo tablero = tablero();

        assertThat(carga(tablero, "proyectos_practicas").estado())
                .isEqualTo(EstadoDeLaCarga.SIN_PROYECTOS);
        assertThat(tablero.listo()).isTrue();
    }

    @Test
    void sinProyectosYSinConfirmarlo_faltanLosProyectos() {
        conDatos.remove("proyectos_practicas");

        assertThat(queFalta(tablero())).containsExactly("proyectos_practicas");
    }

    @Test
    void elPeriodoSinAbrirYElCerradoFaltan_despuesDeLasCargasEnElOrdenDeLaErs() {
        conDatos.removeAll(List.of("docentes", "carreras"));
        abierto = false;

        assertThat(queFalta(tablero()))
                .containsExactly("carreras", "docentes", "PERIODO_SIN_ABRIR");

        abierto = true;
        cerrado = true;
        assertThat(queFalta(tablero())).containsExactly("carreras", "docentes", "PERIODO_CERRADO");
    }

    @Test
    void sinPeriodos_noHayCargasYFaltanLosPeriodos() {
        PreparacionDelPeriodo tablero = PreparacionDelPeriodo.sinPeriodos();

        assertThat(tablero.periodo()).isEmpty();
        assertThat(tablero.cargas()).isEmpty();
        assertThat(tablero.faltan())
                .extracting(LoQueFalta::tipo)
                .containsExactly(TipoDeLoQueFalta.SIN_PERIODOS);
        assertThat(tablero.listo()).isFalse();
    }

    @Test
    void elOrdenDeLasCargas_nuncaDaConErrores() {
        assertThat(new OrdenDeLasCargas(DICCIONARIO, carga -> false).estados(List.of("2026-I")))
                .extracting(EstadoDeCarga::estado)
                .doesNotContain(EstadoDeLaCarga.CON_ERRORES);
    }
}
