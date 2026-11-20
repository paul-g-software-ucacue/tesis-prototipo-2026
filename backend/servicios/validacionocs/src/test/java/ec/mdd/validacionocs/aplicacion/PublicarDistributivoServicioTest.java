package ec.mdd.validacionocs.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.validacionocs.dominio.EstadoDelPeriodo;
import ec.mdd.validacionocs.dominio.EtapaNoAdmitePublicacion;
import ec.mdd.validacionocs.dominio.ProcesoAprobacion;
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
import ec.mdd.validacionocs.puertos.salida.NotificacionDeTransiciones;
import ec.mdd.validacionocs.puertos.salida.RegistroDeCambios;
import ec.mdd.validacionocs.puertos.salida.RepositorioProceso;
import ec.mdd.validacionocs.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Publicar el distributivo aprobado (F18-H01, research §1, §7, contracts/validacionocs.md). */
class PublicarDistributivoServicioTest {

    private static final Instant AHORA = Instant.parse("2026-11-18T15:00:00Z");
    private static final String HASH = "d".repeat(64);
    private static final Solicitante SECRETARIA =
            new Solicitante("secretaria.demo", Set.of("SECRETARIA_GENERAL"), Optional.empty());
    private static final Transicion PUBLICADO =
            new Transicion(
                    TipoDeTransicion.PASO_DE_ETAPA,
                    TipoDeEtapa.APROBADO,
                    TipoDeEtapa.PUBLICADO,
                    Optional.empty(),
                    2,
                    HASH,
                    "secretaria.demo",
                    AHORA);

