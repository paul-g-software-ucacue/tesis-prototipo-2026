package ec.mdd.generacion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ec.mdd.generacion.dominio.DatosDeLaInconsistencia;
import ec.mdd.generacion.dominio.EntradaDelPanel;
import ec.mdd.generacion.dominio.EstadoDeVersion;
import ec.mdd.generacion.dominio.InconsistenciaGlobal;
import ec.mdd.generacion.dominio.JustificacionDeInconsistencia;
import ec.mdd.generacion.dominio.JustificacionObligatoria;
import ec.mdd.generacion.dominio.ObjetoAfectado;
import ec.mdd.generacion.dominio.PanelDeInconsistencias;
import ec.mdd.generacion.dominio.Severidad;
import ec.mdd.generacion.dominio.TipoDeInconsistencia;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.puertos.entrada.ConsultarPanel;
import ec.mdd.generacion.puertos.entrada.InconsistenciaNoEncontrada;
import ec.mdd.generacion.puertos.entrada.InconsistenciaNoJustificable;
import ec.mdd.generacion.puertos.entrada.PeriodoCerrado;
import ec.mdd.generacion.puertos.entrada.SinDistributivo;
import ec.mdd.generacion.puertos.entrada.SolicitudDeJustificacion;
import ec.mdd.generacion.puertos.salida.CambioDeDistributivo;
import ec.mdd.generacion.puertos.salida.RegistroDeCambios;
import ec.mdd.generacion.puertos.salida.RepositorioJustificaciones;
import ec.mdd.generacion.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Una blanda se justifica; lo demás se rechaza sin guardar (RF-29, MD-07). */
class JustificarInconsistenciaServicioTest {

    private static final String PERIODO = "2026-I";
    private static final String DOCENTE = "0109990010";
    private static final String TEXTO = "Tiene horas de tronco común este periodo";
    private static final String QUIEN = "consolidacion.demo";
    private static final Instant AHORA = Instant.parse("2026-11-02T15:00:00Z");
    private static final VersionDistributivo VERSION =
            new VersionDistributivo(
                    UUID.randomUUID(), 2, EstadoDeVersion.EDITABLE, List.of(), List.of());
    private static final ObjetoAfectado OBJETO =
            new ObjetoAfectado(ObjetoAfectado.Clase.DOCENTE, DOCENTE, "PEREZ LUIS");

