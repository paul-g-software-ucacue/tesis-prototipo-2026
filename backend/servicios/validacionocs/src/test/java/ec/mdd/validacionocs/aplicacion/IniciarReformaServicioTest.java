package ec.mdd.validacionocs.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.validacionocs.dominio.EstadoDelPeriodo;
import ec.mdd.validacionocs.dominio.EtapaNoAdmiteReforma;
import ec.mdd.validacionocs.dominio.MotivoDeReformaInvalido;
import ec.mdd.validacionocs.dominio.Observacion;
import ec.mdd.validacionocs.dominio.ProcesoAprobacion;
import ec.mdd.validacionocs.dominio.Reforma;
import ec.mdd.validacionocs.dominio.RolDeOtraEtapa;
import ec.mdd.validacionocs.dominio.Solicitante;
import ec.mdd.validacionocs.dominio.TipoDeEtapa;
import ec.mdd.validacionocs.dominio.TipoDeTransicion;
import ec.mdd.validacionocs.dominio.Transicion;
import ec.mdd.validacionocs.puertos.entrada.ConsultarFlujo;
import ec.mdd.validacionocs.puertos.entrada.FlujoDelDistributivo;
import ec.mdd.validacionocs.puertos.entrada.PeriodoCerrado;
import ec.mdd.validacionocs.puertos.entrada.SinDistributivo;
import ec.mdd.validacionocs.puertos.salida.CambioDeFlujo;
import ec.mdd.validacionocs.puertos.salida.DatosDelPeriodo;
import ec.mdd.validacionocs.puertos.salida.Distributivo;
import ec.mdd.validacionocs.puertos.salida.Distributivo.VersionNuevaEnAprobacion;
import ec.mdd.validacionocs.puertos.salida.NotificacionDeTransiciones;
import ec.mdd.validacionocs.puertos.salida.RegistroDeCambios;
import ec.mdd.validacionocs.puertos.salida.RepositorioProceso;
import ec.mdd.validacionocs.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Iniciar una reforma del distributivo publicado (F18-H02, research §1, §2, §6, §7). */
class IniciarReformaServicioTest {

