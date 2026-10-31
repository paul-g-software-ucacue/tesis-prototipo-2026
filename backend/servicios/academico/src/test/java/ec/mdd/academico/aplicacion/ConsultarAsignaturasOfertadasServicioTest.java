package ec.mdd.academico.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.academico.dominio.Asignatura;
import ec.mdd.academico.dominio.AsignaturaParalelo;
import ec.mdd.academico.dominio.CampoConocimiento;
import ec.mdd.academico.dominio.EstadoDeAsignaturaParalelo;
import ec.mdd.academico.dominio.EstadoPeriodo;
import ec.mdd.academico.dominio.Jornada;
import ec.mdd.academico.dominio.Modalidad;
import ec.mdd.academico.dominio.PeriodoAcademico;
import ec.mdd.academico.dominio.UnidadDeOrganizacionCurricular;
import ec.mdd.academico.puertos.entrada.AsignaturaConCampos;
import ec.mdd.academico.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.academico.puertos.salida.RepositorioCarreras;
import ec.mdd.academico.puertos.salida.RepositorioOferta;
import ec.mdd.academico.puertos.salida.RepositorioPeriodos;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Las asignaturas ofertadas de un periodo, para calcular la afinidad (ADR-023). Con Mockito. */
class ConsultarAsignaturasOfertadasServicioTest {

    private static final String PERIODO = DatosDePrueba.CODIGO;

    private final RepositorioPeriodos periodos = mock(RepositorioPeriodos.class);
    private final RepositorioOferta oferta = mock(RepositorioOferta.class);
    private final RepositorioCarreras carreras = mock(RepositorioCarreras.class);
    private final ConsultarAsignaturasOfertadasServicio servicio =
            new ConsultarAsignaturasOfertadasServicio(periodos, oferta, carreras);

    @Test
    void dadosDosParalelosDeUnaAsignatura_cuandoSeConsulta_entoncesApareceUnaVez() {
        periodoEn(EstadoPeriodo.VIGENTE);
        when(oferta.delPeriodo(PERIODO))
                .thenReturn(
                        List.of(
                                paralelo("POO-204", "A", EstadoDeAsignaturaParalelo.OFERTADA),
                                paralelo("POO-204", "B", EstadoDeAsignaturaParalelo.OFERTADA)));
        when(carreras.asignaturas())
                .thenReturn(List.of(asignatura("POO-204"), asignatura("RED-101")));

        assertThat(servicio.delPeriodo(PERIODO))
                .containsExactly(
                        new AsignaturaConCampos("POO-204", "Asignatura POO-204", "06", "061"));
    }

    @Test
    void dadaUnaAsignaturaRetirada_cuandoSeConsulta_entoncesNoCuenta() {
        periodoEn(EstadoPeriodo.VIGENTE);
        when(oferta.delPeriodo(PERIODO))
                .thenReturn(List.of(paralelo("POO-204", "A", EstadoDeAsignaturaParalelo.RETIRADA)));
        when(carreras.asignaturas()).thenReturn(List.of(asignatura("POO-204")));

        assertThat(servicio.delPeriodo(PERIODO)).isEmpty();
    }

    @Test
    void dadoUnPeriodoCerrado_cuandoSeConsulta_entoncesDevuelveSuOferta() {
        periodoEn(EstadoPeriodo.CERRADO);
        when(oferta.delPeriodo(PERIODO))
                .thenReturn(List.of(paralelo("POO-204", "A", EstadoDeAsignaturaParalelo.OFERTADA)));
        when(carreras.asignaturas()).thenReturn(List.of(asignatura("POO-204")));

        assertThat(servicio.delPeriodo(PERIODO)).hasSize(1);
    }

    @Test
    void dadoUnPeriodoQueNoExiste_cuandoSeConsulta_entoncesPeriodoNoEncontrado() {
        when(periodos.porCodigo("NADA")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.delPeriodo("NADA"))
                .isInstanceOf(PeriodoNoEncontrado.class);
    }

    private void periodoEn(EstadoPeriodo estado) {
        PeriodoAcademico periodo = DatosDePrueba.periodo(estado);
        when(periodos.porCodigo(PERIODO)).thenReturn(Optional.of(periodo));
    }

    private static AsignaturaParalelo paralelo(
            String asignatura, String paralelo, EstadoDeAsignaturaParalelo estado) {
        return new AsignaturaParalelo(
                PERIODO,
                asignatura,
                paralelo,
                Jornada.MATUTINA,
                Modalidad.PRESENCIAL,
                null,
                30,
                null,
                estado);
    }

    private static Asignatura asignatura(String codigo) {
        return new Asignatura(
                codigo,
                "CARRERA",
                "MALLA",
                "Asignatura " + codigo,
                1,
                UnidadDeOrganizacionCurricular.BASICA,
                new BigDecimal("4"),
                null,
                null,
                false,
                new CampoConocimiento("06", "061"),
                List.of("uno", "dos"),
                true);
    }
}
