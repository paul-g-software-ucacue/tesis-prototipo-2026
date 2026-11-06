package ec.mdd.validacionocs.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ec.mdd.validacionocs.adaptadores.salida.LectorYaml;
import ec.mdd.validacionocs.adaptadores.salida.PlantillaEnRecursos;
import ec.mdd.validacionocs.dominio.ArchivoDemasiadoGrande;
import ec.mdd.validacionocs.dominio.ArchivoIlegible;
import ec.mdd.validacionocs.dominio.CargaGuardada;
import ec.mdd.validacionocs.dominio.ConfiguracionFlujo;
import ec.mdd.validacionocs.dominio.EstadoDelPeriodo;
import ec.mdd.validacionocs.dominio.Firmantes;
import ec.mdd.validacionocs.puertos.entrada.ConfiguracionInvalida;
import ec.mdd.validacionocs.puertos.entrada.ConfiguracionVigente;
import ec.mdd.validacionocs.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.validacionocs.puertos.entrada.PeriodoSinPlanificacion;
import ec.mdd.validacionocs.puertos.salida.CambioDeFlujo;
import ec.mdd.validacionocs.puertos.salida.DatosDelPeriodo;
import ec.mdd.validacionocs.puertos.salida.RegistroDeCambios;
import ec.mdd.validacionocs.puertos.salida.RepositorioConfiguracion;
import ec.mdd.validacionocs.puertos.salida.Transaccion;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

/**
 * Cargar valida todo antes de guardar, y un rechazo no cambia lo que rige (FR-016 a FR-019). Usa el
 * lector y la plantilla de verdad: el archivo es la entrada de este caso de uso.
 */
class CargarConfiguracionServicioTest {

    private static final String PERIODO = "2026-I";
    private static final String QUIEN = "admin.demo";
    private static final Instant AHORA = Instant.parse("2026-11-02T15:00:00Z");
    private static final String PLANTILLA = new PlantillaEnRecursos().texto();
    private static final Firmantes TODOS = new Firmantes(Set.of("VICERRECTOR", "RECTOR"), true);

