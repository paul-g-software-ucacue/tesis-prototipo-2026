package ec.mdd.validacionocs.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.validacionocs.adaptadores.salida.LectorYaml;
import ec.mdd.validacionocs.adaptadores.salida.PlantillaEnRecursos;
import ec.mdd.validacionocs.dominio.EstadoDeEnvio;
import ec.mdd.validacionocs.dominio.EstadoDelPeriodo;
import ec.mdd.validacionocs.dominio.Plazo;
import ec.mdd.validacionocs.dominio.ProcesoAprobacion;
import ec.mdd.validacionocs.dominio.TipoDeEtapa;
import ec.mdd.validacionocs.dominio.TipoDeTransicion;
import ec.mdd.validacionocs.dominio.Transicion;
import ec.mdd.validacionocs.dominio.VencimientoDePlazo;
import ec.mdd.validacionocs.puertos.salida.AvisoDePlazoVencido;
import ec.mdd.validacionocs.puertos.salida.CambioDeFlujo;
import ec.mdd.validacionocs.puertos.salida.DatosDelPeriodo;
import ec.mdd.validacionocs.puertos.salida.RegistroDeCambios;
import ec.mdd.validacionocs.puertos.salida.RepositorioConfiguracion;
import ec.mdd.validacionocs.puertos.salida.RepositorioPlazos;
import ec.mdd.validacionocs.puertos.salida.RepositorioProceso;
import ec.mdd.validacionocs.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

/**
 * La revisión diaria de plazos (F12-H03, research §3, D-114): calcula, comprueba el periodo, con el
 * proceso bloqueado vuelve a calcular, registra y, solo si registró, avisa y deja la bitácora.
 */
class RevisarPlazosServicioTest {

    private static final Instant CREADO = Instant.parse("2026-11-01T15:00:00Z");
    // La llegada a EN_CONSOLIDACION; su plazo de 5 días vence el 2026-11-09.
    private static final Instant LLEGADA = Instant.parse("2026-11-04T15:00:00Z");
    private static final Instant VENCIDO = Instant.parse("2026-11-10T12:00:00Z");
    private static final Instant VENCE_HOY = Instant.parse("2026-11-09T20:00:00Z");
    private static final String HASH = "a".repeat(64);

