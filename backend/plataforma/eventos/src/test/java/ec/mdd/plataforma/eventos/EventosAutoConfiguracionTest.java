package ec.mdd.plataforma.eventos;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.modulith.events.FailedEventPublications;

class EventosAutoConfiguracionTest {

    private final EventosAutoConfiguracion configuracion = new EventosAutoConfiguracion();
    private final DataSource sinConexion = new SimpleDriverDataSource();
    private final FailedEventPublications sinFallidas = opciones -> {};

    @Test
    void dadoElEsquemaDeEventos_cuandoSeArmaElFlyway_entoncesSoloMigraEseEsquema() {
        MockEnvironment entorno =
                new MockEnvironment().withProperty(EsquemaDeEventos.PROPIEDAD, "eventos_prueba");

        Flyway flyway = configuracion.flywayEventos(sinConexion, entorno);

        assertThat(flyway.getConfiguration().getSchemas()).containsExactly("eventos_prueba");
        assertThat(flyway.getConfiguration().getLocations())
                .extracting(Object::toString)
                .containsExactly("classpath:db/migration/eventos");
    }

    @Test
    void dadoLaConfiguracionMinimaConLaClaveVacia_cuandoSeArmanLasPiezas_entoncesArrancan() {
        PropiedadesEventos propiedades =
                new PropiedadesEventos(
                        Duration.ofSeconds(1),
                        new PropiedadesEventos.Reenvio(Duration.ofSeconds(30)),
                        Map.of(
                                "usuarios",
                                URI.create("http://127.0.0.1:9/interno/usuarios/eventos")),
                        URI.create("http://127.0.0.1:9/interno/usuarios/token-proceso"),
                        "distributivo",
                        "");

        ProveedorDeTokenDeProceso tokens = configuracion.proveedorDeTokenDeProceso(propiedades);
        ClienteDeEventos cliente = configuracion.clienteDeEventos(propiedades, tokens);

        assertThat(configuracion.oyenteEntreProcesos(cliente)).isNotNull();
        assertThat(configuracion.reenvioDePublicaciones(sinFallidas)).isNotNull();
        assertThat(propiedades.toString()).doesNotContain("clave");
    }

    @Test
    void dadoUnEsquemaInvalidoOAusente_cuandoArrancaElContexto_entoncesFallaConUnMensajeClaro() {
        ApplicationContextRunner contexto =
                new ApplicationContextRunner()
                        .withUserConfiguration(EventosAutoConfiguracion.class)
                        .withBean(DataSource.class, () -> sinConexion)
                        .withBean(FailedEventPublications.class, () -> sinFallidas);

        contexto.withPropertyValues(EsquemaDeEventos.PROPIEDAD + "=generacion")
                .run(
                        arrancado ->
                                assertThat(arrancado)
                                        .getFailure()
                                        .rootCause()
                                        .hasMessageContaining("eventos_<proceso>"));
        contexto.run(
                arrancado ->
                        assertThat(arrancado)
                                .getFailure()
                                .rootCause()
                                .hasMessageContaining("spring.modulith.events.jdbc.schema"));
    }
}
