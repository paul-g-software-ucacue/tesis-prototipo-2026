package ec.mdd.generacion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.generacion.dominio.CargaHoraria;
import ec.mdd.generacion.dominio.Consolidacion;
import ec.mdd.generacion.dominio.ConsolidadoDelPeriodo;
import ec.mdd.generacion.dominio.ContenidoDelParcial;
import ec.mdd.generacion.dominio.EstadoDeVersion;
import ec.mdd.generacion.dominio.EstadoDelParcial;
import ec.mdd.generacion.dominio.InformeSustentado;
import ec.mdd.generacion.dominio.ParcialCarrera;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.puertos.entrada.CondicionesDeRevision;
import ec.mdd.generacion.puertos.entrada.ConsultarConsolidado;
import ec.mdd.generacion.puertos.entrada.DistributivoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.EstadoDelDistributivo;
import ec.mdd.generacion.puertos.entrada.EstadoDelDistributivo.ParcialYSuEstado;
import ec.mdd.generacion.puertos.entrada.ParcialMarcado;
import ec.mdd.generacion.puertos.entrada.ParcialNoEncontrado;
import ec.mdd.generacion.puertos.entrada.VersionCongeladaEnGeneracion;
import ec.mdd.generacion.puertos.salida.RegistroDeCambios;
import ec.mdd.generacion.puertos.salida.RepositorioDistributivos;
import ec.mdd.generacion.puertos.salida.Transaccion;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/** Los parciales que Aprobación le pide a Generación (F12-H02, contracts/generacion.md). */
class ParcialesDelDistributivoServicioTest {

    private static final String PERIODO = "2026-I";
    private static final UUID VERSION = UUID.fromString("6f1c2d4e-8a9b-4c3d-9e1f-0a2b3c4d5e6f");
    private static final Instant CREADO = Instant.parse("2026-11-01T15:00:00Z");