    private final ConsultarPanel panel = mock(ConsultarPanel.class);
    private final RepositorioJustificaciones justificaciones =
            mock(RepositorioJustificaciones.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
    private final Transaccion transaccion =
            new Transaccion() {
                @Override
                public <T> T ejecutar(Supplier<T> trabajo) {
                    return trabajo.get();
                }
            };
    private final JustificarInconsistenciaServicio servicio =
            new JustificarInconsistenciaServicio(
                    panel,
                    justificaciones,
                    registro,
                    transaccion,
                    Clock.fixed(AHORA, ZoneOffset.UTC));

    @BeforeEach
    void conUnaBlandaYUnaDura() {
        when(panel.panel(PERIODO)).thenReturn(conEntradas(true));
    }

    @Test
    void dadaUnaBlanda_cuandoSeJustifica_entoncesLaGuardaYDevuelveJustificada() {
        EntradaDelPanel entrada = servicio.justificar(solicitud(TipoDeInconsistencia.PROPORCION));

        assertThat(entrada.abierta()).isFalse();
        assertThat(entrada.justificacion().orElseThrow().texto()).isEqualTo(TEXTO);
        verify(justificaciones)
                .guardar(
                        VERSION.id(),
                        new JustificacionDeInconsistencia(
                                TipoDeInconsistencia.PROPORCION, DOCENTE, TEXTO, QUIEN, AHORA));
    }

    @Test
    void dadaUnaBlanda_cuandoSeJustifica_entoncesVaAlRegistroSinElTexto() {
        servicio.justificar(solicitud(TipoDeInconsistencia.PROPORCION));

        ArgumentCaptor<CambioDeDistributivo> cambio =
                ArgumentCaptor.forClass(CambioDeDistributivo.class);
        verify(registro).registrar(cambio.capture());
        assertThat(cambio.getValue().accion())
                .isEqualTo(CambioDeDistributivo.Accion.INCONSISTENCIA_JUSTIFICADA);
        assertThat(cambio.getValue().usuario()).isEqualTo(QUIEN);
        assertThat(cambio.getValue().detalle())
                .containsEntry("periodo", PERIODO)
                .containsEntry("tipo", "PROPORCION")
                .containsEntry("objeto", DOCENTE)
                .doesNotContainValue(TEXTO);
        assertThat(cambio.getValue().anterior()).isEmpty();
        assertThat(cambio.getValue().nuevo())
                .containsEntry("justificada", true)
                .containsEntry("justificadaPor", QUIEN)
                .doesNotContainValue(TEXTO);
    }

    @Test
    void dadaUnaBlandaYaJustificada_cuandoSeCambia_entoncesRegistraQuienLaJustificoAntes() {
        Instant antes = AHORA.minusSeconds(3600);
        when(panel.panel(PERIODO))
                .thenReturn(
                        panelCon(
                                true,
                                new EntradaDelPanel(
                                        blanda(),
                                        java.util.Optional.of(
                                                new JustificacionDeInconsistencia(
                                                        TipoDeInconsistencia.PROPORCION,
                                                        DOCENTE,
                                                        "Otro motivo de antes",
                                                        "otra.persona",
                                                        antes)))));

        servicio.justificar(solicitud(TipoDeInconsistencia.PROPORCION));

        ArgumentCaptor<CambioDeDistributivo> cambio =
                ArgumentCaptor.forClass(CambioDeDistributivo.class);
        verify(registro).registrar(cambio.capture());
        assertThat(cambio.getValue().anterior())
                .containsEntry("justificadaPor", "otra.persona")
                .containsEntry("justificadaEn", antes.toString())
                .doesNotContainValue("Otro motivo de antes");
        assertThat(cambio.getValue().nuevo()).containsEntry("justificadaPor", QUIEN);
    }

    @Test
    void dadaUnaYaJustificada_cuandoSeCambia_entoncesAgregaOtra() {
        when(panel.panel(PERIODO))
                .thenReturn(
                        panelCon(
                                true,
                                new EntradaDelPanel(
                                        blanda(),
                                        java.util.Optional.of(
                                                new JustificacionDeInconsistencia(
                                                        TipoDeInconsistencia.PROPORCION,
                                                        DOCENTE,
                                                        "El texto de antes",
                                                        QUIEN,
                                                        AHORA)))));

        EntradaDelPanel entrada = servicio.justificar(solicitud(TipoDeInconsistencia.PROPORCION));

        assertThat(entrada.justificacion().orElseThrow().texto()).isEqualTo(TEXTO);
        verify(justificaciones).guardar(any(), any());
    }

    @Test
    void dadaUnaDura_cuandoSeJustifica_entoncesLaRechazaSinGuardar() {
        assertThatThrownBy(() -> servicio.justificar(solicitud(TipoDeInconsistencia.SOBRECARGA)))
                .isInstanceOf(InconsistenciaNoJustificable.class);

        assertNoGuardoNada();
    }

    @Test
    void dadaUnaExcepcionDeAfinidad_cuandoSeJustifica_entoncesSeHaceEnLaPropuesta() {
        assertThatThrownBy(
                        () ->
                                servicio.justificar(
                                        solicitud(TipoDeInconsistencia.EXCEPCION_DE_AFINIDAD)))
                .isInstanceOf(InconsistenciaNoJustificable.class);

        verifyNoInteractions(panel);
        assertNoGuardoNada();
    }

    @Test
    void dadaUnaQueYaNoExiste_cuandoSeJustifica_entoncesNoEncontrada() {
        assertThatThrownBy(
                        () ->
                                servicio.justificar(
                                        solicitud(TipoDeInconsistencia.GESTORES_FUERA_DE_RANGO)))
                .isInstanceOf(InconsistenciaNoEncontrada.class);

        assertNoGuardoNada();
    }

    @Test
    void dadoUnPeriodoCerrado_cuandoSeJustifica_entoncesLoRechaza() {
        when(panel.panel(PERIODO)).thenReturn(conEntradas(false));

        assertThatThrownBy(() -> servicio.justificar(solicitud(TipoDeInconsistencia.PROPORCION)))
                .isInstanceOf(PeriodoCerrado.class);

        assertNoGuardoNada();
    }

    @Test
    void dadoUnPeriodoSinDistributivo_cuandoSeJustifica_entoncesLoRechaza() {
        when(panel.panel(PERIODO))
                .thenReturn(PanelDeInconsistencias.sinDistributivo(PERIODO, true));

        assertThatThrownBy(() -> servicio.justificar(solicitud(TipoDeInconsistencia.PROPORCION)))
                .isInstanceOf(SinDistributivo.class);

        assertNoGuardoNada();
    }

    @Test
    void conUnTextoCorto_cuandoSeJustifica_entoncesPideCorregirloSinGuardar() {
        assertThatThrownBy(
                        () ->
                                servicio.justificar(
                                        new SolicitudDeJustificacion(
                                                PERIODO,
                                                TipoDeInconsistencia.PROPORCION,
                                                DOCENTE,
                                                "corto",
                                                QUIEN)))
                .isInstanceOf(JustificacionObligatoria.class);

        assertNoGuardoNada();
    }

    private void assertNoGuardoNada() {
        verify(justificaciones, never()).guardar(any(), any());
        verify(registro, never()).registrar(any());
    }

    private static SolicitudDeJustificacion solicitud(TipoDeInconsistencia tipo) {
        return new SolicitudDeJustificacion(PERIODO, tipo, DOCENTE, TEXTO, QUIEN);
    }

    private static PanelDeInconsistencias conEntradas(boolean modificable) {
        return panelCon(
                modificable,
                new EntradaDelPanel(blanda(), java.util.Optional.empty()),
                new EntradaDelPanel(
                        new InconsistenciaGlobal(
                                TipoDeInconsistencia.SOBRECARGA,
                                Severidad.DURA,
                                OBJETO,
                                DatosDeLaInconsistencia.ninguno()),
                        java.util.Optional.empty()));
    }

    private static InconsistenciaGlobal blanda() {
        return new InconsistenciaGlobal(
                TipoDeInconsistencia.PROPORCION,
                Severidad.BLANDA,
                OBJETO,
                DatosDeLaInconsistencia.ninguno());
    }

    private static PanelDeInconsistencias panelCon(
            boolean modificable, EntradaDelPanel... entradas) {
        return new PanelDeInconsistencias(
                PERIODO,
                modificable,
                java.util.Optional.of(VERSION.id()),
                VERSION.numero(),
                List.of(entradas));
    }
}
