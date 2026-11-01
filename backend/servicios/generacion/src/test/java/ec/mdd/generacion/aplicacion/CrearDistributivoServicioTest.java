package ec.mdd.generacion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ec.mdd.generacion.dominio.AsignaturaOfertada;
import ec.mdd.generacion.dominio.AsignaturaParalelo;
import ec.mdd.generacion.dominio.EstadoDeVersion;
import ec.mdd.generacion.dominio.ParcialCarrera;
import ec.mdd.generacion.dominio.PeriodoListo;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.puertos.entrada.DistributivoDelPeriodo;
import ec.mdd.generacion.puertos.entrada.DistributivoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.DistributivoRepetido;
import ec.mdd.generacion.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.PeriodoNoListo;
import ec.mdd.generacion.puertos.salida.CambioDeDistributivo;
import ec.mdd.generacion.puertos.salida.DatosParaSugerencia;
import ec.mdd.generacion.puertos.salida.RegistroDeCambios;
import ec.mdd.generacion.puertos.salida.RepositorioDistributivos;
import ec.mdd.generacion.puertos.salida.Transaccion;
import ec.mdd.plataforma.eventos.ProcesoConError;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Crear el distributivo (RF-56), con los puertos simulados con Mockito. */
class CrearDistributivoServicioTest {

    private static final String PERIODO = "2026-I";
    private static final String QUIEN = "vicerrectorado.demo";
    private static final Instant AHORA = Instant.parse("2026-10-31T15:00:00Z");
    private static final PeriodoListo LISTO = new PeriodoListo(true, false, true);

