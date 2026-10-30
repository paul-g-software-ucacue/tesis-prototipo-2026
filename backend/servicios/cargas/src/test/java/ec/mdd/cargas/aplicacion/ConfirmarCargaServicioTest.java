package ec.mdd.cargas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ec.mdd.cargas.adaptadores.salida.DiccionarioYaml;
import ec.mdd.cargas.dominio.CargaQueFalta;
import ec.mdd.cargas.dominio.EntradaDeHistorial;
import ec.mdd.cargas.dominio.MotivoDeRechazo;
import ec.mdd.cargas.dominio.ReferenciasExistentes;
import ec.mdd.cargas.dominio.ResultadoDeHistorial;
import ec.mdd.cargas.dominio.TablaDeDatos;
import ec.mdd.cargas.puertos.entrada.ArchivoSubido;
import ec.mdd.cargas.puertos.entrada.CargaConErrores;
import ec.mdd.cargas.puertos.entrada.CargasAnterioresPendientes;
import ec.mdd.cargas.puertos.entrada.Confirmacion;
import ec.mdd.cargas.puertos.salida.ArchivoLeido;
import ec.mdd.cargas.puertos.salida.Formato;
import ec.mdd.cargas.puertos.salida.FuenteDeDatos;
import ec.mdd.cargas.puertos.salida.FuenteDelDiccionario;
import ec.mdd.cargas.puertos.salida.HistorialDeCargas;
import ec.mdd.cargas.puertos.salida.ReferenciasDelSistema;
import ec.mdd.cargas.puertos.salida.Transaccion;
import ec.mdd.plataforma.cargas.AplicadorDeCargas;
import ec.mdd.plataforma.cargas.LoteDeCarga;
import ec.mdd.plataforma.cargas.MotorDelLote;
import ec.mdd.plataforma.cargas.RechazoDelLote;
import ec.mdd.plataforma.cargas.ResultadoDelLote;
import ec.mdd.plataforma.cargas.VistaPreviaDesactualizada;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Confirmar una carga (RF-10, MD-13, FR-003 a FR-008), con Mockito. */
class ConfirmarCargaServicioTest {

    private static final Instant AHORA = Instant.parse("2026-10-26T15:00:00Z");
    private static final String HUELLA = "d".repeat(64);
    private static final byte[] CONTENIDO = "ciclos".getBytes(StandardCharsets.UTF_8);
    private static final String HASH = MotorDelLote.sha256(CONTENIDO);

    private final FuenteDeDatos lector = mock(FuenteDeDatos.class);
    private final FuenteDelDiccionario fuente = mock(FuenteDelDiccionario.class);
    private final ReferenciasDelSistema referencias = mock(ReferenciasDelSistema.class);
    private final AplicadorDeCargas dueno = mock(AplicadorDeCargas.class);
    private final HistorialDeCargas historial = mock(HistorialDeCargas.class);
    private final Transaccion transaccion =
            new Transaccion() {
                @Override
                public <T> T ejecutar(Supplier<T> trabajo) {
                    return trabajo.get();
                }
            };
    private final ConfirmarCargaServicio servicio;

