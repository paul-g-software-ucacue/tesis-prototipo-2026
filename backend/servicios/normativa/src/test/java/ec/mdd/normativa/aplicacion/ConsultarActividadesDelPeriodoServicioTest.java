package ec.mdd.normativa.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.normativa.dominio.ActividadDelCatalogo;
import ec.mdd.normativa.dominio.Componente;
import ec.mdd.normativa.puertos.entrada.ActividadesDeLaVersion;
import ec.mdd.normativa.puertos.entrada.PeriodoSinVersion;
import ec.mdd.normativa.puertos.salida.RepositorioActividades;
import ec.mdd.normativa.puertos.salida.RepositorioVersiones;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** El catálogo es el de la versión que rige el periodo (FR-003, D-79). Con Mockito. */
class ConsultarActividadesDelPeriodoServicioTest {

    private final RepositorioVersiones versiones = mock(RepositorioVersiones.class);
    private final RepositorioActividades actividades = mock(RepositorioActividades.class);
    private final ConsultarActividadesDelPeriodoServicio servicio =
            new ConsultarActividadesDelPeriodoServicio(versiones, actividades);

    @Test
    void dadoUnPeriodoConVersion_cuandoSeConsulta_entoncesTraeElCatalogoDeEsaVersion() {
        ActividadDelCatalogo planificar =
                new ActividadDelCatalogo(
                        "140-b", "Planificar", Componente.OTRAS_DOCENCIA, List.of(), List.of());
        when(versiones.porPeriodo("2026-I")).thenReturn(Optional.of(DatosDePrueba.dos()));
        when(actividades.deLaVersion(2)).thenReturn(List.of(planificar));

        ActividadesDeLaVersion catalogo = servicio.actividades("2026-I");

        assertThat(catalogo.version()).isEqualTo(2);
        assertThat(catalogo.actividades()).containsExactly(planificar);
    }

    @Test
    void dadoUnPeriodoSinVersion_cuandoSeConsulta_entoncesPeriodoSinVersion() {
        when(versiones.porPeriodo("2026-II")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.actividades("2026-II"))
                .isInstanceOf(PeriodoSinVersion.class);
    }
}