    private final DatosDelPeriodo datos = mock(DatosDelPeriodo.class);
    private final RepositorioConfiguracion repositorio = mock(RepositorioConfiguracion.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
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
    private final CargarConfiguracionServicio servicio =
            new CargarConfiguracionServicio(
                    datos,
                    new LectorYaml(),
                    repositorio,
                    registro,
                    transaccion,
                    Clock.fixed(AHORA, ZoneOffset.UTC));

    @BeforeEach
    void preparar() {
        when(datos.estado(PERIODO)).thenReturn(Optional.of(new EstadoDelPeriodo("PLANIFICACION")));
        when(datos.firmantes(PERIODO)).thenReturn(TODOS);
    }

    private static byte[] bytes(String texto) {
        return texto.getBytes(StandardCharsets.UTF_8);
    }

    private ConfiguracionVigente cargar(String texto) {
        return servicio.cargar(PERIODO, bytes(texto), QUIEN);
    }

    private void assertNoGuardo() {
        verify(repositorio, never()).guardar(anyString(), anyString(), anyString(), any());
    }

    @Test
    void dadaLaPlantilla_cuandoSeCarga_entoncesGuardaElTextoConQuienYCuando() {
        when(repositorio.vigente(PERIODO))
                .thenReturn(Optional.of(new CargaGuardada(1, PLANTILLA, QUIEN, AHORA)));

        ConfiguracionVigente vigente = cargar(PLANTILLA);

        verify(repositorio).guardar(PERIODO, PLANTILLA, QUIEN, AHORA);
        assertThat(vigente.periodo()).isEqualTo(PERIODO);
        assertThat(vigente.modificable()).isTrue();
        assertThat(vigente.carga().orElseThrow().numero()).isEqualTo(1);
        assertThat(vigente.configuracion().etapas()).hasSize(9);
        assertThat(vigente.configuracion().firmas()).hasSize(3);
    }

    @Test
    void laPrimeraCargaDelPeriodo_cuandoSeGuarda_entoncesLaBitacoraSoloLlevaLaVersionNueva() {
        when(repositorio.vigente(PERIODO))
                .thenReturn(
                        Optional.empty(),
                        Optional.of(new CargaGuardada(1, PLANTILLA, QUIEN, AHORA)));
        doAnswer(invocacion -> assertThat(dentro).as("en la transacción").isTrue())
                .when(registro)
                .registrar(any());

        cargar(PLANTILLA);

        ArgumentCaptor<CambioDeFlujo> cambio = ArgumentCaptor.forClass(CambioDeFlujo.class);
        verify(registro).registrar(cambio.capture());
        assertThat(cambio.getValue().accion())
                .isEqualTo(CambioDeFlujo.Accion.CONFIGURACION_FLUJO_CARGADA);
        assertThat(cambio.getValue().usuario()).isEqualTo(QUIEN);
        assertThat(cambio.getValue().momento()).isEqualTo(AHORA);
        assertThat(cambio.getValue().detalle()).isEqualTo(Map.of("periodo", PERIODO));
        assertThat(cambio.getValue().anterior()).isEmpty();
        assertThat(cambio.getValue().nuevo()).isEqualTo(Map.of("version", 1));
    }

    @Test
    void otraCargaDelPeriodo_cuandoSeGuarda_entoncesLaBitacoraLlevaLaVersionAnteriorYLaNueva() {
        when(repositorio.vigente(PERIODO))
                .thenReturn(
                        Optional.of(new CargaGuardada(1, PLANTILLA, "otro.demo", AHORA)),
                        Optional.of(new CargaGuardada(2, PLANTILLA, QUIEN, AHORA)));

        ConfiguracionVigente vigente = cargar(PLANTILLA);

        assertThat(vigente.carga().orElseThrow().numero()).isEqualTo(2);
        ArgumentCaptor<CambioDeFlujo> cambio = ArgumentCaptor.forClass(CambioDeFlujo.class);
        verify(registro).registrar(cambio.capture());
        assertThat(cambio.getValue().anterior()).isEqualTo(Map.of("version", 1));
        assertThat(cambio.getValue().nuevo()).isEqualTo(Map.of("version", 2));
    }

    @Test
    void unArchivoRechazado_cuandoSeCarga_entoncesNoLlegaALaBitacora() {
        assertThatThrownBy(() -> cargar("etapas: [")).isInstanceOf(RuntimeException.class);

        verifyNoInteractions(registro);
        assertNoGuardo();
    }

    @Test
    void dadoUnArchivoConBom_cuandoSeCarga_entoncesLoLeeYGuardaSinEl() {
        when(repositorio.vigente(PERIODO))
                .thenReturn(Optional.of(new CargaGuardada(1, PLANTILLA, QUIEN, AHORA)));

        cargar("﻿" + PLANTILLA);

        verify(repositorio).guardar(PERIODO, PLANTILLA, QUIEN, AHORA);
    }

    @Test
    void dadoUnArchivoSinEnOcs_cuandoSeCarga_entoncesSeRechazaDiciendoLaEtapaYNadaCambia() {
        String sinOcs = PLANTILLA.replace("  EN_OCS:\n", "  OTRA_COSA:\n");

        assertThatThrownBy(() -> cargar(sinOcs))
                .isInstanceOfSatisfying(
                        ConfiguracionInvalida.class,
                        error ->
                                assertThat(error.problemas())
                                        .contains(
                                                "Falta la etapa EN_OCS.",
                                                "La etapa OTRA_COSA no existe."));
        assertNoGuardo();
    }

    @Test
    void dadoUnRolFirmanteSinUsuario_cuandoSeCarga_entoncesSeRechazaDiciendoCual() {
        when(datos.firmantes(PERIODO)).thenReturn(new Firmantes(Set.of("VICERRECTOR"), true));

        assertThatThrownBy(() -> cargar(PLANTILLA))
                .isInstanceOfSatisfying(
                        ConfiguracionInvalida.class,
                        error ->
                                assertThat(error.problemas())
                                        .containsExactly("El rol RECTOR no tiene usuarios."));
        assertNoGuardo();
    }

    @Test
    void sinResponsableDesignado_cuandoSeCarga_entoncesSeRechazaDiciendolo() {
        when(datos.firmantes(PERIODO))
                .thenReturn(new Firmantes(Set.of("VICERRECTOR", "RECTOR"), false));

        assertThatThrownBy(() -> cargar(PLANTILLA))
                .isInstanceOfSatisfying(
                        ConfiguracionInvalida.class,
                        error ->
                                assertThat(error.problemas())
                                        .containsExactly(
                                                "Falta designar al Responsable de consolidación"
                                                        + " del periodo."));
        assertNoGuardo();
    }

    @Test
    void dadosProblemasDelArchivoYDeLosFirmantes_cuandoSeCarga_entoncesLosDiceJuntos() {
        when(datos.firmantes(PERIODO)).thenReturn(new Firmantes(Set.of("VICERRECTOR"), false));
        String malo = "otra: 1\n" + PLANTILLA.replace("plazo_dias: 15", "plazo_dias: 0");

        assertThatThrownBy(() -> cargar(malo))
                .isInstanceOfSatisfying(
                        ConfiguracionInvalida.class,
                        error ->
                                assertThat(error.problemas())
                                        .contains(
                                                "Línea 1: la clave otra no existe.",
                                                "El plazo de la etapa BORRADOR_CARRERA debe ser un"
                                                        + " número entero de días, mayor que 0.",
                                                "El rol RECTOR no tiene usuarios.",
                                                "Falta designar al Responsable de consolidación"
                                                        + " del periodo."));
        assertNoGuardo();
    }

    @Test
    void dadoUnArchivoDeUnMega_cuandoSeCarga_entoncesPasaElLimite() {
        byte[] exacto = new byte[ConfiguracionFlujo.TAMANO_MAXIMO];
        java.util.Arrays.fill(exacto, (byte) ' ');

        assertThatThrownBy(() -> servicio.cargar(PERIODO, exacto, QUIEN))
                .isInstanceOf(ArchivoIlegible.class);
    }

    @Test
    void dadoUnArchivoDeMasDeUnMega_cuandoSeCarga_entoncesSeRechazaSinPreguntarANadie() {
        byte[] grande = new byte[ConfiguracionFlujo.TAMANO_MAXIMO + 1];

        assertThatThrownBy(() -> servicio.cargar(PERIODO, grande, QUIEN))
                .isInstanceOf(ArchivoDemasiadoGrande.class);
        verifyNoInteractions(datos, repositorio);
    }

    @Test
    void dadoUnArchivoVacio_cuandoSeCarga_entoncesEsIlegible() {
        assertThatThrownBy(() -> cargar("")).isInstanceOf(ArchivoIlegible.class);
        assertNoGuardo();
    }

    @Test
    void dadoUnArchivoQueNoEsTexto_cuandoSeCarga_entoncesEsIlegible() {
        byte[] binario = {(byte) 0xFF, (byte) 0xFE, 0, 1, 2};

        assertThatThrownBy(() -> servicio.cargar(PERIODO, binario, QUIEN))
                .isInstanceOf(ArchivoIlegible.class);
        assertNoGuardo();
    }

    @ParameterizedTest
    @ValueSource(strings = {"EN_APROBACION", "VIGENTE", "CERRADO"})
    void dadoUnPeriodoFueraDePlanificacion_cuandoSeCarga_entoncesSeRechaza(String estado) {
        when(datos.estado(PERIODO)).thenReturn(Optional.of(new EstadoDelPeriodo(estado)));

        assertThatThrownBy(() -> cargar(PLANTILLA)).isInstanceOf(PeriodoSinPlanificacion.class);
        assertNoGuardo();
    }

    @Test
    void dadoUnPeriodoQueNoExiste_cuandoSeCarga_entoncesSeRechaza() {
        when(datos.estado(PERIODO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cargar(PLANTILLA)).isInstanceOf(PeriodoNoEncontrado.class);
        assertNoGuardo();
    }

    @Test
    void dadoQueRolesNoResponde_cuandoSeCarga_entoncesPropagaElErrorSinGuardar() {
        doThrow(new IllegalStateException("sin respuesta")).when(datos).firmantes(PERIODO);

        assertThatThrownBy(() -> cargar(PLANTILLA)).isInstanceOf(IllegalStateException.class);
        assertNoGuardo();
    }
}
