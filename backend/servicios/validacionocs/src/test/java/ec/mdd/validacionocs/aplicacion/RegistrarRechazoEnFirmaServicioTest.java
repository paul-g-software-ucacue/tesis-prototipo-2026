package ec.mdd.validacionocs.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.validacionocs.dominio.Observacion;
import ec.mdd.validacionocs.dominio.ProcesoAprobacion;
import ec.mdd.validacionocs.dominio.RechazoDelFirmante;
import ec.mdd.validacionocs.dominio.TipoDeEtapa;
import ec.mdd.validacionocs.dominio.TipoDeTransicion;
import ec.mdd.validacionocs.dominio.Transicion;
import ec.mdd.validacionocs.puertos.salida.AvisoDeFirma;
import ec.mdd.validacionocs.puertos.salida.AvisoDeFirma.FirmasPorAnular;
import ec.mdd.validacionocs.puertos.salida.CambioDeFlujo;
import ec.mdd.validacionocs.puertos.salida.Distributivo;
import ec.mdd.validacionocs.puertos.salida.Distributivo.VersionNuevaEnAprobacion;
import ec.mdd.validacionocs.puertos.salida.RegistroDeCambios;
import ec.mdd.validacionocs.puertos.salida.RepositorioProceso;
import ec.mdd.validacionocs.puertos.salida.Transaccion;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * El rechazo en firma devuelve el distributivo a revisión (F14-H04, D-105): con el proceso
 * bloqueado, guarda la transición con su rechazo y la registra en la bitácora; un aviso repetido o
 * fuera de lugar no cambia nada ni falla.
 */
class RegistrarRechazoEnFirmaServicioTest {

    private static final Instant RECHAZADA = Instant.parse("2026-11-15T12:00:00Z");
    private static final String HASH_NUEVA = "e".repeat(64);
    private static final RechazoDelFirmante RECHAZO =
            new RechazoDelFirmante(
                    2,
                    "VICERRECTOR",
                    "vicerrectorado.demo",
                    RECHAZADA,
                    new Observacion("Falta la carga de un docente de Software."));
    private static final Transicion A_REVISION =
            new Transicion(
                    TipoDeTransicion.RECHAZO_EN_FIRMA,
                    TipoDeEtapa.CONGELADO_EN_FIRMA,
                    TipoDeEtapa.EN_REVISION_VRA,
                    Optional.empty(),
                    1,
                    "d".repeat(64),
                    "sistema",
                    RECHAZADA,
                    Map.of(),
                    Optional.of(RECHAZO));
    private static final Transicion VERSION_NUEVA =
            new Transicion(
                    TipoDeTransicion.VERSION_NUEVA,
                    TipoDeEtapa.EN_REVISION_VRA,
                    TipoDeEtapa.EN_REVISION_VRA,
                    Optional.empty(),
                    2,
                    HASH_NUEVA,
                    "sistema",
                    RECHAZADA);

