package ec.mdd.generacion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.generacion.dominio.ContenidoDelParcial;
import ec.mdd.generacion.dominio.EstadoDeVersion;
import ec.mdd.generacion.dominio.EstadoDelParcial;
import ec.mdd.generacion.dominio.ParcialCarrera;
import ec.mdd.generacion.dominio.VersionDistributivo;
import ec.mdd.generacion.puertos.entrada.DistributivoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.EstadoDelDistributivo;
import ec.mdd.generacion.puertos.entrada.EstadoDelDistributivo.ParcialYSuEstado;
import ec.mdd.generacion.puertos.entrada.ParcialMarcado;
import ec.mdd.generacion.puertos.entrada.ParcialNoEncontrado;
import ec.mdd.generacion.puertos.salida.RegistroDeCambios;
import ec.mdd.generacion.puertos.salida.RepositorioDistributivos;
import ec.mdd.generacion.puertos.salida.Transaccion;
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
    // El servicio no recibe el registro de cambios: no puede registrar (D-95).
    private final ParcialesDelDistributivoServicio servicio =
            new ParcialesDelDistributivoServicio(distributivos, transaccion);

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
}
