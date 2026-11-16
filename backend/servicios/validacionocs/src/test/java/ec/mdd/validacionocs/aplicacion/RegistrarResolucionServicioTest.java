package ec.mdd.validacionocs.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.validacionocs.dominio.ArchivoDeResolucion;
import ec.mdd.validacionocs.dominio.EstadoDelPeriodo;
import ec.mdd.validacionocs.dominio.EtapaNoAdmiteResolucion;
import ec.mdd.validacionocs.dominio.NumeroDeResolucionInvalido;
import ec.mdd.validacionocs.dominio.ProcesoAprobacion;
import ec.mdd.validacionocs.dominio.ResolucionNoEsPdf;
import ec.mdd.validacionocs.dominio.ResolucionOcs;
import ec.mdd.validacionocs.dominio.Solicitante;
import ec.mdd.validacionocs.dominio.TipoDeEtapa;
import ec.mdd.validacionocs.dominio.TipoDeTransicion;
import ec.mdd.validacionocs.dominio.Transicion;
import ec.mdd.validacionocs.puertos.entrada.ConsultarFlujo;
import ec.mdd.validacionocs.puertos.entrada.FlujoDelDistributivo;
import ec.mdd.validacionocs.puertos.entrada.PeriodoCerrado;
import ec.mdd.validacionocs.puertos.entrada.SinDistributivo;
import ec.mdd.validacionocs.puertos.salida.AlmacenDeResoluciones;
import ec.mdd.validacionocs.puertos.salida.CambioDeFlujo;
import ec.mdd.validacionocs.puertos.salida.DatosDelPeriodo;
import ec.mdd.validacionocs.puertos.salida.Distributivo;
import ec.mdd.validacionocs.puertos.salida.RegistroDeCambios;
import ec.mdd.validacionocs.puertos.salida.RepositorioProceso;
import ec.mdd.validacionocs.puertos.salida.Transaccion;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Registrar la resolución del OCS (F15-H01, research §1, contracts/validacionocs.md). */
class RegistrarResolucionServicioTest {