    private final RepositorioProceso procesos = mock(RepositorioProceso.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
    private final Distributivo distributivo = mock(Distributivo.class);
    private final AvisoDeFirma aviso = mock(AvisoDeFirma.class);
    private final ProcesoAprobacion leido = mock(ProcesoAprobacion.class);
    private final ProcesoAprobacion proceso = mock(ProcesoAprobacion.class);
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

    private final RegistrarRechazoEnFirmaServicio servicio =
            new RegistrarRechazoEnFirmaServicio(
                    procesos, distributivo, aviso, registro, transaccion);

    @Test
    void
            dadoElProcesoEnFirma_cuandoSeRechaza_entoncesOrdenaLaVersionNuevaYGuardaLasDosTransiciones() {
        when(procesos.de("2026-I")).thenReturn(Optional.of(leido));
        when(leido.admiteRechazoEnFirma(1)).thenReturn(true);
        when(distributivo.crearVersionNueva("2026-I", 1, RECHAZADA))
                .thenAnswer(
                        invocacion -> {
                            assertThat(dentro).as("Generación, en su transacción").isFalse();
                            return Optional.of(new VersionNuevaEnAprobacion(2, HASH_NUEVA));
                        });
        when(procesos.bloquear("2026-I"))
                .thenAnswer(
                        invocacion -> {
                            assertThat(dentro).isTrue();
                            return Optional.of(proceso);
                        });
        when(proceso.rechazarEnFirma(1, RECHAZO, RECHAZADA)).thenReturn(List.of(A_REVISION));
        when(proceso.registrarVersionNueva(1, 2, HASH_NUEVA, RECHAZADA))
                .thenReturn(List.of(VERSION_NUEVA));
        org.mockito.Mockito.doAnswer(
                        invocacion -> {
                            assertThat(dentro).as("el aviso, con las transiciones").isTrue();
                            return null;
                        })
                .when(aviso)
                .avisarAnulacion(any(FirmasPorAnular.class));

        servicio.registrar("2026-I", 1, RECHAZO);

        verify(procesos).guardar(proceso, List.of(A_REVISION, VERSION_NUEVA));
        verify(aviso).avisarAnulacion(new FirmasPorAnular("2026-I", 1, RECHAZADA));
        ArgumentCaptor<CambioDeFlujo> cambio = ArgumentCaptor.forClass(CambioDeFlujo.class);
        verify(registro, org.mockito.Mockito.times(2)).registrar(cambio.capture());
        CambioDeFlujo rechazo = cambio.getAllValues().get(0);
        assertThat(rechazo.accion())
                .isEqualTo(CambioDeFlujo.Accion.DISTRIBUTIVO_RECHAZADO_EN_FIRMA);
        assertThat(rechazo.usuario()).isEqualTo("sistema");
        assertThat(rechazo.detalle())
                .containsEntry("periodo", "2026-I")
                .containsEntry("version", 1)
                .containsEntry("orden", 2)
                .containsEntry("rol", "VICERRECTOR")
                .containsEntry("rechazadoPor", "vicerrectorado.demo")
                .containsEntry("observacion", "Falta la carga de un docente de Software.");
        assertThat(rechazo.anterior()).containsEntry("estado", "CONGELADO_EN_FIRMA");
        assertThat(rechazo.nuevo()).containsEntry("estado", "EN_REVISION_VRA");
        CambioDeFlujo nueva = cambio.getAllValues().get(1);
        assertThat(nueva.accion()).isEqualTo(CambioDeFlujo.Accion.DISTRIBUTIVO_VERSION_NUEVA);
        assertThat(nueva.usuario()).isEqualTo("sistema");
        assertThat(nueva.detalle())
                .containsEntry("periodo", "2026-I")
                .containsEntry("version", 2)
                .containsEntry("versionAnulada", 1)
                .containsEntry("motivo", "RECHAZO_EN_FIRMA");
        assertThat(nueva.anterior()).containsEntry("version", 1);
        assertThat(nueva.nuevo()).containsEntry("version", 2);
    }

    @Test
    void dadoElAvisoRepetidoOFueraDeLugar_cuandoSeRechaza_entoncesNoLlamaAGeneracionNiGuarda() {
        when(procesos.de("2026-I")).thenReturn(Optional.of(leido));
        when(leido.admiteRechazoEnFirma(1)).thenReturn(false);

        servicio.registrar("2026-I", 1, RECHAZO);

        verify(distributivo, never())
                .crearVersionNueva(any(), org.mockito.ArgumentMatchers.anyInt(), any());
        verify(procesos, never()).bloquear(any());
        verify(registro, never()).registrar(any());
        verify(aviso, never()).avisarAnulacion(any(FirmasPorAnular.class));
    }

    @Test
    void dadoQueGeneracionNoCreaLaVersion_cuandoSeRechaza_entoncesNoGuardaNada() {
        when(procesos.de("2026-I")).thenReturn(Optional.of(leido));
        when(leido.admiteRechazoEnFirma(1)).thenReturn(true);
        when(distributivo.crearVersionNueva("2026-I", 1, RECHAZADA)).thenReturn(Optional.empty());

        servicio.registrar("2026-I", 1, RECHAZO);

        verify(procesos, never()).bloquear(any());
        verify(registro, never()).registrar(any());
        verify(aviso, never()).avisarAnulacion(any(FirmasPorAnular.class));
    }

    @Test
    void dadoQueOtroAvisoGanoMientrasTanto_cuandoSeBloquea_entoncesNoGuardaNada() {
        when(procesos.de("2026-I")).thenReturn(Optional.of(leido));
        when(leido.admiteRechazoEnFirma(1)).thenReturn(true);
        when(distributivo.crearVersionNueva("2026-I", 1, RECHAZADA))
                .thenReturn(Optional.of(new VersionNuevaEnAprobacion(2, HASH_NUEVA)));
        when(procesos.bloquear("2026-I")).thenReturn(Optional.of(proceso));
        when(proceso.rechazarEnFirma(1, RECHAZO, RECHAZADA)).thenReturn(List.of());

        servicio.registrar("2026-I", 1, RECHAZO);

        verify(procesos, never()).guardar(any(), anyList());
        verify(registro, never()).registrar(any());
        verify(aviso, never()).avisarAnulacion(any(FirmasPorAnular.class));
    }

    @Test
    void dadoUnPeriodoSinProceso_cuandoSeRechaza_entoncesNoFalla() {
        when(procesos.de("2026-I")).thenReturn(Optional.empty());

        servicio.registrar("2026-I", 1, RECHAZO);

        verify(registro, never()).registrar(any());
        verify(distributivo, never())
                .crearVersionNueva(any(), org.mockito.ArgumentMatchers.anyInt(), any());
    }
}
