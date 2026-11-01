package ec.mdd.academico.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.academico.dominio.Asignatura;
import ec.mdd.academico.dominio.AsignaturaParalelo;
import ec.mdd.academico.dominio.CampoConocimiento;
import ec.mdd.academico.dominio.DatosMaestros;
import ec.mdd.academico.dominio.EstadoDeAsignaturaParalelo;
import ec.mdd.academico.dominio.EstadoPeriodo;
import ec.mdd.academico.dominio.Jornada;
import ec.mdd.academico.dominio.Modalidad;
import ec.mdd.academico.dominio.OfertaDelPeriodo;
import ec.mdd.academico.dominio.UnidadDeOrganizacionCurricular;
import ec.mdd.academico.puertos.entrada.ConsultarOferta;
import ec.mdd.academico.puertos.entrada.ParaleloConCampos;
import ec.mdd.academico.puertos.entrada.PeriodoNoEncontrado;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Una asignatura-paralelo de la oferta, para sugerir docentes (ADR-023). Con Mockito. */
class ConsultarParaleloDeLaOfertaServicioTest {

    private static final String PERIODO = DatosDePrueba.CODIGO;

    private final ConsultarOferta consultar = mock(ConsultarOferta.class);
    private final ConsultarParaleloDeLaOfertaServicio servicio =
            new ConsultarParaleloDeLaOfertaServicio(consultar);

    @Test
    void dadoUnParaleloConHorasIndicadas_cuandoSeConsulta_entoncesTraeEsasHoras() {
        oferta(paralelo("A", new BigDecimal("2.5"), EstadoDeAsignaturaParalelo.OFERTADA));

        assertThat(servicio.paralelo(PERIODO, "POO-204", "A"))
                .contains(
                        new ParaleloConCampos(
                                "POO-204",
                                "Programación",
                                "TDS",
                                "A",
                                "06",
                                "061",
                                true,
                                new BigDecimal("2.5"),
                                EstadoDeAsignaturaParalelo.OFERTADA,
                                false));
    }

    @Test
    void dadoUnParaleloSinHorasIndicadas_cuandoSeConsulta_entoncesTraeLasDeLaAsignatura() {
        oferta(paralelo("A", null, EstadoDeAsignaturaParalelo.OFERTADA));

        assertThat(servicio.paralelo(PERIODO, "POO-204", "A"))
                .get()
                .extracting(ParaleloConCampos::horasSemanales)
                .isEqualTo(new BigDecimal("4"));
    }

    @Test
    void dadoUnParaleloRetirado_cuandoSeConsulta_entoncesLoDevuelveConSuEstado() {
        oferta(paralelo("A", null, EstadoDeAsignaturaParalelo.RETIRADA));

        assertThat(servicio.paralelo(PERIODO, "POO-204", "A"))
                .get()
                .extracting(ParaleloConCampos::estado)
                .isEqualTo(EstadoDeAsignaturaParalelo.RETIRADA);
    }

    @Test
    void dadoUnParaleloQueNoEstaEnLaOferta_cuandoSeConsulta_entoncesVacio() {
        oferta(paralelo("A", null, EstadoDeAsignaturaParalelo.OFERTADA));

        assertThat(servicio.paralelo(PERIODO, "POO-204", "Z")).isEmpty();
    }

    @Test
    void dadoUnPeriodoQueNoExiste_cuandoSeConsulta_entoncesPeriodoNoEncontrado() {
        when(consultar.oferta("NADA")).thenThrow(new PeriodoNoEncontrado("NADA"));

        assertThatThrownBy(() -> servicio.paralelo("NADA", "POO-204", "A"))
                .isInstanceOf(PeriodoNoEncontrado.class);
    }

    @Test
    void dadoUnPeriodoCerrado_cuandoSeConsulta_entoncesLoDice() {
        ofertaEn(EstadoPeriodo.CERRADO, paralelo("A", null, EstadoDeAsignaturaParalelo.OFERTADA));

        assertThat(servicio.paralelo(PERIODO, "POO-204", "A"))
                .get()
                .extracting(ParaleloConCampos::periodoCerrado)
                .isEqualTo(true);
    }

    @Test
    void dadaLaOferta_cuandoSeConsultaEntera_entoncesTraeTambienLasRetiradas() {
        ofertaEn(
                EstadoPeriodo.VIGENTE,
                paralelo("A", null, EstadoDeAsignaturaParalelo.OFERTADA),
                paralelo("B", null, EstadoDeAsignaturaParalelo.RETIRADA));

        assertThat(servicio.delPeriodo(PERIODO))
                .extracting(ParaleloConCampos::paralelo, ParaleloConCampos::estado)
                .containsExactly(
                        tuple("A", EstadoDeAsignaturaParalelo.OFERTADA),
                        tuple("B", EstadoDeAsignaturaParalelo.RETIRADA));
    }

    private void oferta(AsignaturaParalelo paralelo) {
        ofertaEn(EstadoPeriodo.VIGENTE, paralelo);
    }

    private void ofertaEn(EstadoPeriodo estado, AsignaturaParalelo... paralelos) {
        when(consultar.oferta(PERIODO))
                .thenReturn(
                        new OfertaDelPeriodo(
                                DatosDePrueba.periodo(estado),
                                List.of(paralelos),
                                new DatosMaestros(
                                        List.of(),
                                        List.of(),
                                        List.of(asignatura()),
                                        List.of(),
                                        List.of())));
    }

    private static AsignaturaParalelo paralelo(
            String paralelo, BigDecimal horas, EstadoDeAsignaturaParalelo estado) {
        return new AsignaturaParalelo(
                PERIODO,
                "POO-204",
                paralelo,
                Jornada.MATUTINA,
                Modalidad.PRESENCIAL,
                null,
                30,
                horas,
                estado);
    }

    private static Asignatura asignatura() {
        return new Asignatura(
                "POO-204",
                "TDS",
                "MALLA",
                "Programación",
                1,
                UnidadDeOrganizacionCurricular.BASICA,
                new BigDecimal("4"),
                null,
                null,
                true,
                new CampoConocimiento("06", "061"),
                List.of("uno", "dos"),
                true);
    }
}