    private static final Instant AHORA = Instant.parse("2026-11-15T15:00:00Z");
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 14);
    private static final byte[] PDF =
            "%PDF-1.7\nresolucion\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
    private static final String HUELLA = ArchivoDeResolucion.sha256(PDF);
    private static final String HASH = "d".repeat(64);
    private static final Solicitante SECRETARIA =
            new Solicitante("secretaria.demo", Set.of("SECRETARIA_GENERAL"), Optional.empty());
    private static final ResolucionOcs RESOLUCION =
            new ResolucionOcs("RPC-SO-12-No.123-2026", FECHA, HUELLA, "resolucion.pdf", PDF.length);
    private static final Transicion APROBADO =
            new Transicion(
                    TipoDeTransicion.PASO_DE_ETAPA,
                    TipoDeEtapa.EN_OCS,
                    TipoDeEtapa.APROBADO,
                    Optional.empty(),
                    2,
                    HASH,
                    "secretaria.demo",
                    AHORA,
                    Map.of(),
                    Optional.empty(),
                    Optional.of(RESOLUCION));

    private final RepositorioProceso procesos = mock(RepositorioProceso.class);
    private final Distributivo distributivo = mock(Distributivo.class);
    private final DatosDelPeriodo datos = mock(DatosDelPeriodo.class);
    private final AlmacenDeResoluciones almacen = mock(AlmacenDeResoluciones.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
    private final ConsultarFlujo flujo = mock(ConsultarFlujo.class);
    private final ProcesoAprobacion leido = mock(ProcesoAprobacion.class);
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

    private final RegistrarResolucionServicio servicio =
            new RegistrarResolucionServicio(
                    procesos,
                    distributivo,
                    datos,
                    almacen,
                    registro,
                    transaccion,
                    flujo,
                    Clock.fixed(AHORA, ZoneOffset.UTC));

    private void abierto() {
        when(datos.estado("2026-I")).thenReturn(Optional.of(new EstadoDelPeriodo("PLANIFICACION")));
        when(distributivo.actual("2026-I"))
                .thenReturn(
                        Optional.of(
                                new Distributivo.DistributivoActual(
                                        2, AHORA.minusSeconds(86_400 * 30), Map.of())));
        when(procesos.de("2026-I")).thenReturn(Optional.of(leido));
    }

    @Test
    void dadoElDistributivoEnOcs_cuandoSeRegistra_entoncesGuardaElArchivoYDespuesLaTransicion() {
        abierto();
        doAnswer(
                        invocacion -> {
                            assertThat(dentro).as("el archivo, fuera de la transacción").isFalse();
                            return null;
                        })
                .when(almacen)
                .guardar(HUELLA, PDF);
        when(procesos.bloquear("2026-I"))
                .thenAnswer(
                        invocacion -> {
                            assertThat(dentro).isTrue();
                            return Optional.of(bloqueado);
                        });
        when(bloqueado.registrarResolucion(SECRETARIA, RESOLUCION, AHORA))
                .thenReturn(List.of(APROBADO));
        when(flujo.flujo("2026-I", SECRETARIA)).thenReturn(vista);

        FlujoDelDistributivo resultado =
                servicio.registrar(
                        "2026-I",
                        "  RPC-SO-12-No.123-2026 ",
                        FECHA,
                        "C:\\resolucion.pdf",
                        PDF,
                        SECRETARIA);

        assertThat(resultado).isSameAs(vista);
        verify(leido).validarResolucion(SECRETARIA, RESOLUCION, AHORA);
        verify(almacen).guardar(HUELLA, PDF);
        verify(procesos).guardar(bloqueado, List.of(APROBADO));
        ArgumentCaptor<CambioDeFlujo> cambio = ArgumentCaptor.forClass(CambioDeFlujo.class);
        verify(registro).registrar(cambio.capture());
        assertThat(cambio.getValue().accion())
                .isEqualTo(CambioDeFlujo.Accion.RESOLUCION_REGISTRADA);
        assertThat(cambio.getValue().usuario()).isEqualTo("secretaria.demo");
        assertThat(cambio.getValue().detalle())
                .containsEntry("periodo", "2026-I")
                .containsEntry("version", 2)
                .containsEntry("hash", HASH)
                .containsEntry("numero", "RPC-SO-12-No.123-2026")
                .containsEntry("fecha", "2026-11-14")
                .containsEntry("sha256", HUELLA);
        assertThat(cambio.getValue().anterior()).containsEntry("estado", "EN_OCS");
        assertThat(cambio.getValue().nuevo()).containsEntry("estado", "APROBADO");
    }

    @Test
    void dadoUnArchivoQueNoEsPdfOUnNumeroInvalido_cuandoSeRegistra_entoncesNoGuardaNada() {
        abierto();

        assertThatThrownBy(
                        () ->
                                servicio.registrar(
                                        "2026-I", "N1", FECHA, "r.pdf", new byte[] {1}, SECRETARIA))
                .isInstanceOf(ResolucionNoEsPdf.class);
        assertThatThrownBy(
                        () -> servicio.registrar("2026-I", "   ", FECHA, "r.pdf", PDF, SECRETARIA))
                .isInstanceOf(NumeroDeResolucionInvalido.class);
        verify(almacen, never()).guardar(anyString(), any());
        verify(procesos, never()).bloquear(any());
    }

    @Test
    void dadoQueElAgregadoNoAdmiteLaResolucion_cuandoSeRegistra_entoncesNoGuardaElArchivo() {
        abierto();
        doThrow(new EtapaNoAdmiteResolucion())
                .when(leido)
                .validarResolucion(SECRETARIA, RESOLUCION, AHORA);

        assertThatThrownBy(
                        () ->
                                servicio.registrar(
                                        "2026-I",
                                        "RPC-SO-12-No.123-2026",
                                        FECHA,
                                        "resolucion.pdf",
                                        PDF,
                                        SECRETARIA))
                .isInstanceOf(EtapaNoAdmiteResolucion.class);
        verify(almacen, never()).guardar(anyString(), any());
        verify(procesos, never()).bloquear(any());
    }

    @Test
    void dadoQueOtroRegistroGanoMientrasTanto_cuandoSeBloquea_entoncesNoGuardaLaTransicion() {
        abierto();
        when(procesos.bloquear("2026-I")).thenReturn(Optional.of(bloqueado));
        when(bloqueado.registrarResolucion(SECRETARIA, RESOLUCION, AHORA))
                .thenThrow(new EtapaNoAdmiteResolucion());

        assertThatThrownBy(
                        () ->
                                servicio.registrar(
                                        "2026-I",
                                        "RPC-SO-12-No.123-2026",
                                        FECHA,
                                        "resolucion.pdf",
                                        PDF,
                                        SECRETARIA))
                .isInstanceOf(EtapaNoAdmiteResolucion.class);
        verify(procesos, never()).guardar(any(), anyList());
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadoElPeriodoCerradoOSinDistributivo_cuandoSeRegistra_entoncesSeRechazaSinGuardar() {
        when(datos.estado("2026-I")).thenReturn(Optional.of(new EstadoDelPeriodo("CERRADO")));
        assertThatThrownBy(
                        () -> servicio.registrar("2026-I", "N1", FECHA, "r.pdf", PDF, SECRETARIA))
                .isInstanceOf(PeriodoCerrado.class);

        when(datos.estado("2026-I")).thenReturn(Optional.of(new EstadoDelPeriodo("PLANIFICACION")));
        when(distributivo.actual("2026-I")).thenReturn(Optional.empty());
        assertThatThrownBy(
                        () -> servicio.registrar("2026-I", "N1", FECHA, "r.pdf", PDF, SECRETARIA))
                .isInstanceOf(SinDistributivo.class);
        verify(almacen, never()).guardar(anyString(), any());
    }
}
