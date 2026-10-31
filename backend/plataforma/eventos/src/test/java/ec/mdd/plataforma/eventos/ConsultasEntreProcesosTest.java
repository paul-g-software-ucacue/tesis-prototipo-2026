package ec.mdd.plataforma.eventos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.plataforma.pruebas.ReceptorSimulado;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Las consultas entre procesos con el token de proceso y el plazo de espera (ADR-023). */
class ConsultasEntreProcesosTest {

    private static final String RUTA = "/interno/normativa/periodos/2026-I/pesos-de-afinidad";

    private record Dato(int version, BigDecimal peso) {}

    private final ReceptorSimulado receptor = new ReceptorSimulado();

    @AfterEach
    void cerrar() {
        receptor.close();
    }

    @Test
    void dadoElDuenoQueResponde_cuandoSeConsulta_entoncesDevuelveElCuerpoConElTokenDeProceso() {
        receptor.responderConsulta(RUTA, 200, "{\"version\":3,\"peso\":2.5}");

        Optional<Dato> dato = consultas("clave").obtener(url(), Dato.class);

        assertThat(dato).contains(new Dato(3, new BigDecimal("2.5")));
        assertThat(receptor.consultasRecibidas().getFirst().autorizacion())
                .isEqualTo("Bearer token-1");
    }

    @Test
    void dadoUn404_cuandoSeConsulta_entoncesResultadoVacio() {
        receptor.responderConsulta(RUTA, 404, "");

        assertThat(consultas("clave").obtener(url(), Dato.class)).isEmpty();
    }

    @Test
    void dadoUnTokenRechazado_cuandoSeConsulta_entoncesPideOtroYReintentaUnaVez() {
        receptor.responderConsulta(RUTA, 401, "");
        receptor.responderConsulta(RUTA, 200, "{\"version\":3,\"peso\":1}");

        assertThat(consultas("clave").obtener(url(), Dato.class)).isPresent();

        assertThat(receptor.tokensEmitidos()).isEqualTo(2);
        assertThat(receptor.consultasRecibidas().getLast().autorizacion())
                .isEqualTo("Bearer token-2");
    }

    @Test
    void dadoDosRechazosDelToken_cuandoSeConsulta_entoncesProcesoConErrorSinMasIntentos() {
        receptor.responderConsulta(RUTA, 401, "");
        receptor.responderConsulta(RUTA, 401, "");

        assertThatThrownBy(() -> consultas("clave").obtener(url(), Dato.class))
                .isInstanceOf(ProcesoConError.class);

        assertThat(receptor.consultasRecibidas()).hasSize(2);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 403, 500, 503})
    void dadoUnEstadoInesperado_cuandoSeConsulta_entoncesProcesoConError(int estado) {
        receptor.responderConsulta(RUTA, estado, "");

        assertThatThrownBy(() -> consultas("clave").obtener(url(), Dato.class))
                .isInstanceOf(ProcesoConError.class)
                .hasMessageContaining(String.valueOf(estado));
    }

    @Test
    void dadoUnCuerpoIlegible_cuandoSeConsulta_entoncesProcesoConError() {
        receptor.responderConsulta(RUTA, 200, "{no es json");

        assertThatThrownBy(() -> consultas("clave").obtener(url(), Dato.class))
                .isInstanceOf(ProcesoConError.class);
    }

    @Test
    void dadoElProcesoDetenido_cuandoSeConsulta_entoncesProcesoConError() {
        // El token se emite antes de detener: el fallo que se prueba es el de la consulta.
        ConsultasEntreProcesos consultas = consultas("clave");
        receptor.responderConsulta(RUTA, 200, "{\"version\":3,\"peso\":1}");
        consultas.obtener(url(), Dato.class);
        receptor.detener();

        assertThatThrownBy(() -> consultas.obtener(url(), Dato.class))
                .isInstanceOf(ProcesoConError.class);
    }

    @Test
    void dadoElProcesoLento_cuandoSeAgotaElPlazo_entoncesProcesoSinRespuesta() {
        ConsultasEntreProcesos consultas = consultas("clave");
        receptor.responderConsulta(RUTA, 200, "{\"version\":3,\"peso\":1}");
        receptor.responderConsulta(RUTA, 200, "{\"version\":3,\"peso\":1}");
        consultas.obtener(url(), Dato.class);
        receptor.retrasar(Duration.ofSeconds(3));

        assertThatThrownBy(() -> consultas.obtener(url(), Dato.class))
                .isInstanceOf(ProcesoSinRespuesta.class);
    }

    @Test
    void dadoUsuariosQueNoEmiteElToken_cuandoSeConsulta_entoncesProcesoConError() {
        receptor.responderToken(500);

        assertThatThrownBy(() -> consultas("clave").obtener(url(), Dato.class))
                .isInstanceOf(ProcesoConError.class);

        assertThat(receptor.consultasRecibidas()).isEmpty();
    }

    @Test
    void dadoUnProcesoSinClave_cuandoSeConsulta_entoncesProcesoConError() {
        assertThatThrownBy(() -> consultas("").obtener(url(), Dato.class))
                .isInstanceOf(ProcesoConError.class);
    }

    @Test
    void dadoUnaConsultaQueFalla_cuandoSeLanza_entoncesElMensajeNoLlevaLaRuta() {
        receptor.responderConsulta(RUTA, 500, "");

        assertThatThrownBy(() -> consultas("clave").obtener(url(), Dato.class))
                .hasMessageNotContaining("interno");
    }

    private URI url() {
        return receptor.base().resolve(RUTA);
    }

    private ConsultasEntreProcesos consultas(String clave) {
        Duration plazo = Duration.ofMillis(800);
        return new ConsultasEntreProcesos(
                Clientes.http(plazo),
                new ProveedorDeTokenDeProceso(
                        Clientes.http(plazo),
                        receptor.urlToken(),
                        "distributivo",
                        clave,
                        Clock.systemUTC()));
    }
}
