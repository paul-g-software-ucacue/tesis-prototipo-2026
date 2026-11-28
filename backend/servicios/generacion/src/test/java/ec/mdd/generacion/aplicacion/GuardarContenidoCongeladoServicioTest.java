package ec.mdd.generacion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.generacion.dominio.ActividadNoLectiva;
import ec.mdd.generacion.dominio.AsignaturaOfertada;
import ec.mdd.generacion.dominio.AsignaturaParalelo;
import ec.mdd.generacion.dominio.CargaCongelada;
import ec.mdd.generacion.dominio.CargaHoraria;
import ec.mdd.generacion.dominio.ContenidoCongelado;
import ec.mdd.generacion.dominio.Dedicacion;
import ec.mdd.generacion.dominio.DocenteDeLaVersion;
import ec.mdd.generacion.dominio.EstadoDeVersion;
import ec.mdd.generacion.dominio.InconsistenciasAlCongelar;
import ec.mdd.generacion.dominio.Justificacion;
import ec.mdd.generacion.dominio.PanelDeInconsistencias;
import ec.mdd.generacion.dominio.ParcialCarrera;
import ec.mdd.generacion.dominio.Propuesta;
import ec.mdd.generacion.dominio.Severidad;
import ec.mdd.generacion.dominio.TipoDeJustificacion;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.puertos.entrada.ConsultarContenidoCongelado.CargaDeLaVersion;
import ec.mdd.generacion.puertos.entrada.ConsultarContenidoCongelado.CargasCongeladas;
import ec.mdd.generacion.puertos.entrada.ConsultarPanel;
import ec.mdd.generacion.puertos.salida.DatosParaElDocumento;
import ec.mdd.generacion.puertos.salida.DatosParaSugerencia;
import ec.mdd.generacion.puertos.salida.RepositorioContenidoCongelado;
import ec.mdd.generacion.puertos.salida.RepositorioDistributivos;
import ec.mdd.generacion.puertos.salida.Transaccion;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Guardar y consultar el contenido congelado (F20-H01, D-115): idempotente, solo de una versión
 * congelada, y tardío si falta al pedirlo.
 */
class GuardarContenidoCongeladoServicioTest {

    private static final Instant AHORA = Instant.parse("2026-11-21T15:00:00Z");
    private static final String PERIODO = "2026-I";
    private static final String ANA = "0109990010";
    private static final AsignaturaParalelo BD_A =
            new AsignaturaParalelo(
                    new AsignaturaOfertada("BD-301", "Bases de datos", "06", "061"),
                    "A",
                    "TDS",
                    new BigDecimal("8"),
                    false,
                    false);

    private final RepositorioDistributivos distributivos = mock(RepositorioDistributivos.class);
    private final RepositorioContenidoCongelado repositorio =
            mock(RepositorioContenidoCongelado.class);
    private final DatosParaSugerencia datos = mock(DatosParaSugerencia.class);
    private final DatosParaElDocumento carreras = mock(DatosParaElDocumento.class);
    private final ConsultarPanel panel = mock(ConsultarPanel.class);
    private final Transaccion transaccion =
            new Transaccion() {
                @Override
                public <T> T ejecutar(Supplier<T> trabajo) {
                    return trabajo.get();
                }
            };
    private final GuardarContenidoCongeladoServicio servicio =
            new GuardarContenidoCongeladoServicio(
                    distributivos,
                    repositorio,
                    datos,
                    carreras,
                    panel,
                    transaccion,
                    Clock.fixed(AHORA, ZoneOffset.UTC));
    private final ConsultarContenidoCongeladoServicio consulta =
            new ConsultarContenidoCongeladoServicio(servicio, repositorio);

    @Test
    void dadaUnaVersionCongeladaSinContenido_cuandoSeGuarda_entoncesLoGuardaNoTardio() {
        VersionDistributivo version = version(EstadoDeVersion.CONGELADA);
        cuandoLaVersionEs(version);

        servicio.guardar(PERIODO, 1);

        ArgumentCaptor<ContenidoCongelado> guardado =
                ArgumentCaptor.forClass(ContenidoCongelado.class);
        verify(repositorio)
                .guardar(eq(version.id()), guardado.capture(), any(), eq(AHORA), eq(false));
        assertThat(guardado.getValue().cargas())
                .singleElement()
                .satisfies(
                        carga -> {
                            assertThat(carga.identificacion()).isEqualTo(ANA);
                            assertThat(carga.horas().clase()).isEqualByComparingTo("8");
                            assertThat(carga.asignaturas().get(0).nombreCarrera())
                                    .isEqualTo("Software");
                        });
    }

