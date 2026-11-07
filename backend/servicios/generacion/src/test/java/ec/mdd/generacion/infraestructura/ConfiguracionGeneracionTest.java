package ec.mdd.generacion.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

/** Los métodos @Bean de la configuración, llamados directamente. */
class ConfiguracionGeneracionTest {

    private final ConfiguracionGeneracion configuracion = new ConfiguracionGeneracion();

    @Test
    void dadosLosPuertos_cuandoSePidenLosCasosDeUso_entoncesLosArma() {
        assertThat(configuracion.repositorioDistributivos(null)).isNotNull();
        assertThat(configuracion.registroDeCambiosGeneracion(null, null)).isNotNull();
        assertThat(configuracion.crearDistributivo(null, null, null, null)).isNotNull();
        assertThat(configuracion.proponerDocente(null, null, null, null, null, null)).isNotNull();
        assertThat(configuracion.consultarParciales(null, null)).isNotNull();
        assertThat(configuracion.consultarSugerencia(null, null, null, null)).isNotNull();
        assertThat(configuracion.asignarActividad(null, null, null, null, null)).isNotNull();
        assertThat(configuracion.consultarActividadesPosibles(null, null)).isNotNull();
        assertThat(configuracion.consultarActividades(null, null)).isNotNull();
        assertThat(configuracion.consultarCargaHoraria(null, null, null)).isNotNull();
    }

    @Test
    void dadaLaPlantillaDeTransacciones_cuandoSeEjecuta_entoncesDevuelveElResultado() {
        assertThat(
                        configuracion
                                .transaccionGeneracion(TransactionOperations.withoutTransaction())
                                .ejecutar(() -> "hecho"))
                .isEqualTo("hecho");
    }
}
