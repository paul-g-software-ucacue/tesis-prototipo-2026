package ec.mdd.academico.aplicacion;

import static ec.mdd.academico.aplicacion.DatosDePrueba.RELOJ;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.academico.dominio.Asignatura;
import ec.mdd.academico.dominio.AsignaturaParalelo;
import ec.mdd.academico.dominio.AsignaturaParaleloRepetida;
import ec.mdd.academico.dominio.CampoAmplio;
import ec.mdd.academico.dominio.CampoConocimiento;
import ec.mdd.academico.dominio.Carrera;
import ec.mdd.academico.dominio.EstadoDeAsignaturaParalelo;
import ec.mdd.academico.dominio.EstadoPeriodo;
import ec.mdd.academico.dominio.Jornada;
import ec.mdd.academico.dominio.Malla;
import ec.mdd.academico.dominio.Modalidad;
import ec.mdd.academico.dominio.NivelDeFormacion;
import ec.mdd.academico.dominio.OfertaCerrada;
import ec.mdd.academico.dominio.PeriodoAcademico;
import ec.mdd.academico.dominio.TipoDeUnidad;
import ec.mdd.academico.dominio.UnidadDeOrganizacionCurricular;
import ec.mdd.academico.dominio.UnidadOrganica;
import ec.mdd.academico.puertos.entrada.AsignaturaParaleloNoEncontrada;
import ec.mdd.academico.puertos.entrada.DatosDeAsignaturaParalelo;
import ec.mdd.academico.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.academico.puertos.salida.CambioRegistrado;
import ec.mdd.academico.puertos.salida.CambioRegistrado.Accion;
import ec.mdd.academico.puertos.salida.RegistroDeCambios;
import ec.mdd.academico.puertos.salida.RepositorioCarreras;
import ec.mdd.academico.puertos.salida.RepositorioEstructura;
import ec.mdd.academico.puertos.salida.RepositorioOferta;
import ec.mdd.academico.puertos.salida.RepositorioPeriodos;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

/**
 * Mantener la oferta del periodo (FR-002, FR-003, FR-010): cada cambio se valida con el periodo,
 * las carreras y la oferta bloqueados, se guarda y se registra en la misma transacción. Con
 * Mockito.
 */
class MantenerOfertaServicioTest {

    private static final String QUIEN = "vicerrectorado.demo";
    private static final String PERIODO = "2026-I";

    private final RepositorioPeriodos periodos = mock(RepositorioPeriodos.class);
    private final RepositorioOferta repositorio = mock(RepositorioOferta.class);
    private final RepositorioCarreras carreras = mock(RepositorioCarreras.class);
    private final RepositorioEstructura estructura = mock(RepositorioEstructura.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
    private final DatosDePrueba.TransaccionDePrueba transaccion =
            new DatosDePrueba.TransaccionDePrueba();
    private final MantenerOfertaServicio servicio =
            new MantenerOfertaServicio(
                    periodos, repositorio, carreras, estructura, registro, transaccion, RELOJ);

    private final List<AsignaturaParalelo> oferta = new ArrayList<>();
    private EstadoPeriodo estado = EstadoPeriodo.PLANIFICACION;

    @BeforeEach
    void datos() {
        when(periodos.bloquear(PERIODO))
                .thenAnswer(
                        invocacion ->
                                Optional.of(
                                        new PeriodoAcademico(
                                                PERIODO,
                                                "Periodo 2026-I",
                                                LocalDate.of(2026, 4, 6),
                                                LocalDate.of(2026, 8, 7),
                                                estado)));
        when(estructura.todas())
                .thenReturn(
                        List.of(
                                UnidadOrganica.nueva(
                                        "MATRIZ",
                                        "Sede matriz",
                                        TipoDeUnidad.SEDE_MATRIZ,
                                        null,
                                        null,
                                        null)));
        when(carreras.camposAmplios()).thenReturn(List.of(new CampoAmplio("06", "TIC")));
        when(carreras.carreras())
                .thenReturn(
                        List.of(
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
                                        5)));
        when(carreras.mallas()).thenReturn(List.of(Malla.nueva("M1", "C1")));
        when(carreras.asignaturas())
                .thenReturn(
                        List.of(
                                Asignatura.nueva(
                                        "A1",
                                        "C1",
                                        "M1",
                                        "PROGRAMACIÓN",
                                        1,
                                        UnidadDeOrganizacionCurricular.BASICA,
                                        new BigDecimal("4"),
                                        null,
                                        null,
                                        false,
                                        new CampoConocimiento("06", "061"),
                                        List.of("uno", "dos"))));
        when(repositorio.delPeriodo(PERIODO)).thenAnswer(invocacion -> List.copyOf(oferta));
    }

    static AsignaturaParalelo nocturna(String paralelo) {
        return AsignaturaParalelo.nueva(
                PERIODO, "A1", paralelo, Jornada.NOCTURNA, Modalidad.PRESENCIAL, null, 28, null);
    }

    @Test
    void dadaUnaAsignaturaParaleloValida_cuandoSeOferta_entoncesBloqueaValidaGuardaYRegistra() {
        AsignaturaParalelo nueva = nocturna("A_NOCTURNA");

        assertThat(servicio.ofertar(nueva, QUIEN)).isEqualTo(nueva);

        InOrder orden = inOrder(periodos, carreras, repositorio, registro);
        orden.verify(periodos).bloquear(PERIODO);
        orden.verify(carreras).bloquear();
        orden.verify(repositorio).bloquear();
        orden.verify(repositorio).registrar(nueva, QUIEN, DatosDePrueba.AHORA);
        CambioRegistrado cambio = registrado();
        assertThat(cambio.accion()).isEqualTo(Accion.ASIGNATURA_PARALELO_REGISTRADA);
        assertThat(cambio.detalle())
                .containsEntry("periodo", PERIODO)
                .containsEntry("asignatura", "A1")
                .containsEntry("paralelo", "A_NOCTURNA");
        assertThat(transaccion.dentro()).isFalse();
    }

