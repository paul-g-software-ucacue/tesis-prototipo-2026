package ec.mdd.generacion.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.generacion.puertos.salida.EventoSaliente;
import ec.mdd.plataforma.eventos.EventoEntreProcesos;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

/** Los métodos @Bean de la configuración, llamados directamente. */
class ConfiguracionGeneracionTest {

    private final ConfiguracionGeneracion configuracion = new ConfiguracionGeneracion();

    @Test
    void dadoElPublicador_cuandoPublica_entoncesEmiteElSobreConElOrigenYElMismoIdentificador() {
        List<Object> publicados = new ArrayList<>();
        UUID id = UUID.randomUUID();
        Instant momento = Instant.parse("2026-10-14T15:04:05Z");

        configuracion
                .publicadorDeEventos(publicados::add)
                .publicar(
                        new EventoSaliente(
                                id,
                                "generacion.EventoDePrueba",
                                "usuarios",
                                momento,
                                Map.of("nota", "x")));

        assertThat(publicados)
                .singleElement()
                .isEqualTo(
                        new EventoEntreProcesos(
                                id,
                                "generacion.EventoDePrueba",
                                "generacion",
                                "usuarios",
                                momento,
                                Map.of("nota", "x")));
        assertThat(configuracion.repositorioEventosDePrueba(null)).isNotNull();
        assertThat(configuracion.publicarEventoDePrueba(null, null)).isNotNull();
    }

    @Test
    void dadosLosPuertos_cuandoSePidenLosCasosDeUso_entoncesLosArma() {
        assertThat(configuracion.repositorioDistributivos(null)).isNotNull();
        assertThat(configuracion.registroDeCambiosGeneracion(null)).isNotNull();
        assertThat(configuracion.crearDistributivo(null, null, null, null)).isNotNull();
        assertThat(configuracion.proponerDocente(null, null, null, null, null)).isNotNull();
        assertThat(configuracion.consultarParciales(null, null)).isNotNull();
        assertThat(configuracion.consultarSugerencia(null, null, null)).isNotNull();
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
