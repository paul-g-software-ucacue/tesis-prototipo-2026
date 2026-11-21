package ec.mdd.validacionocs.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.validacionocs.dominio.ProcesoAprobacion;
import ec.mdd.validacionocs.dominio.TipoDeEtapa;
import ec.mdd.validacionocs.dominio.TipoDeTransicion;
import ec.mdd.validacionocs.dominio.Transicion;
import ec.mdd.validacionocs.puertos.entrada.ConsultarPublicaciones.PublicacionVigente;
import ec.mdd.validacionocs.puertos.salida.RepositorioProceso;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** La publicación vigente de cada periodo, para Reporting (F20-H01, D-115). */
class ConsultarPublicacionesServicioTest {

    private static final Instant PUBLICADA = Instant.parse("2026-11-18T15:00:00Z");
    private static final String HASH = "d".repeat(64);

    private final RepositorioProceso procesos = mock(RepositorioProceso.class);
    private final ConsultarPublicacionesServicio servicio =
            new ConsultarPublicacionesServicio(procesos);

    @Test
    void sinPublicaciones_cuandoSeConsulta_entoncesNoHayNinguna() {
        when(procesos.conPublicacion()).thenReturn(List.of());

        assertThat(servicio.vigentes()).isEmpty();
    }

    @Test
    void dadosUnPublicadoYUnoEnReforma_cuandoSeConsulta_entoncesCadaUnoDaSuUltimaPublicacion() {
        ProcesoAprobacion publicado = proceso("2026-I", 1, false);
        ProcesoAprobacion enReforma = proceso("2026-II", 2, true);
        when(procesos.conPublicacion()).thenReturn(List.of("2026-I", "2026-II"));
        when(procesos.de("2026-I")).thenReturn(Optional.of(publicado));
        when(procesos.de("2026-II")).thenReturn(Optional.of(enReforma));

        assertThat(servicio.vigentes())
                .containsExactly(
                        new PublicacionVigente(
                                "2026-I", 1, HASH, "secretaria.demo", PUBLICADA, false),
                        new PublicacionVigente(
                                "2026-II", 2, HASH, "secretaria.demo", PUBLICADA, true));
    }

    @Test
    void dadoUnPeriodoQueYaNoTieneProceso_cuandoSeConsulta_entoncesSeOmite() {
        when(procesos.conPublicacion()).thenReturn(List.of("2026-I"));
        when(procesos.de("2026-I")).thenReturn(Optional.empty());

        assertThat(servicio.vigentes()).isEmpty();
    }

    private static ProcesoAprobacion proceso(String periodo, int version, boolean reforma) {
        ProcesoAprobacion proceso = mock(ProcesoAprobacion.class);
        Transicion publicacion =
                new Transicion(
                        TipoDeTransicion.PASO_DE_ETAPA,
                        TipoDeEtapa.APROBADO,
                        TipoDeEtapa.PUBLICADO,
                        Optional.empty(),
                        version,
                        HASH,
                        "secretaria.demo",
                        PUBLICADA);
        when(proceso.periodo()).thenReturn(periodo);
        when(proceso.publicacion()).thenReturn(Optional.of(publicacion));
        when(proceso.reformaEnCurso())
                .thenReturn(reforma ? Optional.of(publicacion) : Optional.empty());
        return proceso;
    }
}