    @Test
    void dadaUnaRepetidaUnPeriodoCerradoOInexistente_cuandoSeOferta_entoncesSuErrorSinRegistro() {
        oferta.add(nocturna("A_NOCTURNA"));

        assertThatThrownBy(() -> servicio.ofertar(nocturna("A_NOCTURNA"), QUIEN))
                .isInstanceOf(AsignaturaParaleloRepetida.class);
        estado = EstadoPeriodo.EN_APROBACION;
        assertThatThrownBy(() -> servicio.ofertar(nocturna("B_NOCTURNA"), QUIEN))
                .isInstanceOf(OfertaCerrada.class);
        when(periodos.bloquear("2027-I")).thenReturn(Optional.empty());
        assertThatThrownBy(
                        () ->
                                servicio.ofertar(
                                        AsignaturaParalelo.nueva(
                                                "2027-I",
                                                "A1",
                                                "A_NOCTURNA",
                                                Jornada.NOCTURNA,
                                                Modalidad.PRESENCIAL,
                                                null,
                                                28,
                                                null),
                                        QUIEN))
                .isInstanceOf(PeriodoNoEncontrado.class);
        verify(repositorio, never()).registrar(any(), anyString(), any());
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadaUnaOfertada_cuandoSeCorrige_entoncesRegistraCadaCambioYSinCambiosNoRegistra() {
        oferta.add(nocturna("A_NOCTURNA"));
        DatosDeAsignaturaParalelo datos =
                new DatosDeAsignaturaParalelo(Jornada.NOCTURNA, Modalidad.DUAL, null, 30, null);

        AsignaturaParalelo corregida = servicio.corregir(PERIODO, "A1", "A_NOCTURNA", datos, QUIEN);

        assertThat(corregida.modalidad()).isEqualTo(Modalidad.DUAL);
        verify(repositorio).guardar(corregida);
        CambioRegistrado cambio = registrado();
        assertThat(cambio.accion()).isEqualTo(Accion.ASIGNATURA_PARALELO_CORREGIDA);
        assertThat(cambio.detalle()).containsEntry("paralelo", "A_NOCTURNA").containsKey("cambios");
        oferta.set(0, corregida);
        servicio.corregir(PERIODO, "A1", "A_NOCTURNA", datos, QUIEN);
        verify(registro, times(1)).registrar(any());
        assertThatThrownBy(() -> servicio.corregir(PERIODO, "A1", "Z", datos, QUIEN))
                .isInstanceOf(AsignaturaParaleloNoEncontrada.class);
    }

    @Test
    void dadaUnaOfertada_cuandoSeRetiraYSeVuelveAOfertar_entoncesRegistraCadaUnaYElMismoEstadoNo() {
        oferta.add(nocturna("A_NOCTURNA"));

        AsignaturaParalelo retirada =
                servicio.cambiarEstado(
                        PERIODO, "A1", "A_NOCTURNA", EstadoDeAsignaturaParalelo.RETIRADA, QUIEN);
        oferta.set(0, retirada);
        servicio.cambiarEstado(
                PERIODO, "A1", "A_NOCTURNA", EstadoDeAsignaturaParalelo.RETIRADA, QUIEN);
        AsignaturaParalelo otraVez =
                servicio.cambiarEstado(
                        PERIODO, "A1", "A_NOCTURNA", EstadoDeAsignaturaParalelo.OFERTADA, QUIEN);

        assertThat(retirada.estado()).isEqualTo(EstadoDeAsignaturaParalelo.RETIRADA);
        assertThat(otraVez.estado()).isEqualTo(EstadoDeAsignaturaParalelo.OFERTADA);
        ArgumentCaptor<CambioRegistrado> cambios = ArgumentCaptor.forClass(CambioRegistrado.class);
        verify(registro, times(2)).registrar(cambios.capture());
        assertThat(cambios.getAllValues())
                .extracting(CambioRegistrado::accion)
                .containsExactly(
                        Accion.ASIGNATURA_PARALELO_RETIRADA, Accion.ASIGNATURA_PARALELO_REOFERTADA);
        assertThat(cambios.getAllValues().getFirst().detalle())
                .isEqualTo(
                        Map.of("periodo", PERIODO, "asignatura", "A1", "paralelo", "A_NOCTURNA"));
    }

    @Test
    void dadoUnPeriodoFueraDePlanificacion_cuandoSePideElMismoEstado_entoncesLanzaOfertaCerrada() {
        oferta.add(nocturna("A_NOCTURNA"));
        estado = EstadoPeriodo.VIGENTE;

        assertThatThrownBy(
                        () ->
                                servicio.cambiarEstado(
                                        PERIODO,
                                        "A1",
                                        "A_NOCTURNA",
                                        EstadoDeAsignaturaParalelo.OFERTADA,
                                        QUIEN))
                .isInstanceOf(OfertaCerrada.class);
    }

    private CambioRegistrado registrado() {
        ArgumentCaptor<CambioRegistrado> cambio = ArgumentCaptor.forClass(CambioRegistrado.class);
        verify(registro).registrar(cambio.capture());
        return cambio.getValue();
    }
}
