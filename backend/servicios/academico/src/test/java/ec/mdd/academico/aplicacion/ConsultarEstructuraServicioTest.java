package ec.mdd.academico.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.academico.dominio.GestionAutorizada;
import ec.mdd.academico.dominio.TipoDeUnidad;
import ec.mdd.academico.dominio.UnidadOrganica;
import ec.mdd.academico.puertos.entrada.UnidadNoEncontrada;
import ec.mdd.academico.puertos.salida.RepositorioEstructura;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Consultar la estructura y el catálogo de gestiones (FR-005, FR-010), con Mockito. */
class ConsultarEstructuraServicioTest {

    private static final UnidadOrganica MATRIZ =
            UnidadOrganica.nueva(
                    "MATRIZ", "Sede matriz", TipoDeUnidad.SEDE_MATRIZ, null, null, null);
    private static final UnidadOrganica RECT =
            UnidadOrganica.nueva("RECT", "Rectorado", TipoDeUnidad.GOBIERNO, "MATRIZ", "G01", null);

    private final RepositorioEstructura repositorio = mock(RepositorioEstructura.class);
    private final ConsultarEstructuraServicio servicio =
            new ConsultarEstructuraServicio(repositorio);

    @Test
    void dadaUnaEstructura_cuandoSeConsulta_entoncesDevuelveLasUnidadesYUnaPorCodigo() {
        when(repositorio.todas()).thenReturn(List.of(MATRIZ, RECT));

        assertThat(servicio.unidades()).containsExactly(MATRIZ, RECT);
        assertThat(servicio.unidad("RECT")).isEqualTo(RECT);
        assertThatThrownBy(() -> servicio.unidad("NADA")).isInstanceOf(UnidadNoEncontrada.class);
    }

    @Test
    void dadoElCatalogo_cuandoSeConsulta_entoncesDevuelveLasGestionesDelRepositorio() {
        List<GestionAutorizada> catalogo = List.of(new GestionAutorizada("G01", "Rector"));
        when(repositorio.catalogo()).thenReturn(catalogo);

        assertThat(servicio.gestionesAutorizadas()).isEqualTo(catalogo);
    }
}
