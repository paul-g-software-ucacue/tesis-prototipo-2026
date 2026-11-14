package ec.mdd.firma.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.plataforma.eventos.ConsultasEntreProcesos;
import ec.mdd.plataforma.eventos.ProcesoConError;
import ec.mdd.plataforma.eventos.ProveedorDeTokenDeProceso;
import ec.mdd.plataforma.pruebas.ReceptorSimulado;
import java.time.Clock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class IdentificacionDesdeUsuariosTest {

    private final ReceptorSimulado receptor = new ReceptorSimulado();
    private final IdentificacionDesdeUsuarios identificaciones =
            new IdentificacionDesdeUsuarios(
                    new ConsultasEntreProcesos(
                            RestClient.create(),
                            new ProveedorDeTokenDeProceso(
                                    RestClient.create(),
                                    receptor.urlToken(),
                                    "distributivo",
                                    "clave",
                                    Clock.systemUTC())),
                    receptor.base());

    @AfterEach
    void cerrar() {
        receptor.close();
    }

    @Test
    void dadaUnaCuentaConIdentificacion_cuandoSePide_entoncesLaDevuelve() {
        receptor.responderConsulta(
                "/interno/usuarios/vicerrectorado.demo/identificacion",
                200,
                "{\"identificacion\":\"0109990028\"}");

        assertThat(identificaciones.de("vicerrectorado.demo")).contains("0109990028");
    }

    @Test
    void dadaUnaCuentaSinIdentificacion_cuandoSePide_entoncesVacio() {
        receptor.responderConsulta(
                "/interno/usuarios/docente.demo/identificacion",
                404,
                "{\"codigo\":\"USUARIOS_SIN_IDENTIFICACION\"}");

        assertThat(identificaciones.de("docente.demo")).isEmpty();
    }

    @Test
    void dadoUnErrorDeUsuarios_cuandoSePide_entoncesSePropaga() {
        receptor.responderConsulta(
                "/interno/usuarios/docente.demo/identificacion", 500, "{\"codigo\":\"X\"}");

        assertThatThrownBy(() -> identificaciones.de("docente.demo"))
                .isInstanceOf(ProcesoConError.class);
    }
}