    private static final Instant AHORA = Instant.parse("2026-11-20T15:00:00Z");
    private static final String HASH_PUBLICADA = "d".repeat(64);
    private static final String HASH_NUEVA = "e".repeat(64);
    private static final String MOTIVO = "Cambia el docente de Software por una licencia.";
    private static final Solicitante VICERRECTOR =
            new Solicitante("vicerrectorado.demo", Set.of("VICERRECTOR"), Optional.empty());
    private static final Transicion PUBLICACION =
            new Transicion(
                    TipoDeTransicion.PASO_DE_ETAPA,
                    TipoDeEtapa.APROBADO,
                    TipoDeEtapa.PUBLICADO,
                    Optional.empty(),
                    2,
                    HASH_PUBLICADA,
                    "secretaria.demo",
                    AHORA.minusSeconds(86_400));
    private static final Transicion REFORMA =
            new Transicion(
                    TipoDeTransicion.REFORMA,
                    TipoDeEtapa.PUBLICADO,
                    TipoDeEtapa.EN_CONSOLIDACION,
                    Optional.empty(),
                    3,
                    HASH_NUEVA,
                    "vicerrectorado.demo",
                    AHORA,
                    Map.of(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of(new Reforma(2, new Observacion(MOTIVO))));

    private final RepositorioProceso procesos = mock(RepositorioProceso.class);
    private final Distributivo distributivo = mock(Distributivo.class);
    private final DatosDelPeriodo datos = mock(DatosDelPeriodo.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
    private final ConsultarFlujo flujo = mock(ConsultarFlujo.class);
    private final NotificacionDeTransiciones notificacion = mock(NotificacionDeTransiciones.class);
    private final ProcesoAprobacion leido = mock(ProcesoAprobacion.class);
    private final ProcesoAprobacion bloqueado = mock(ProcesoAprobacion.class);
    private final FlujoDelDistributivo vista = mock(FlujoDelDistributivo.class);
    private final List<String> eventos = new ArrayList<>();
    private boolean dentro;
    private final Transaccion transaccion =
            new Transaccion() {
                @Override
                public <T> T ejecutar(Supplier<T> trabajo) {
                    dentro = true;
                    try {
                        return trabajo.get();
                    } finally {
                        dentro = false;
                    }
                }
            };

    private final IniciarReformaServicio servicio =
            new IniciarReformaServicio(
                    procesos,
                    distributivo,
                    datos,
                    registro,
                    transaccion,
                    flujo,
                    Clock.fixed(AHORA, ZoneOffset.UTC),
                    notificacion);

    private void publicado() {
        when(datos.estado("2026-I")).thenReturn(Optional.of(new EstadoDelPeriodo("PLANIFICACION")));
        when(distributivo.actual("2026-I"))
                .thenReturn(
                        Optional.of(
                                new Distributivo.DistributivoActual(
                                        2, AHORA.minusSeconds(86_400 * 30), Map.of())));
        when(procesos.de("2026-I")).thenReturn(Optional.of(leido));
        when(leido.publicacion()).thenReturn(Optional.of(PUBLICACION));
        when(procesos.bloquear("2026-I"))
                .thenAnswer(
                        uso -> {
                            eventos.add("bloquear, dentro: " + dentro);
                            return Optional.of(bloqueado);
                        });
        when(distributivo.crearVersionDeReforma("2026-I", 2, "vicerrectorado.demo", AHORA))
                .thenAnswer(
                        uso -> {
                            eventos.add("generación, dentro: " + dentro);
                            return Optional.of(new VersionNuevaEnAprobacion(3, HASH_NUEVA));
                        });
        when(flujo.flujo("2026-I", VICERRECTOR)).thenReturn(vista);
    }

    @Test
    void
            dadoElDistributivoPublicado_cuandoSeIniciaLaReforma_entoncesValidaCreaLaVersionYRegistra() {
        publicado();
        when(bloqueado.iniciarReforma(VICERRECTOR, new Observacion(MOTIVO), 3, HASH_NUEVA, AHORA))
                .thenReturn(List.of(REFORMA));

        FlujoDelDistributivo resultado =
                servicio.iniciar("2026-I", "  " + MOTIVO + "  ", VICERRECTOR);

        assertThat(resultado).isSameAs(vista);
        verify(leido).validarReforma(VICERRECTOR);
        // Generación, fuera de la transacción de Aprobación y antes de bloquear (D-106).
        assertThat(eventos).containsExactly("generación, dentro: false", "bloquear, dentro: true");
        verify(procesos).guardar(bloqueado, List.of(REFORMA));
        verify(notificacion).notificar(bloqueado, List.of(REFORMA));
        ArgumentCaptor<CambioDeFlujo> cambio = ArgumentCaptor.forClass(CambioDeFlujo.class);
        verify(registro).registrar(cambio.capture());
        assertThat(cambio.getValue().accion()).isEqualTo(CambioDeFlujo.Accion.REFORMA_INICIADA);
        assertThat(cambio.getValue().usuario()).isEqualTo("vicerrectorado.demo");
        assertThat(cambio.getValue().momento()).isEqualTo(AHORA);
        assertThat(cambio.getValue().detalle())
                .containsEntry("periodo", "2026-I")
                .containsEntry("versionPublicada", 2)
                .containsEntry("version", 3)
                .containsEntry("hash", HASH_NUEVA)
                .containsEntry("motivo", MOTIVO);
        assertThat(cambio.getValue().anterior()).containsEntry("estado", "PUBLICADO");
        assertThat(cambio.getValue().nuevo()).containsEntry("estado", "EN_CONSOLIDACION");
    }

    @Test
    void dadoUnMotivoInvalido_cuandoSeIniciaLaReforma_entoncesSeRechazaSinPedirNada() {
        for (String motivo : java.util.Arrays.asList(null, "", "   corta   ", "x".repeat(501))) {
            assertThatThrownBy(() -> servicio.iniciar("2026-I", motivo, VICERRECTOR))
                    .isInstanceOf(MotivoDeReformaInvalido.class);
        }
        verify(datos, never()).estado(anyString());
        verify(distributivo, never()).crearVersionDeReforma(any(), anyInt(), any(), any());
        verify(procesos, never()).bloquear(any());
    }

    @Test
    void dadoQueElAgregadoNoAdmiteLaReforma_cuandoSeInicia_entoncesNoPideLaVersion() {
        publicado();
        doThrow(new EtapaNoAdmiteReforma()).when(leido).validarReforma(VICERRECTOR);

        assertThatThrownBy(() -> servicio.iniciar("2026-I", MOTIVO, VICERRECTOR))
                .isInstanceOf(EtapaNoAdmiteReforma.class);

        doThrow(new RolDeOtraEtapa()).when(leido).validarReforma(VICERRECTOR);
        assertThatThrownBy(() -> servicio.iniciar("2026-I", MOTIVO, VICERRECTOR))
                .isInstanceOf(RolDeOtraEtapa.class);
        verify(distributivo, never()).crearVersionDeReforma(any(), anyInt(), any(), any());
        verify(procesos, never()).bloquear(any());
    }

    @Test
    void dadoQueNoHayProceso_cuandoSeIniciaLaReforma_entoncesEtapaNoAdmiteReforma() {
        publicado();
        when(procesos.de("2026-I")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.iniciar("2026-I", MOTIVO, VICERRECTOR))
                .isInstanceOf(EtapaNoAdmiteReforma.class);
        verify(distributivo, never()).crearVersionDeReforma(any(), anyInt(), any(), any());
    }

    @Test
    void
            dadoQueGeneracionNoTieneLaVersion_cuandoSeIniciaLaReforma_entoncesErrorTecnicoSinBloquear() {
        publicado();
        when(distributivo.crearVersionDeReforma("2026-I", 2, "vicerrectorado.demo", AHORA))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.iniciar("2026-I", MOTIVO, VICERRECTOR))
                .isInstanceOf(IllegalStateException.class);
        verify(procesos, never()).bloquear(any());
    }

    @Test
    void dadaLaSegundaDeDosReformas_cuandoSeBloquea_entoncesNoGuardaNiNotificaNiRegistra() {
        publicado();
        doThrow(new EtapaNoAdmiteReforma())
                .when(bloqueado)
                .iniciarReforma(VICERRECTOR, new Observacion(MOTIVO), 3, HASH_NUEVA, AHORA);

        assertThatThrownBy(() -> servicio.iniciar("2026-I", MOTIVO, VICERRECTOR))
                .isInstanceOf(EtapaNoAdmiteReforma.class);
        verify(procesos, never()).guardar(any(), anyList());
        verify(notificacion, never()).notificar(any(), anyList());
        verify(registro, never()).registrar(any());
    }

    @Test
    void
            dadoElPeriodoCerradoOSinDistributivo_cuandoSeIniciaLaReforma_entoncesSuErrorSinPedirNada() {
        when(datos.estado("2026-I")).thenReturn(Optional.of(new EstadoDelPeriodo("CERRADO")));

        assertThatThrownBy(() -> servicio.iniciar("2026-I", MOTIVO, VICERRECTOR))
                .isInstanceOf(PeriodoCerrado.class);

        when(datos.estado("2026-I")).thenReturn(Optional.of(new EstadoDelPeriodo("PLANIFICACION")));
        when(distributivo.actual("2026-I")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.iniciar("2026-I", MOTIVO, VICERRECTOR))
                .isInstanceOf(SinDistributivo.class);
        verify(distributivo, never()).crearVersionDeReforma(any(), anyInt(), any(), any());
        verify(procesos, never()).bloquear(any());
    }
}
