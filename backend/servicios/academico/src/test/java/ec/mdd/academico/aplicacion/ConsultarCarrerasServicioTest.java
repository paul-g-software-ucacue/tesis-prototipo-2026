package ec.mdd.academico.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.academico.dominio.Asignatura;
import ec.mdd.academico.dominio.CampoAmplio;
import ec.mdd.academico.dominio.CampoConocimiento;
import ec.mdd.academico.dominio.Carrera;
import ec.mdd.academico.dominio.Jornada;
import ec.mdd.academico.dominio.Malla;
import ec.mdd.academico.dominio.Modalidad;
import ec.mdd.academico.dominio.NivelDeFormacion;
import ec.mdd.academico.dominio.UnidadDeOrganizacionCurricular;
import ec.mdd.academico.puertos.entrada.AsignaturaNoEncontrada;
import ec.mdd.academico.puertos.entrada.CarreraConMallas;
import ec.mdd.academico.puertos.entrada.CarreraNoEncontrada;
import ec.mdd.academico.puertos.salida.RepositorioCarreras;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Consultar carreras, mallas y asignaturas (FR-007, FR-010, FR-012), con Mockito. */
class ConsultarCarrerasServicioTest {

    private final RepositorioCarreras repositorio = mock(RepositorioCarreras.class);
    private final ConsultarCarrerasServicio servicio = new ConsultarCarrerasServicio(repositorio);

    private static final Carrera DS =
            Carrera.nueva(
                    "C1",
                    "DESARROLLO DE SOFTWARE",
                    "DS",
                    "MATRIZ",
                    NivelDeFormacion.TECNOLOGICO_SUPERIOR,
                    Modalidad.PRESENCIAL,
                    List.of(Jornada.NOCTURNA),
                    new CampoConocimiento("06", "061"),
                    null,
                    5);

    @BeforeEach
    void datos() {
        when(repositorio.camposAmplios()).thenReturn(List.of(new CampoAmplio("05", "Ciencias")));
        when(repositorio.carreras()).thenReturn(List.of(DS));
        when(repositorio.mallas())
                .thenReturn(List.of(Malla.nueva("DS-2019", "C1"), Malla.nueva("DS-AC-2025", "C1")));
        when(repositorio.asignaturas())
                .thenReturn(
                        List.of(
                                asignatura("A1", "DS-2019", "3", true),
                                asignatura("A2", "DS-2019", "4.5", true),
                                asignatura("A3", "DS-2019", "2", false),
                                asignatura("B1", "DS-AC-2025", "5", true)));
    }

    @Test
    void dadasLasCarreras_cuandoSeListan_entoncesCadaMallaConSusAsignaturasActivasYSusHoras() {
        List<CarreraConMallas> carreras = servicio.carreras();

        assertThat(carreras)
                .singleElement()
                .satisfies(
                        carrera -> {
                            assertThat(carrera.carrera()).isEqualTo(DS);
                            assertThat(carrera.mallas()).hasSize(2);
                            assertThat(carrera.mallas().getFirst().malla().codigo())
                                    .isEqualTo("DS-2019");
                            assertThat(carrera.mallas().getFirst().asignaturas()).isEqualTo(2);
                            assertThat(carrera.mallas().getFirst().horas())
                                    .isEqualByComparingTo("7.5");
                            assertThat(carrera.mallas().get(1).horas()).isEqualByComparingTo("5");
                        });
    }

    @Test
    void dadaUnaCarreraOUnaAsignatura_cuandoSeConsulta_entoncesLaDevuelveO404() {
        assertThat(servicio.carrera("C1").carrera()).isEqualTo(DS);
        assertThatThrownBy(() -> servicio.carrera("C9")).isInstanceOf(CarreraNoEncontrada.class);
        assertThat(servicio.asignaturasDe("C1")).hasSize(4);
        assertThatThrownBy(() -> servicio.asignaturasDe("C9"))
                .isInstanceOf(CarreraNoEncontrada.class);
        assertThat(servicio.asignatura("A2").horasSemanalesClase()).isEqualByComparingTo("4.5");
        assertThatThrownBy(() -> servicio.asignatura("ZZ"))
                .isInstanceOf(AsignaturaNoEncontrada.class);
        assertThat(servicio.camposAmplios()).extracting(CampoAmplio::codigo).containsExactly("05");
    }

    private static Asignatura asignatura(
            String codigo, String malla, String horas, boolean activa) {
        return Asignatura.nueva(
                        codigo,
                        "C1",
                        malla,
                        "ASIGNATURA",
                        1,
                        UnidadDeOrganizacionCurricular.BASICA,
                        new BigDecimal(horas),
                        null,
                        null,
                        false,
                        new CampoConocimiento("05", "054"),
                        List.of("a", "b"))
                .conEstado(activa);
    }
}