    private final RepositorioDistributivos distributivos = mock(RepositorioDistributivos.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
    private final AtomicInteger transacciones = new AtomicInteger();
    private final Transaccion transaccion =
            new Transaccion() {
                @Override
                public <T> T ejecutar(Supplier<T> trabajo) {
                    transacciones.incrementAndGet();
                    return trabajo.get();
                }
            };
    private final ConsultarConsolidado consolidado = mock(ConsultarConsolidado.class);
    // El servicio no recibe el registro de cambios: no puede registrar (D-95).
    private final ParcialesDelDistributivoServicio servicio =
            new ParcialesDelDistributivoServicio(distributivos, transaccion, consolidado);

    @Test
    void dadoElDistributivo_cuandoSePideSuEstado_entoncesVersionCreacionYParciales() {
        when(distributivos.vigenteDe(PERIODO))
                .thenReturn(Optional.of(version(EstadoDelParcial.EN_ELABORACION)));
        when(distributivos.creadoEn(PERIODO)).thenReturn(Optional.of(CREADO));

        EstadoDelDistributivo estado = servicio.estadoDelDistributivo(PERIODO);

        assertThat(estado)
                .isEqualTo(
                        new EstadoDelDistributivo(
                                PERIODO,
                                1,
                                CREADO,
                                List.of(
                                        new ParcialYSuEstado("DS", false),
                                        new ParcialYSuEstado("TDS", false))));
    }

    @Test
    void dadoUnPeriodoSinDistributivo_cuandoSePideSuEstado_entoncesDistributivoNoEncontrado() {
        when(distributivos.vigenteDe(PERIODO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.estadoDelDistributivo(PERIODO))
                .isInstanceOf(DistributivoNoEncontrado.class);
    }

    @Test
    void dadoUnParcialEnElaboracion_cuandoSeMarca_entoncesBloqueaLoMarcaYDevuelveSuHash() {
        VersionDistributivo version = version(EstadoDelParcial.EN_ELABORACION);
        when(distributivos.bloquearVigente(PERIODO)).thenReturn(Optional.of(version));

        ParcialMarcado marcado = servicio.marcarParcialEnviado(PERIODO, "DS");

        verify(distributivos).marcarParcial(VERSION, "DS", EstadoDelParcial.ENVIADO);
        assertThat(marcado)
                .isEqualTo(
                        new ParcialMarcado(
                                1, ContenidoDelParcial.de(PERIODO, "DS", version).hash()));
        assertThat(transacciones).hasValue(1);
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadoUnParcialYaEnviado_cuandoSeMarcaOtraVez_entoncesDevuelveElMismoHashSinGuardar() {
        VersionDistributivo version = version(EstadoDelParcial.ENVIADO);
        when(distributivos.bloquearVigente(PERIODO)).thenReturn(Optional.of(version));

        ParcialMarcado marcado = servicio.marcarParcialEnviado(PERIODO, "DS");

        verify(distributivos, never()).marcarParcial(any(), anyString(), any());
        assertThat(marcado.hash()).isEqualTo(ContenidoDelParcial.de(PERIODO, "DS", version).hash());
    }

    @Test
    void dadaUnaCarreraSinParcial_cuandoSeMarca_entoncesParcialNoEncontrado() {
        when(distributivos.bloquearVigente(PERIODO))
                .thenReturn(Optional.of(version(EstadoDelParcial.EN_ELABORACION)));

        assertThatThrownBy(() -> servicio.marcarParcialEnviado(PERIODO, "OTRA"))
                .isInstanceOf(ParcialNoEncontrado.class);
        verify(distributivos, never()).marcarParcial(any(), anyString(), any());
    }

    // Devolver (F12-H05, contracts/generacion.md).

    @Test
    void dadasDosCarreras_cuandoSeDevuelven_entoncesBloqueaYMarcaLasDosDevueltas() {
        when(distributivos.bloquearVigente(PERIODO))
                .thenReturn(Optional.of(version(EstadoDelParcial.ENVIADO)));

        int numero = servicio.marcarParcialesDevueltos(PERIODO, List.of("DS", "TDS"));

        assertThat(numero).isEqualTo(1);
        verify(distributivos).marcarParcial(VERSION, "DS", EstadoDelParcial.DEVUELTO);
        verify(distributivos).marcarParcial(VERSION, "TDS", EstadoDelParcial.DEVUELTO);
        assertThat(transacciones).hasValue(1);
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadaUnaCarreraSinParcial_cuandoSeDevuelve_entoncesNoMarcaNinguna() {
        when(distributivos.bloquearVigente(PERIODO))
                .thenReturn(Optional.of(version(EstadoDelParcial.ENVIADO)));

        assertThatThrownBy(() -> servicio.marcarParcialesDevueltos(PERIODO, List.of("DS", "OTRA")))
                .isInstanceOf(ParcialNoEncontrado.class);
        verify(distributivos, never()).marcarParcial(any(), anyString(), any());
    }

    @Test
    void dadoUnParcialYaDevuelto_cuandoSeDevuelveOtraVez_entoncesNoLoToca() {
        when(distributivos.bloquearVigente(PERIODO))
                .thenReturn(Optional.of(version(EstadoDelParcial.DEVUELTO)));

        servicio.marcarParcialesDevueltos(PERIODO, List.of("DS"));

        verify(distributivos, never()).marcarParcial(any(), anyString(), any());
    }

    @Test
    void dadoUnPeriodoSinDistributivo_cuandoSeDevuelve_entoncesDistributivoNoEncontrado() {
        when(distributivos.bloquearVigente(PERIODO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.marcarParcialesDevueltos(PERIODO, List.of("DS")))
                .isInstanceOf(DistributivoNoEncontrado.class);
    }

    @Test
    void dadoElDistributivo_cuandoSePideSuHash_entoncesEsElDeLosHashesDeSusParciales() {
        VersionDistributivo version = version(EstadoDelParcial.ENVIADO);
        when(distributivos.vigenteDe(PERIODO)).thenReturn(Optional.of(version));

        assertThat(servicio.hashDelDistributivo(PERIODO))
                .isEqualTo(
                        ContenidoDelParcial.hashDelDistributivo(
                                PERIODO,
                                1,
                                Map.of(
                                        "DS",
                                        ContenidoDelParcial.de(PERIODO, "DS", version).hash(),
                                        "TDS",
                                        ContenidoDelParcial.de(PERIODO, "TDS", version).hash())));
    }

    // Congelar la versión (F13-H01, contracts/generacion.md, D-100).

    @Test
    void dadaUnaVersionEditable_cuandoSeCongela_entoncesLaBloqueaLaCongelaYDevuelveSuHash() {
        VersionDistributivo editable = conEstado(EstadoDeVersion.EDITABLE);
        when(distributivos.bloquearVigente(PERIODO)).thenReturn(Optional.of(editable));

        VersionCongeladaEnGeneracion congelada = servicio.congelarVersion(PERIODO);

        verify(distributivos).congelar(VERSION);
        assertThat(congelada).isEqualTo(new VersionCongeladaEnGeneracion(1, hashDe(editable)));
        assertThat(transacciones).hasValue(1);
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadaUnaVersionYaCongelada_cuandoSeCongelaOtraVez_entoncesDevuelveElMismoHashSinGuardar() {
        VersionDistributivo congelada = conEstado(EstadoDeVersion.CONGELADA);
        when(distributivos.bloquearVigente(PERIODO)).thenReturn(Optional.of(congelada));

        VersionCongeladaEnGeneracion otraVez = servicio.congelarVersion(PERIODO);

        verify(distributivos, never()).congelar(any());
        assertThat(otraVez).isEqualTo(new VersionCongeladaEnGeneracion(1, hashDe(congelada)));
    }

    @Test
    void
            dadaUnaVersionCongelada_cuandoSePideElHashDelDistributivo_entoncesEsElMismoDeLaCongelacion() {
        VersionDistributivo editable = conEstado(EstadoDeVersion.EDITABLE);
        when(distributivos.bloquearVigente(PERIODO)).thenReturn(Optional.of(editable));
        when(distributivos.vigenteDe(PERIODO)).thenReturn(Optional.of(editable));

        assertThat(servicio.congelarVersion(PERIODO).hash())
                .isEqualTo(servicio.hashDelDistributivo(PERIODO));
    }

    @Test
    void dadoUnPeriodoSinDistributivo_cuandoSeCongela_entoncesDistributivoNoEncontrado() {
        when(distributivos.bloquearVigente(PERIODO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.congelarVersion(PERIODO))
                .isInstanceOf(DistributivoNoEncontrado.class);
        verify(distributivos, never()).congelar(any());
    }

    private static VersionDistributivo conEstado(EstadoDeVersion estado) {
        return new VersionDistributivo(
                VERSION,
                1,
                estado,
                List.of(
                        new ParcialCarrera("DS", EstadoDelParcial.ENVIADO),
                        new ParcialCarrera("TDS", EstadoDelParcial.ENVIADO)),
                List.of());
    }

    private static String hashDe(VersionDistributivo version) {
        return ContenidoDelParcial.hashDelDistributivo(
                PERIODO,
                version.numero(),
                Map.of(
                        "DS", ContenidoDelParcial.de(PERIODO, "DS", version).hash(),
                        "TDS", ContenidoDelParcial.de(PERIODO, "TDS", version).hash()));
    }

    private static VersionDistributivo version(EstadoDelParcial estadoDeDs) {
        return new VersionDistributivo(
                VERSION,
                1,
                EstadoDeVersion.EDITABLE,
                List.of(
                        new ParcialCarrera("DS", estadoDeDs),
                        new ParcialCarrera("TDS", EstadoDelParcial.EN_ELABORACION)),
                List.of());
    }

    // Condiciones para enviar a revisión (F10-H03, contracts/generacion.md).

    private static final String HUELLA = "a".repeat(64);
    private static final CargaHoraria SIN_HORAS =
            new CargaHoraria(
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO);

    private static ConsolidadoDelPeriodo consolidado(
            boolean consolidado,
            boolean desactualizado,
            boolean informe,
            long duras,
            long blandas) {
        return new ConsolidadoDelPeriodo(
                PERIODO,
                true,
                Optional.of(VERSION),
                3,
                consolidado
                        ? Optional.of(new Consolidacion("consolidacion.demo", CREADO))
                        : Optional.empty(),
                desactualizado,
                List.of(),
                SIN_HORAS,
                duras,
                blandas,
                informe
                        ? Optional.of(
                                new InformeSustentado(
                                        "informe.pdf",
                                        1024,
                                        HUELLA,
                                        "clave",
                                        "consolidacion.demo",
                                        CREADO))
                        : Optional.empty());
    }

    @Test
    void dadoUnConsolidadoListo_cuandoSePidenLasCondiciones_entoncesTodasSeCumplen() {
        when(consolidado.consolidado(PERIODO)).thenReturn(consolidado(true, false, true, 0, 0));

        assertThat(servicio.condicionesDeRevision(PERIODO))
                .isEqualTo(new CondicionesDeRevision(3, true, false, Optional.of(HUELLA), 0, 0));
        verify(distributivos, never()).marcarParcial(any(), anyString(), any());
    }

    @Test
    void dadoUnConsolidadoSinNada_cuandoSePidenLasCondiciones_entoncesCadaFaltaSeVe() {
        when(consolidado.consolidado(PERIODO)).thenReturn(consolidado(false, false, false, 2, 1));

        assertThat(servicio.condicionesDeRevision(PERIODO))
                .isEqualTo(new CondicionesDeRevision(3, false, false, Optional.empty(), 2, 1));
    }

    @Test
    void dadoUnConsolidadoDesactualizado_cuandoSePidenLasCondiciones_entoncesLoDice() {
        when(consolidado.consolidado(PERIODO)).thenReturn(consolidado(true, true, true, 0, 0));

        assertThat(servicio.condicionesDeRevision(PERIODO).desactualizado()).isTrue();
    }

    @Test
    void
            dadoUnPeriodoSinDistributivo_cuandoSePidenLasCondiciones_entoncesDistributivoNoEncontrado() {
        when(consolidado.consolidado(PERIODO))
                .thenReturn(ConsolidadoDelPeriodo.sinDistributivo(PERIODO, true));

        assertThatThrownBy(() -> servicio.condicionesDeRevision(PERIODO))
                .isInstanceOf(DistributivoNoEncontrado.class);
    }
}
