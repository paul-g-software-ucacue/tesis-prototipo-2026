package ec.mdd.reporting.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.reporting.adaptadores.salida.LectorDeLayoutYaml;
import ec.mdd.reporting.dominio.ArchivoIlegible;
import ec.mdd.reporting.dominio.LayoutCargado;
import ec.mdd.reporting.dominio.LayoutExportacion;
import ec.mdd.reporting.dominio.LayoutInvalido;
import ec.mdd.reporting.puertos.entrada.GestionarLayoutSiga.LayoutsSiga;
import ec.mdd.reporting.puertos.salida.LayoutsGuardados;
import ec.mdd.reporting.puertos.salida.LayoutsGuardados.LayoutGuardado;
import ec.mdd.reporting.puertos.salida.RegistroEnBitacora;
import ec.mdd.reporting.puertos.salida.Transaccion;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Consultar y cargar el layout de exportación (F19-H01, FR-013 a FR-018, D-119): una carga válida
 * se guarda y se registra en la misma transacción; una inválida no deja nada.
 */
class GestionarLayoutSigaServicioTest {

    private static final Instant AHORA = Instant.parse("2026-11-22T15:00:00Z");
    private static final String VALIDO =
            "columnas:\n"
                    + "  - {nombre: ID, campo: identificacion}\n"
                    + "  - {nombre: C, campo: componente}\n"
                    + "  - {nombre: H, campo: horas}\n";

    private final LayoutsGuardados guardados = mock(LayoutsGuardados.class);
    private final RegistroEnBitacora registro = mock(RegistroEnBitacora.class);
    private boolean dentro;
    private boolean guardadoDentro;
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

    private final GestionarLayoutSigaServicio servicio =
            new GestionarLayoutSigaServicio(
                    new LayoutVigente(guardados, new LectorDeLayoutYaml()),
                    new LectorDeLayoutYaml(),
                    guardados,
                    registro,
                    transaccion,
                    Clock.fixed(AHORA, ZoneOffset.UTC));

    @BeforeEach
    void preparar() {
        when(guardados.porDefecto()).thenReturn(plantilla());
        when(guardados.todos()).thenReturn(List.of());
        when(guardados.vigente()).thenReturn(Optional.empty());
        when(guardados.guardar(anyString(), anyString(), anyString(), any()))
                .thenAnswer(
                        invocacion -> {
                            guardadoDentro = dentro;
                            return 4;
                        });
    }

    @Test
    void sinCargas_cuandoSeConsulta_entoncesRigeElPorDefectoSinAnteriores() {
        LayoutsSiga layouts = servicio.consultar();

        assertThat(layouts.vigente().esPorDefecto()).isTrue();
        assertThat(layouts.vigente().columnas()).hasSize(13);
        assertThat(layouts.anteriores()).isEmpty();
    }

    @Test
    void conCargas_cuandoSeConsulta_entoncesRigeLaUltimaYLasDemasSonAnteriores() {
        when(guardados.todos())
                .thenReturn(
                        List.of(
                                guardado(3, AHORA.plusSeconds(120)),
                                guardado(2, AHORA.plusSeconds(60)),
                                guardado(1, AHORA)));

        LayoutsSiga layouts = servicio.consultar();

        assertThat(layouts.vigente().numero()).isEqualTo(3);
        assertThat(layouts.anteriores())
                .extracting(LayoutExportacion::numero)
                .containsExactly(2, 1);
    }

    @Test
    void laPlantilla_esElTextoDelRecurso() {
        assertThat(servicio.plantilla()).isEqualTo(plantilla());
    }

    @Test
    void dadoUnArchivoValido_cuandoSeCarga_entoncesSeGuardaYSeRegistraEnLaMismaTransaccion() {
        doAnswer(
                        invocacion -> {
                            assertThat(dentro).isTrue();
                            return null;
                        })
                .when(registro)
                .registrar(any(LayoutCargado.class));

        LayoutExportacion cargado =
                servicio.cargar(("﻿" + VALIDO).getBytes(StandardCharsets.UTF_8), "secretaria.demo");

        assertThat(cargado.numero()).isEqualTo(4);
        assertThat(cargado.cargadoPor()).contains("secretaria.demo");
        assertThat(cargado.encabezado()).containsExactly("ID", "C", "H");
        assertThat(guardadoDentro).isTrue();
        String sha = LayoutExportacion.huella(VALIDO);
        verify(guardados).guardar(VALIDO, sha, "secretaria.demo", AHORA);
        ArgumentCaptor<LayoutCargado> entrada = ArgumentCaptor.forClass(LayoutCargado.class);
        verify(registro).registrar(entrada.capture());
        assertThat(entrada.getValue())
                .isEqualTo(new LayoutCargado("secretaria.demo", AHORA, 4, 3, sha));
    }

    @Test
    void dadoUnArchivoInvalido_cuandoSeCarga_entoncesNiSeGuardaNiSeRegistra() {
        byte[] invalido =
                "columnas:\n  - {nombre: ID, campo: identificacion}\n"
                        .getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> servicio.cargar(invalido, "secretaria.demo"))
                .isInstanceOfSatisfying(
                        LayoutInvalido.class, error -> assertThat(error.problemas()).hasSize(2));
        verify(guardados, never()).guardar(anyString(), anyString(), anyString(), any());
        verify(registro, never()).registrar(any(LayoutCargado.class));
    }

    @Test
    void dadoUnArchivoIlegible_cuandoSeCarga_entoncesNiSeGuardaNiSeRegistra() {
        byte[] ilegible = "columnas: [\n".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> servicio.cargar(ilegible, "secretaria.demo"))
                .isInstanceOf(ArchivoIlegible.class);
        verify(guardados, never()).guardar(anyString(), anyString(), anyString(), any());
    }

    @Test
    void dadoQueElRegistroFalla_cuandoSeCarga_entoncesLaCargaFalla() {
        doThrow(new IllegalStateException("sin base"))
                .when(registro)
                .registrar(any(LayoutCargado.class));

        assertThatThrownBy(
                        () ->
                                servicio.cargar(
                                        VALIDO.getBytes(StandardCharsets.UTF_8), "secretaria.demo"))
                .isInstanceOf(IllegalStateException.class);
        verify(guardados).guardar(eq(VALIDO), anyString(), anyString(), any());
    }

    private static LayoutGuardado guardado(int numero, Instant momento) {
        return new LayoutGuardado(
                numero, VALIDO, LayoutExportacion.huella(VALIDO), "secretaria.demo", momento);
    }

    private static String plantilla() {
        try (InputStream entrada =
                GestionarLayoutSigaServicioTest.class.getResourceAsStream(
                        "/reporting/layout-siga-por-defecto.yaml")) {
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }
}
