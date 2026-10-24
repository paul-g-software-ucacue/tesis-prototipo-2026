package ec.mdd.cargas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.cargas.adaptadores.salida.DiccionarioYaml;
import ec.mdd.cargas.dominio.DiccionarioDeDatos;
import ec.mdd.cargas.puertos.entrada.CargaNoEncontrada;
import ec.mdd.cargas.puertos.salida.FuenteDelDiccionario;
import org.junit.jupiter.api.Test;

/** Consultar el diccionario de datos y la carga de cada plantilla (F01-H01), con Mockito. */
class ConsultarPlantillasServicioTest {

    private static final DiccionarioDeDatos DICCIONARIO = new DiccionarioYaml().leer();

    private final FuenteDelDiccionario fuente = mock(FuenteDelDiccionario.class);

    @Test
    void dadaLaFuente_cuandoSeConsultaVariasVeces_entoncesLaLeeUnaSolaVez() {
        when(fuente.leer()).thenReturn(DICCIONARIO);
        ConsultarPlantillasServicio servicio = new ConsultarPlantillasServicio(fuente);

        assertThat(servicio.diccionario()).isSameAs(DICCIONARIO);
        assertThat(servicio.carga("docentes").numero()).isEqualTo(6);
        assertThat(servicio.diccionario()).isSameAs(DICCIONARIO);
        verify(fuente, times(1)).leer();
    }

    @Test
    void dadoUnCodigoQueNoEsDeUnaCarga_cuandoSePide_entoncesCargaNoEncontrada() {
        when(fuente.leer()).thenReturn(DICCIONARIO);
        ConsultarPlantillasServicio servicio = new ConsultarPlantillasServicio(fuente);

        assertThatThrownBy(() -> servicio.carga("comisiones"))
                .isInstanceOf(CargaNoEncontrada.class)
                .hasMessageContaining("comisiones");
    }
}