    private final DatosParaSugerencia datos = mock(DatosParaSugerencia.class);
    private final RepositorioDistributivos distributivos = mock(RepositorioDistributivos.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
    private final Transaccion transaccion =
            new Transaccion() {
                @Override
                public <T> T ejecutar(Supplier<T> trabajo) {
                    return trabajo.get();
                }
            };
    private final CrearDistributivoServicio servicio =
            new CrearDistributivoServicio(
                    datos,
                    distributivos,
                    registro,
                    transaccion,
                    Clock.fixed(AHORA, ZoneOffset.UTC));

    @Test
    void dadoUnPeriodoListo_cuandoSeCrea_entoncesNaceLaVersion1ConUnParcialPorCarrera() {
        conPeriodo(LISTO);
        when(datos.ofertaDelPeriodo(PERIODO))
                .thenReturn(
                        Optional.of(
                                List.of(
                                        paralelo("A", "TDS", false),
                                        paralelo("B", "TDS", false),
                                        paralelo("A", "AGRO", false))));

        DistributivoDelPeriodo creado = servicio.crear(PERIODO, QUIEN);

        assertThat(creado.version()).isEqualTo(1);
        assertThat(creado.carreras()).containsExactly("AGRO", "TDS");
        ArgumentCaptor<VersionDistributivo> version =
                ArgumentCaptor.forClass(VersionDistributivo.class);
        verify(distributivos).crear(eq(PERIODO), version.capture(), eq(QUIEN), eq(AHORA));
        assertThat(version.getValue().estado()).isEqualTo(EstadoDeVersion.EDITABLE);
        assertThat(version.getValue().parciales())
                .extracting(ParcialCarrera::carrera)
                .containsExactly("AGRO", "TDS");
    }

    @Test
    void dadoUnPeriodoListo_cuandoSeCrea_entoncesQuedaRegistradoConQuienYCuando() {
        conPeriodo(LISTO);
        when(datos.ofertaDelPeriodo(PERIODO))
                .thenReturn(Optional.of(List.of(paralelo("A", "TDS", false))));

        servicio.crear(PERIODO, QUIEN);

        ArgumentCaptor<CambioDeDistributivo> cambio =
                ArgumentCaptor.forClass(CambioDeDistributivo.class);
        verify(registro).registrar(cambio.capture());
        assertThat(cambio.getValue().accion())
                .isEqualTo(CambioDeDistributivo.Accion.DISTRIBUTIVO_CREADO);
        assertThat(cambio.getValue().usuario()).isEqualTo(QUIEN);
        assertThat(cambio.getValue().momento()).isEqualTo(AHORA);
        assertThat(cambio.getValue().detalle()).containsEntry("periodo", PERIODO);
    }

    @Test
    void dadaUnaCarreraConTodoRetirado_cuandoSeCrea_entoncesNoTieneParcial() {
        conPeriodo(LISTO);
        when(datos.ofertaDelPeriodo(PERIODO))
                .thenReturn(
                        Optional.of(
                                List.of(paralelo("A", "TDS", false), paralelo("A", "AGRO", true))));

        assertThat(servicio.crear(PERIODO, QUIEN).carreras()).containsExactly("TDS");
    }

    @Test
    void dadoUnPeriodoQueNoEstaListo_cuandoSeCrea_entoncesNoGuardaNada() {
        conPeriodo(new PeriodoListo(true, false, false));

        assertThatThrownBy(() -> servicio.crear(PERIODO, QUIEN)).isInstanceOf(PeriodoNoListo.class);

        assertNoGuardoNada();
    }

    @Test
    void dadoUnPeriodoCerrado_cuandoSeCrea_entoncesNoGuardaNada() {
        conPeriodo(new PeriodoListo(true, true, true));

        assertThatThrownBy(() -> servicio.crear(PERIODO, QUIEN)).isInstanceOf(PeriodoNoListo.class);

        assertNoGuardoNada();
    }

    @Test
    void dadoUnPeriodoQueNoExiste_cuandoSeCrea_entoncesPeriodoNoEncontrado() {
        when(distributivos.vigenteDe(PERIODO)).thenReturn(Optional.empty());
        when(datos.periodoListo(PERIODO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crear(PERIODO, QUIEN))
                .isInstanceOf(PeriodoNoEncontrado.class);

        assertNoGuardoNada();
    }

    @Test
    void dadoUnPeriodoQueAcademicoNoTiene_cuandoSeCrea_entoncesPeriodoNoEncontrado() {
        conPeriodo(LISTO);
        when(datos.ofertaDelPeriodo(PERIODO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crear(PERIODO, QUIEN))
                .isInstanceOf(PeriodoNoEncontrado.class);

        assertNoGuardoNada();
    }

    @Test
    void dadoQueYaTieneDistributivo_cuandoSeCrea_entoncesNoConsultaNadaMas() {
        when(distributivos.vigenteDe(PERIODO))
                .thenReturn(
                        Optional.of(
                                VersionDistributivo.primera(UUID.randomUUID(), List.of("TDS"))));

        assertThatThrownBy(() -> servicio.crear(PERIODO, QUIEN))
                .isInstanceOf(DistributivoRepetido.class);

        verifyNoInteractions(datos);
        assertNoGuardoNada();
    }

    @Test
    void dadoUnFalloEntreProcesos_cuandoSeCrea_entoncesNoGuardaNada() {
        conPeriodo(LISTO);
        when(datos.ofertaDelPeriodo(PERIODO)).thenThrow(new ProcesoConError("500"));

        assertThatThrownBy(() -> servicio.crear(PERIODO, QUIEN))
                .isInstanceOf(ProcesoConError.class);

        assertNoGuardoNada();
    }

    @Test
    void dadoUnDistributivo_cuandoSeConsulta_entoncesTraeSuVersionYSusCarreras() {
        when(distributivos.vigenteDe(PERIODO))
                .thenReturn(
                        Optional.of(
                                VersionDistributivo.primera(UUID.randomUUID(), List.of("TDS"))));

        DistributivoDelPeriodo distributivo = servicio.distributivo(PERIODO);

        assertThat(distributivo.periodo()).isEqualTo(PERIODO);
        assertThat(distributivo.version()).isEqualTo(1);
        assertThat(distributivo.carreras()).containsExactly("TDS");
    }

    @Test
    void dadoQueNoTieneDistributivo_cuandoSeConsulta_entoncesDistributivoNoEncontrado() {
        when(distributivos.vigenteDe(PERIODO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.distributivo(PERIODO))
                .isInstanceOf(DistributivoNoEncontrado.class);
    }

    private void conPeriodo(PeriodoListo preparacion) {
        when(distributivos.vigenteDe(PERIODO)).thenReturn(Optional.empty());
        when(datos.periodoListo(PERIODO)).thenReturn(Optional.of(preparacion));
    }

    private void assertNoGuardoNada() {
        verify(distributivos, never()).crear(any(), any(), any(), any());
        verify(registro, never()).registrar(any());
    }

    private static AsignaturaParalelo paralelo(String paralelo, String carrera, boolean retirada) {
        return new AsignaturaParalelo(
                new AsignaturaOfertada("POO-204", "Programación", "06", "061"),
                paralelo,
                carrera,
                new BigDecimal("4"),
                false,
                retirada);
    }
}
