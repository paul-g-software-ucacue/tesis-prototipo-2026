package ec.mdd.academico.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.academico.dominio.Asignatura;
import ec.mdd.academico.dominio.CampoAmplio;
import ec.mdd.academico.dominio.CampoConocimiento;
import ec.mdd.academico.dominio.Carrera;
import ec.mdd.academico.dominio.EstadoDeCarrera;
import ec.mdd.academico.dominio.Jornada;
import ec.mdd.academico.dominio.Modalidad;
import ec.mdd.academico.dominio.NivelDeFormacion;
import ec.mdd.academico.dominio.UnidadDeOrganizacionCurricular;
import ec.mdd.academico.puertos.entrada.AsignaturaDeReferencia;
import ec.mdd.academico.puertos.entrada.CampoAmplioDeReferencia;
import ec.mdd.academico.puertos.entrada.CarreraDeReferencia;
import ec.mdd.academico.puertos.entrada.ReferenciasDeAcademico;
import ec.mdd.academico.puertos.salida.RepositorioCarreras;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Las referencias que Académico publica para Docentes (F03-H04, research §4), con Mockito. */
class ConsultarReferenciasServicioTest {

    private final RepositorioCarreras repositorio = mock(RepositorioCarreras.class);
    private final ConsultarReferenciasServicio servicio =
            new ConsultarReferenciasServicio(repositorio);

    @Test
    void dadasCarrerasYAsignaturas_cuandoSePidenLasReferencias_entoncesVanTodasPorCodigo() {
        when(repositorio.carreras())
                .thenReturn(
                        List.of(
                                carrera("C2", "CONTABILIDAD", "CO")
                                        .conEstado(EstadoDeCarrera.EN_CIERRE),
                                carrera("C1", "DESARROLLO DE SOFTWARE", "DS")));
        when(repositorio.asignaturas())
                .thenReturn(
                        List.of(
                                asignatura("B1", "C2", "CONTABILIDAD I").conEstado(false),
                                asignatura("A1", "C1", "PROGRAMACIÓN")));
        when(repositorio.camposAmplios())
                .thenReturn(
                        List.of(
                                new CampoAmplio("06", "Tecnologías de la información"),
                                new CampoAmplio("04", "Administración de empresas y derecho")));

        ReferenciasDeAcademico referencias = servicio.referencias();

        assertThat(referencias.carreras())
                .containsExactly(
                        new CarreraDeReferencia("C1", "DESARROLLO DE SOFTWARE", "DS"),
                        new CarreraDeReferencia("C2", "CONTABILIDAD", "CO"));
        assertThat(referencias.asignaturas())
                .containsExactly(
                        new AsignaturaDeReferencia("A1", "PROGRAMACIÓN", "C1"),
                        new AsignaturaDeReferencia("B1", "CONTABILIDAD I", "C2"));
        assertThat(referencias.camposAmplios())
                .extracting(CampoAmplioDeReferencia::codigo)
                .containsExactly("04", "06");
    }

    private static Carrera carrera(String codigo, String nombre, String siglas) {
        return Carrera.nueva(
                codigo,
                nombre,
                siglas,
                "MATRIZ",
                NivelDeFormacion.TECNOLOGICO_SUPERIOR,
                Modalidad.PRESENCIAL,
                List.of(Jornada.NOCTURNA),
                new CampoConocimiento("06", "061"),
                null,
                5);
    }

    private static Asignatura asignatura(String codigo, String carrera, String nombre) {
        return Asignatura.nueva(
                codigo,
                carrera,
                "M-2019",
                nombre,
                1,
                UnidadDeOrganizacionCurricular.BASICA,
                new BigDecimal("4"),
                null,
                null,
                false,
                new CampoConocimiento("06", "061"),
                List.of("a", "b"));
    }
}