    ConfirmarCargaServicioTest() {
        when(fuente.leer()).thenReturn(new DiccionarioYaml().leer());
        when(referencias.existentes())
                .thenReturn(
                        new ReferenciasExistentes(
                                Set.of(), Set.of(), Set.of(), Set.of(), Set.of()));
        when(dueno.cargas()).thenReturn(ValidarArchivoServicioTest.CARGAS);
        when(dueno.tieneDatos(any())).thenReturn(true);
        DuenosDeLasCargas duenos =
                new DuenosDeLasCargas(List.of(dueno), ValidarArchivoServicioTest.CARGAS);
        ValidarArchivoServicio validar =
                new ValidarArchivoServicio(
                        new ConsultarPlantillasServicio(fuente), lector, referencias, duenos);
        servicio =
                new ConfirmarCargaServicio(
                        validar,
                        duenos,
                        historial,
                        transaccion,
                        Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private void conLaFila(List<String> fila) {
        when(lector.leer(eq("03_ciclos_academicos.csv"), any()))
                .thenReturn(
                        new ArchivoLeido(
                                Formato.CSV,
                                new TablaDeDatos(
                                        ValidarArchivoServicioTest.CICLOS, List.of(fila))));
    }

    private Confirmacion confirmar() {
        return servicio.confirmar(
                "ciclos_academicos",
                new ArchivoSubido("03_ciclos_academicos.csv", CONTENIDO),
                HUELLA,
                "datos.demo");
    }

    private EntradaDeHistorial registrada() {
        ArgumentCaptor<EntradaDeHistorial> entrada =
                ArgumentCaptor.forClass(EntradaDeHistorial.class);
        verify(historial).registrar(entrada.capture());
        return entrada.getValue();
    }

    @Test
    void conUnaCargaAnteriorSinDatos_noLeeElArchivoNiLlamaAlDuenoNiEscribeElHistorial() {
        when(dueno.tieneDatos("docentes")).thenReturn(false);

        assertThatThrownBy(
                        () ->
                                servicio.confirmar(
                                        "cargos_autoridades",
                                        new ArchivoSubido("08.csv", CONTENIDO),
                                        HUELLA,
                                        "datos.demo"))
                .isInstanceOfSatisfying(
                        CargasAnterioresPendientes.class,
                        error ->
                                assertThat(error.faltan())
                                        .extracting(CargaQueFalta::codigo)
                                        .containsExactly("docentes"));
        verifyNoInteractions(lector, historial);
        verify(dueno, never()).aplicar(any(), any(), any());
    }

    @Test
    void dadoUnArchivoValido_entoncesElDuenoLoAplicaYQuedaEnElHistorial() {
        conLaFila(ValidarArchivoServicioTest.PERIODO);
        when(dueno.aplicar(any(), eq(HUELLA), eq("datos.demo")))
                .thenReturn(new ResultadoDelLote(1, 0, 2));

        Confirmacion confirmacion = confirmar();

        assertThat(confirmacion)
                .isEqualTo(new Confirmacion(ResultadoDeHistorial.APLICADA, 1, 0, 2, AHORA));
        ArgumentCaptor<LoteDeCarga> lote = ArgumentCaptor.forClass(LoteDeCarga.class);
        verify(dueno).aplicar(lote.capture(), eq(HUELLA), eq("datos.demo"));
        assertThat(lote.getValue().hash()).isEqualTo(HASH);
        assertThat(lote.getValue().filas()).hasSize(1);
        assertThat(registrada())
                .isEqualTo(
                        new EntradaDeHistorial(
                                "ciclos_academicos",
                                "datos.demo",
                                AHORA,
                                "03_ciclos_academicos.csv",
                                HASH,
                                ResultadoDeHistorial.APLICADA,
                                1,
                                0,
                                2,
                                Optional.empty()));
    }

    @Test
    void dadoUnArchivoConErrores_entoncesNoLlamaAlDuenoYQuedaRechazado() {
        conLaFila(
                List.of(
                        "2026-III",
                        "PRIMER PERIODO",
                        "2026-02-30",
                        "2026-08-31",
                        "20",
                        "",
                        "",
                        "APROBADO"));

        assertThatThrownBy(this::confirmar)
                .isInstanceOfSatisfying(
                        CargaConErrores.class, error -> assertThat(error.filas()).isOne());
        verify(dueno, never()).aplicar(any(), anyString(), anyString());
        assertThat(registrada().motivo()).contains(MotivoDeRechazo.CON_ERRORES);
    }

    @Test
    void dadoElMismoArchivoYaAplicado_entoncesRepetidaSinLlamarAlDueno() {
        conLaFila(ValidarArchivoServicioTest.PERIODO);
        Instant primera = Instant.parse("2026-10-20T09:00:00Z");
        when(historial.aplicada("ciclos_academicos", HASH))
                .thenReturn(
                        Optional.of(
                                new EntradaDeHistorial(
                                        "ciclos_academicos",
                                        "consolidacion.demo",
                                        primera,
                                        "ciclos.csv",
                                        HASH,
                                        ResultadoDeHistorial.APLICADA,
                                        2,
                                        0,
                                        0,
                                        Optional.empty())));

        Confirmacion confirmacion = confirmar();

        assertThat(confirmacion)
                .isEqualTo(new Confirmacion(ResultadoDeHistorial.REPETIDA, 0, 0, 0, primera));
        verify(dueno, never()).aplicar(any(), anyString(), anyString());
        assertThat(registrada().resultado()).isEqualTo(ResultadoDeHistorial.REPETIDA);
    }

    @Test
    void siElDuenoRechazaUnaFilaOLaVistaCambio_entoncesQuedaRechazadoConSuMotivo() {
        conLaFila(ValidarArchivoServicioTest.PERIODO);
        when(dueno.aplicar(any(), anyString(), anyString()))
                .thenThrow(new RechazoDelLote(2, new IllegalStateException("regla")));

        assertThatThrownBy(this::confirmar).isInstanceOf(RechazoDelLote.class);
        assertThat(registrada().motivo()).contains(MotivoDeRechazo.RECHAZADA_POR_EL_DUENO);
    }

    @Test
    void siLaVistaPreviaCambio_entoncesQuedaRechazadaComoDesactualizada() {
        conLaFila(ValidarArchivoServicioTest.PERIODO);
        when(dueno.aplicar(any(), anyString(), anyString()))
                .thenThrow(new VistaPreviaDesactualizada());

        assertThatThrownBy(this::confirmar).isInstanceOf(VistaPreviaDesactualizada.class);
        assertThat(registrada().motivo()).contains(MotivoDeRechazo.VISTA_PREVIA_DESACTUALIZADA);
    }

    @Test
    void elHistorialDeUnaCarga() {
        ConsultarHistorialServicio consultar =
                new ConsultarHistorialServicio(new ConsultarPlantillasServicio(fuente), historial);
        when(historial.deLaCarga("carreras")).thenReturn(List.of());

        assertThat(consultar.historial("carreras")).isEmpty();
        assertThatThrownBy(() -> consultar.historial("comisiones"))
                .isInstanceOf(ec.mdd.cargas.puertos.entrada.CargaNoEncontrada.class);
    }
}