    private final RepositorioProceso procesos = mock(RepositorioProceso.class);
    private final RepositorioConfiguracion configuraciones = mock(RepositorioConfiguracion.class);
    private final DatosDelPeriodo datos = mock(DatosDelPeriodo.class);
    private final RepositorioPlazos plazos = mock(RepositorioPlazos.class);
    private final AvisoDePlazoVencido aviso = mock(AvisoDePlazoVencido.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
    private final Transaccion transaccion = mock(Transaccion.class);
    private boolean dentro;

    @BeforeEach
    void preparar() {
        when(configuraciones.vigenteEn(any(), any())).thenReturn(Optional.empty());
        when(datos.estado(any())).thenReturn(Optional.of(new EstadoDelPeriodo("EN_APROBACION")));
        when(plazos.registrar(any())).thenReturn(true);
        when(transaccion.ejecutar(any()))
                .thenAnswer(
                        invocacion -> {
                            dentro = true;
                            try {
                                return ((Supplier<?>) invocacion.getArgument(0)).get();
                            } finally {
                                dentro = false;
                            }
                        });
    }

    @Test
    void dadoUnPlazoVencido_cuandoSeRevisa_entoncesRegistraAvisaYAnotaEnEseOrden() {
        enCurso("2026-I", enConsolidacion("2026-I"));
        when(plazos.registrar(any()))
                .thenAnswer(
                        invocacion -> {
                            assertThat(dentro).isTrue();
                            return true;
                        });

        assertThat(servicio(VENCIDO).revisar()).isEqualTo(1);

        InOrder orden = inOrder(datos, procesos, plazos, aviso, registro);
        orden.verify(datos).estado("2026-I");
        orden.verify(procesos).bloquear("2026-I");
        ArgumentCaptor<VencimientoDePlazo> vencimiento =
                ArgumentCaptor.forClass(VencimientoDePlazo.class);
        orden.verify(plazos).registrar(vencimiento.capture());
        orden.verify(aviso).avisar(any(), any());
        ArgumentCaptor<CambioDeFlujo> cambio = ArgumentCaptor.forClass(CambioDeFlujo.class);
        orden.verify(registro).registrar(cambio.capture());
        assertThat(vencimiento.getValue())
                .isEqualTo(
                        new VencimientoDePlazo(
                                "2026-I",
                                Plazo.de(TipoDeEtapa.EN_CONSOLIDACION, LLEGADA, 5),
                                VENCIDO,
                                1));
        assertThat(cambio.getValue().accion()).isEqualTo(CambioDeFlujo.Accion.PLAZO_VENCIDO);
        assertThat(cambio.getValue().usuario()).isEqualTo("sistema");
        assertThat(cambio.getValue().momento()).isEqualTo(VENCIDO);
        assertThat(cambio.getValue().detalle())
                .isEqualTo(Map.of("periodo", "2026-I", "etapa", "EN_CONSOLIDACION"));
        assertThat(cambio.getValue().nuevo())
                .isEqualTo(Map.of("fechaLimite", "2026-11-09", "diasDeAtraso", 1));
    }

    @Test
    void dadoUnPlazoQueVenceHoy_cuandoSeRevisa_entoncesNoHaceNadaNiPreguntaPorElPeriodo() {
        enCurso("2026-I", enConsolidacion("2026-I"));

        assertThat(servicio(VENCE_HOY).revisar()).isZero();

        verify(datos, never()).estado(any());
        verify(transaccion, never()).ejecutar(any());
    }

    @Test
    void dadoElPeriodoCerrado_cuandoSeRevisa_entoncesNoAvisa() {
        enCurso("2026-I", enConsolidacion("2026-I"));
        when(datos.estado("2026-I")).thenReturn(Optional.of(new EstadoDelPeriodo("CERRADO")));

        assertThat(servicio(VENCIDO).revisar()).isZero();

        verify(transaccion, never()).ejecutar(any());
        verify(aviso, never()).avisar(any(), any());
    }

    // Sin identidad no hay token de proceso para preguntar a Académico: la revisión no puede
    // esperar al día siguiente (escenario 8, SC-004). Sigue como con el periodo abierto, y el aviso
    // queda guardado para entregarse cuando identidad vuelva.
    @Test
    void dadoAcademicoSinResponder_cuandoSeRevisa_entoncesRegistraYAvisaComoConElPeriodoAbierto() {
        enCurso("2026-I", enConsolidacion("2026-I"));
        when(datos.estado("2026-I")).thenThrow(new IllegalStateException("Académico no responde"));

        assertThat(servicio(VENCIDO).revisar()).isEqualTo(1);

        verify(plazos).registrar(any());
        verify(aviso).avisar(any(), any());
    }

    @Test
    void dadaUnaEtapaQueCambioEntreMedio_cuandoSeBloquea_entoncesNoRegistraNada() {
        ProcesoAprobacion antes = enConsolidacion("2026-I");
        ProcesoAprobacion despues = enRevision("2026-I");
        when(procesos.enCurso()).thenReturn(List.of("2026-I"));
        when(procesos.de("2026-I")).thenReturn(Optional.of(antes));
        when(procesos.bloquear("2026-I")).thenReturn(Optional.of(despues));

        assertThat(servicio(VENCIDO).revisar()).isZero();

        verify(plazos, never()).registrar(any());
        verify(aviso, never()).avisar(any(), any());
    }

    @Test
    void dadoUnVencimientoYaRegistrado_cuandoSeRevisaOtraVez_entoncesNoAvisaNiAnota() {
        enCurso("2026-I", enConsolidacion("2026-I"));
        when(plazos.registrar(any())).thenReturn(false);

        assertThat(servicio(VENCIDO).revisar()).isZero();

        verify(aviso, never()).avisar(any(), any());
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadoUnPeriodoQueFalla_cuandoSeRevisa_entoncesSigueConLosDemas() {
        enCurso("2026-I", enConsolidacion("2026-I"));
        enCurso("2026-II", enConsolidacion("2026-II"));
        when(procesos.enCurso()).thenReturn(List.of("2026-I", "2026-II"));
        when(procesos.bloquear("2026-I"))
                .thenThrow(new IllegalStateException("La base no responde"));

        assertThat(servicio(VENCIDO).revisar()).isEqualTo(1);

        verify(aviso).avisar(any(), any());
    }

    @Test
    void dadoUnPeriodoSinProceso_cuandoSeRevisa_entoncesNoHaceNada() {
        when(procesos.enCurso()).thenReturn(List.of("2026-I"));
        when(procesos.de("2026-I")).thenReturn(Optional.empty());

        assertThat(servicio(VENCIDO).revisar()).isZero();

        verify(transaccion, never()).ejecutar(any());
    }

    private RevisarPlazosServicio servicio(Instant ahora) {
        return new RevisarPlazosServicio(
                procesos,
                new EtapasAlCrearse(configuraciones, new LectorYaml(), new PlantillaEnRecursos()),
                datos,
                plazos,
                aviso,
                registro,
                transaccion,
                Clock.fixed(ahora, ZoneOffset.UTC));
    }

    private void enCurso(String periodo, ProcesoAprobacion proceso) {
        when(procesos.enCurso()).thenReturn(List.of(periodo));
        when(procesos.de(periodo)).thenReturn(Optional.of(proceso));
        when(procesos.bloquear(periodo)).thenReturn(Optional.of(proceso));
    }

    private static ProcesoAprobacion enConsolidacion(String periodo) {
        return en(periodo, TipoDeEtapa.EN_CONSOLIDACION);
    }

    private static ProcesoAprobacion enRevision(String periodo) {
        return en(periodo, TipoDeEtapa.EN_REVISION_VRA);
    }

    private static ProcesoAprobacion en(String periodo, TipoDeEtapa etapa) {
        EtapasAlCrearse etapas =
                new EtapasAlCrearse(
                        mock(RepositorioConfiguracion.class),
                        new LectorYaml(),
                        new PlantillaEnRecursos());
        return ProcesoAprobacion.de(
                periodo,
                etapas.de(periodo, CREADO),
                etapa,
                CREADO,
                Map.of("DS", EstadoDeEnvio.ENVIADO),
                List.of(
                        new Transicion(
                                TipoDeTransicion.PASO_DE_ETAPA,
                                TipoDeEtapa.BORRADOR_CARRERA,
                                etapa,
                                Optional.empty(),
                                1,
                                HASH,
                                "coordinacion.demo",
                                LLEGADA)));
    }
}
