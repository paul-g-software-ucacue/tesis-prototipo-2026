package ec.mdd.academico.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.academico.dominio.Asignatura;
import ec.mdd.academico.dominio.CampoAmplio;
import ec.mdd.academico.dominio.CampoConocimiento;
import ec.mdd.academico.dominio.Carrera;
import ec.mdd.academico.dominio.EstadoDeCarrera;
import ec.mdd.academico.dominio.EstadoPeriodo;
import ec.mdd.academico.dominio.Jornada;
import ec.mdd.academico.dominio.Modalidad;
import ec.mdd.academico.dominio.NivelDeFormacion;
import ec.mdd.academico.dominio.PeriodoAcademico;
import ec.mdd.academico.dominio.TipoDeUnidad;
import ec.mdd.academico.dominio.UnidadDeOrganizacionCurricular;
import ec.mdd.academico.dominio.UnidadOrganica;
import ec.mdd.academico.puertos.entrada.AsignaturaDeReferencia;
import ec.mdd.academico.puertos.entrada.CampoAmplioDeReferencia;
import ec.mdd.academico.puertos.entrada.CarreraDeReferencia;
import ec.mdd.academico.puertos.entrada.PeriodoDeReferencia;
import ec.mdd.academico.puertos.entrada.ReferenciasDeAcademico;
import ec.mdd.academico.puertos.entrada.UnidadDeReferencia;
import ec.mdd.academico.puertos.salida.NormativaDelPeriodo;
import ec.mdd.academico.puertos.salida.RepositorioCarreras;
import ec.mdd.academico.puertos.salida.RepositorioEstructura;
import ec.mdd.academico.puertos.salida.RepositorioPeriodos;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

/**
 * Las referencias que Académico publica para Docentes (F03-H04, research §4), con las unidades y
 * los periodos de F03-H05 (research §2), con Mockito.
 */
class ConsultarReferenciasServicioTest {

    private final RepositorioCarreras repositorio = mock(RepositorioCarreras.class);
    private final RepositorioEstructura estructura = mock(RepositorioEstructura.class);
    private final RepositorioPeriodos periodos = mock(RepositorioPeriodos.class);
    private final NormativaDelPeriodo normativa = mock(NormativaDelPeriodo.class);
    private final ConsultarReferenciasServicio servicio =
            new ConsultarReferenciasServicio(repositorio, estructura, periodos, normativa);

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

    @Test
    void dadasUnidadesYPeriodos_cuandoSePidenLasReferencias_entoncesVanTodosPorCodigo() {
        when(estructura.todas())
                .thenReturn(
                        List.of(
                                UnidadOrganica.nueva(
                                        "G-INV",
                                        "Gestión de Investigación",
                                        TipoDeUnidad.GESTION,
                                        "VRA",
                                        "G07",
                                        null),
                                UnidadOrganica.nueva(
                                                "COM-DIST",
                                                "Comisión del distributivo",
                                                TipoDeUnidad.COMISION,
                                                "VRA",
                                                null,
                                                false)
                                        .conEstado(false)));
        when(periodos.todos())
                .thenReturn(
                        List.of(
                                PeriodoAcademico.nuevo(
                                        "2026-II",
                                        "Periodo 2026-II",
                                        LocalDate.of(2026, 10, 5),
                                        LocalDate.of(2027, 2, 5)),
                                PeriodoAcademico.nuevo(
                                        "2026-I",
                                        "Periodo 2026-I",
                                        LocalDate.of(2026, 4, 6),
                                        LocalDate.of(2026, 8, 7))));

        ReferenciasDeAcademico referencias = servicio.referencias();

        assertThat(referencias.unidades())
                .containsExactly(
                        new UnidadDeReferencia(
                                "COM-DIST", "Comisión del distributivo", "COMISION", null, false),
                        new UnidadDeReferencia(
                                "G-INV", "Gestión de Investigación", "GESTION", "G07", true));
        assertThat(referencias.periodos())
                .containsExactly(
                        new PeriodoDeReferencia(
                                "2026-I",
                                "Periodo 2026-I",
                                LocalDate.of(2026, 4, 6),
                                LocalDate.of(2026, 8, 7),
                                false,
                                false),
                        new PeriodoDeReferencia(
                                "2026-II",
                                "Periodo 2026-II",
                                LocalDate.of(2026, 10, 5),
                                LocalDate.of(2027, 2, 5),
                                false,
                                false));
    }

    @Test
    void unPeriodoConSuVersion_estaAbierto_yUnoCerrado_estaCerrado() {
        when(periodos.todos())
                .thenReturn(
                        List.of(
                                PeriodoAcademico.nuevo(
                                        "2026-I",
                                        "Periodo 2026-I",
                                        LocalDate.of(2026, 4, 6),
                                        LocalDate.of(2026, 8, 7)),
                                new PeriodoAcademico(
                                        "2025-II",
                                        "Periodo 2025-II",
                                        LocalDate.of(2025, 10, 6),
                                        LocalDate.of(2026, 2, 6),
                                        EstadoPeriodo.CERRADO)));
        when(normativa.versionDe("2026-I")).thenReturn(OptionalInt.of(1));
        when(normativa.versionDe("2025-II")).thenReturn(OptionalInt.of(1));

        assertThat(servicio.referencias().periodos())
                .extracting(
                        PeriodoDeReferencia::codigo,
                        PeriodoDeReferencia::abierto,
                        PeriodoDeReferencia::cerrado)
                .containsExactly(tuple("2025-II", true, true), tuple("2026-I", true, false));
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