    @Test
    void dadaUnaVersionConContenido_cuandoSeGuardaOtraVez_entoncesNoHaceNada() {
        when(repositorio.hash(PERIODO, 1)).thenReturn(Optional.of("a".repeat(64)));

        servicio.guardar(PERIODO, 1);

        verify(repositorio, never()).guardar(any(), any(), any(), any(), anyBoolean());
        verify(datos, never()).ofertaDelPeriodo(any());
    }

    @Test
    void dadaUnaVersionEditableOInexistente_cuandoSeGuarda_entoncesNoHaceNada() {
        cuandoLaVersionEs(version(EstadoDeVersion.EDITABLE));
        servicio.guardar(PERIODO, 1);
        when(distributivos.version(PERIODO, 2)).thenReturn(Optional.empty());
        servicio.guardar(PERIODO, 2);

        verify(repositorio, never()).guardar(any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void dadaUnaVersionCongeladaSinContenido_cuandoSeConsulta_entoncesLoGuardaTardioYLoLee() {
        VersionDistributivo version = version(EstadoDeVersion.CONGELADA);
        cuandoLaVersionEs(version);
        List<DocenteDeLaVersion> docentes =
                List.of(
                        new DocenteDeLaVersion(
                                ANA,
                                "PEREZ ANA",
                                Dedicacion.TC,
                                new CargaHoraria(
                                        new BigDecimal("8"),
                                        BigDecimal.ZERO,
                                        BigDecimal.ZERO,
                                        BigDecimal.ZERO,
                                        BigDecimal.ZERO)));
        when(repositorio.hash(PERIODO, 1))
                .thenReturn(Optional.empty(), Optional.of("b".repeat(64)));
        when(repositorio.docentes(PERIODO, 1)).thenReturn(docentes);

        Optional<CargasCongeladas> cargas = consulta.cargas(PERIODO, 1);

        verify(repositorio).guardar(eq(version.id()), any(), any(), eq(AHORA), eq(true));
        assertThat(cargas).contains(new CargasCongeladas("b".repeat(64), docentes));
    }

    @Test
    void dadaUnaVersionQueNoEstaCongelada_cuandoSeConsulta_entoncesNoHayContenido() {
        cuandoLaVersionEs(version(EstadoDeVersion.ANULADA));

        assertThat(consulta.cargas(PERIODO, 1)).isEmpty();
        assertThat(consulta.carga(PERIODO, 1, ANA)).isEmpty();
    }

    @Test
    void dadoUnDocente_cuandoSeConsultaSuCarga_entoncesVieneConElHashOVaciaSiNoEsta() {
        CargaCongelada carga = mock(CargaCongelada.class);
        when(repositorio.hash(PERIODO, 1)).thenReturn(Optional.of("c".repeat(64)));
        when(repositorio.carga(PERIODO, 1, ANA)).thenReturn(Optional.of(carga));
        when(repositorio.carga(PERIODO, 1, "0109990099")).thenReturn(Optional.empty());

        assertThat(consulta.carga(PERIODO, 1, ANA))
                .contains(new CargaDeLaVersion("c".repeat(64), carga));
        assertThat(consulta.carga(PERIODO, 1, "0109990099")).isEmpty();
    }

    // F19-H02 (D-120): las inconsistencias abiertas de la versión, fijadas al guardar su contenido.
    @Test
    void dadoElPanelDeLaMismaVersion_cuandoSeGuarda_entoncesGuardaSusDurasYBlandasAbiertas() {
        VersionDistributivo version = version(EstadoDeVersion.CONGELADA);
        cuandoLaVersionEs(version);
        cuandoElPanelEs(1, 1, 2);

        servicio.guardar(PERIODO, 1);

        verify(repositorio)
                .guardar(
                        eq(version.id()),
                        any(),
                        eq(Optional.of(new InconsistenciasAlCongelar(1, 2))),
                        eq(AHORA),
                        eq(false));
    }

    @Test
    void dadoElPanelDeOtraVersion_cuandoSeGuarda_entoncesNoGuardaInconsistencias() {
        VersionDistributivo version = version(EstadoDeVersion.CONGELADA);
        cuandoLaVersionEs(version);
        cuandoElPanelEs(2, 0, 0);

        servicio.guardar(PERIODO, 1);

        verify(repositorio)
                .guardar(eq(version.id()), any(), eq(Optional.empty()), eq(AHORA), eq(false));
    }

    @Test
    void dadoUnPeriodoSinPanel_cuandoSeGuarda_entoncesNoGuardaInconsistencias() {
        VersionDistributivo version = version(EstadoDeVersion.CONGELADA);
        cuandoLaVersionEs(version);
        when(panel.panel(PERIODO))
                .thenReturn(PanelDeInconsistencias.sinDistributivo(PERIODO, false));

        servicio.guardar(PERIODO, 1);

        verify(repositorio)
                .guardar(eq(version.id()), any(), eq(Optional.empty()), eq(AHORA), eq(false));
    }

    @Test
    void dadoUnGuardadoTardioDeLaVersionVigente_cuandoSeConsulta_entoncesTambienLasGuarda() {
        VersionDistributivo version = version(EstadoDeVersion.CONGELADA);
        cuandoLaVersionEs(version);
        cuandoElPanelEs(1, 0, 0);

        consulta.cargas(PERIODO, 1);

        verify(repositorio)
                .guardar(
                        eq(version.id()),
                        any(),
                        eq(Optional.of(new InconsistenciasAlCongelar(0, 0))),
                        eq(AHORA),
                        eq(true));
    }

    @Test
    void dadoUnPanelQueFallaAlCongelar_cuandoSeGuarda_entoncesNoGuardaYPropagaParaReintentar() {
        cuandoLaVersionEs(version(EstadoDeVersion.CONGELADA));
        doThrow(new IllegalStateException("datos no responde")).when(panel).panel(PERIODO);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> servicio.guardar(PERIODO, 1))
                .isInstanceOf(IllegalStateException.class);
        verify(repositorio, never()).guardar(any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void dadoUnPanelQueFallaEnUnGuardadoTardio_cuandoSeConsulta_entoncesGuardaSinInconsistencias() {
        VersionDistributivo version = version(EstadoDeVersion.CONGELADA);
        cuandoLaVersionEs(version);
        doThrow(new IllegalStateException("datos no responde")).when(panel).panel(PERIODO);

        consulta.cargas(PERIODO, 1);

        verify(repositorio)
                .guardar(eq(version.id()), any(), eq(Optional.empty()), eq(AHORA), eq(true));
    }

    @Test
    void dadoUnContenidoGuardado_cuandoSeConsultaCompleto_entoncesTraeSusInconsistencias() {
        when(repositorio.hash(PERIODO, 1)).thenReturn(Optional.of("d".repeat(64)));
        when(repositorio.cargas(PERIODO, 1)).thenReturn(List.of());
        when(repositorio.inconsistencias(PERIODO, 1))
                .thenReturn(Optional.of(new InconsistenciasAlCongelar(0, 1)));

        assertThat(consulta.contenido(PERIODO, 1))
                .hasValueSatisfying(
                        completo ->
                                assertThat(completo.inconsistencias())
                                        .contains(new InconsistenciasAlCongelar(0, 1)));
    }

    private void cuandoElPanelEs(int numero, long duras, long blandas) {
        PanelDeInconsistencias leido = mock(PanelDeInconsistencias.class);
        when(leido.creado()).thenReturn(true);
        when(leido.numero()).thenReturn(numero);
        when(leido.abiertas(Severidad.DURA)).thenReturn(duras);
        when(leido.abiertas(Severidad.BLANDA)).thenReturn(blandas);
        when(panel.panel(PERIODO)).thenReturn(leido);
    }

    private void cuandoLaVersionEs(VersionDistributivo version) {
        when(repositorio.hash(PERIODO, 1)).thenReturn(Optional.empty());
        when(distributivos.version(PERIODO, 1)).thenReturn(Optional.of(version));
        when(datos.ofertaDelPeriodo(PERIODO)).thenReturn(Optional.of(List.of(BD_A)));
        when(datos.docentesActivos()).thenReturn(List.of());
        when(carreras.nombresDeCarreras()).thenReturn(Map.of("TDS", "Software"));
        when(repositorio.guardar(any(), any(), any(), any(), anyBoolean())).thenReturn(true);
        when(panel.panel(PERIODO))
                .thenReturn(PanelDeInconsistencias.sinDistributivo(PERIODO, false));
    }

    private static VersionDistributivo version(EstadoDeVersion estado) {
        return new VersionDistributivo(
                UUID.randomUUID(),
                1,
                estado,
                List.of(ParcialCarrera.nuevo("TDS")),
                List.of(
                        new Propuesta(
                                "BD-301",
                                "A",
                                "TDS",
                                ANA,
                                "PEREZ ANA",
                                new Justificacion(TipoDeJustificacion.AFINIDAD, null),
                                AHORA,
                                "coordinacion.demo")),
                List.<ActividadNoLectiva>of());
    }
}