    private final RepositorioProceso procesos = mock(RepositorioProceso.class);
    private final Distributivo distributivo = mock(Distributivo.class);
    private final DatosDelPeriodo datos = mock(DatosDelPeriodo.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
    private final ConsultarFlujo flujo = mock(ConsultarFlujo.class);
    private final NotificacionDeTransiciones notificacion = mock(NotificacionDeTransiciones.class);
    private final ProcesoAprobacion bloqueado = mock(ProcesoAprobacion.class);
    private final FlujoDelDistributivo vista = mock(FlujoDelDistributivo.class);
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

    private final PublicarDistributivoServicio servicio =
            new PublicarDistributivoServicio(
                    procesos,
                    distributivo,
                    datos,
                    registro,
                    transaccion,
                    flujo,
                    Clock.fixed(AHORA, ZoneOffset.UTC),
                    notificacion);

    private void abierto() {
        when(datos.estado("2026-I")).thenReturn(Optional.of(new EstadoDelPeriodo("PLANIFICACION")));
        when(distributivo.actual("2026-I"))
                .thenReturn(
                        Optional.of(
                                new Distributivo.DistributivoActual(
                                        2, AHORA.minusSeconds(86_400 * 30), Map.of())));
        when(flujo.flujo("2026-I", SECRETARIA)).thenReturn(vista);
    }

    @Test
    void
            dadoElDistributivoAprobado_cuandoSePublica_entoncesGuardaNotificaYRegistraEnLaTransaccion() {
        abierto();
        when(procesos.bloquear("2026-I"))
                .thenAnswer(
                        invocacion -> {
                            assertThat(dentro).isTrue();
                            return Optional.of(bloqueado);
                        });
        when(bloqueado.publicar(SECRETARIA, AHORA)).thenReturn(List.of(PUBLICADO));
        doAnswer(
                        invocacion -> {
                            assertThat(dentro).as("la notificación, en la transacción").isTrue();
                            return null;
                        })
                .when(notificacion)
                .notificar(bloqueado, List.of(PUBLICADO));

        FlujoDelDistributivo resultado = servicio.publicar("2026-I", SECRETARIA);

        assertThat(resultado).isSameAs(vista);
        verify(procesos).guardar(bloqueado, List.of(PUBLICADO));
        verify(notificacion).notificar(bloqueado, List.of(PUBLICADO));
        ArgumentCaptor<CambioDeFlujo> cambio = ArgumentCaptor.forClass(CambioDeFlujo.class);
        verify(registro).registrar(cambio.capture());
        assertThat(cambio.getValue().accion())
                .isEqualTo(CambioDeFlujo.Accion.DISTRIBUTIVO_PUBLICADO);
        assertThat(cambio.getValue().usuario()).isEqualTo("secretaria.demo");
        assertThat(cambio.getValue().momento()).isEqualTo(AHORA);
        assertThat(cambio.getValue().detalle())
                .containsEntry("periodo", "2026-I")
                .containsEntry("version", 2)
                .containsEntry("hash", HASH);
        assertThat(cambio.getValue().anterior()).containsEntry("estado", "APROBADO");
        assertThat(cambio.getValue().nuevo()).containsEntry("estado", "PUBLICADO");
    }

    @Test
    void dadoQueElAgregadoNoAdmitePublicar_cuandoSePublica_entoncesNoGuardaNiNotificaNiRegistra() {
        abierto();
        when(procesos.bloquear("2026-I")).thenReturn(Optional.of(bloqueado));
        doThrow(new EtapaNoAdmitePublicacion()).when(bloqueado).publicar(SECRETARIA, AHORA);

        assertThatThrownBy(() -> servicio.publicar("2026-I", SECRETARIA))
                .isInstanceOf(EtapaNoAdmitePublicacion.class);
        verify(procesos, never()).guardar(any(), anyList());
        verify(notificacion, never()).notificar(any(), anyList());
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadoOtroRol_cuandoPublica_entoncesRolDeOtraEtapaSinGuardar() {
        abierto();
        when(procesos.bloquear("2026-I")).thenReturn(Optional.of(bloqueado));
        doThrow(new RolDeOtraEtapa()).when(bloqueado).publicar(SECRETARIA, AHORA);

        assertThatThrownBy(() -> servicio.publicar("2026-I", SECRETARIA))
                .isInstanceOf(RolDeOtraEtapa.class);
        verify(procesos, never()).guardar(any(), anyList());
        verify(notificacion, never()).notificar(any(), anyList());
    }

    @Test
    void dadoQueNoHayProceso_cuandoSePublica_entoncesEtapaNoAdmitePublicacion() {
        abierto();
        when(procesos.bloquear("2026-I")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.publicar("2026-I", SECRETARIA))
                .isInstanceOf(EtapaNoAdmitePublicacion.class);
    }

    @Test
    void dadoElPeriodoCerradoOSinDistributivo_cuandoSePublica_entoncesSuErrorSinBloquear() {
        when(datos.estado("2026-I")).thenReturn(Optional.of(new EstadoDelPeriodo("CERRADO")));

        assertThatThrownBy(() -> servicio.publicar("2026-I", SECRETARIA))
                .isInstanceOf(PeriodoCerrado.class);

        when(datos.estado("2026-I")).thenReturn(Optional.of(new EstadoDelPeriodo("PLANIFICACION")));
        when(distributivo.actual("2026-I")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.publicar("2026-I", SECRETARIA))
                .isInstanceOf(SinDistributivo.class);
        verify(procesos, never()).bloquear(any());
    }

    @Test
    void dadoUnFalloAlNotificar_cuandoSePublica_entoncesSaleDeLaTransaccion() {
        abierto();
        when(procesos.bloquear("2026-I")).thenReturn(Optional.of(bloqueado));
        when(bloqueado.publicar(SECRETARIA, AHORA)).thenReturn(List.of(PUBLICADO));
        doThrow(new IllegalStateException("sin registro de publicaciones"))
                .when(notificacion)
                .notificar(bloqueado, List.of(PUBLICADO));

        assertThatThrownBy(() -> servicio.publicar("2026-I", SECRETARIA))
                .isInstanceOf(IllegalStateException.class);
        assertThat(dentro).isFalse();
        verify(registro, never()).registrar(any());
    }
}
