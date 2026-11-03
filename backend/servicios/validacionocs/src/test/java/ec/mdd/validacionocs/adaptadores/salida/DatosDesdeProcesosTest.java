package ec.mdd.validacionocs.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.plataforma.eventos.ConsultasEntreProcesos;
import ec.mdd.plataforma.eventos.ProveedorDeTokenDeProceso;
import ec.mdd.plataforma.pruebas.ReceptorSimulado;
import ec.mdd.validacionocs.dominio.EstadoDelPeriodo;
import ec.mdd.validacionocs.dominio.Firmantes;
import java.time.Clock;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/** Pide el estado a Académico y los firmantes a Roles, contra procesos simulados (ADR-023). */
class DatosDesdeProcesosTest {

    private final ReceptorSimulado datos = new ReceptorSimulado();
    private final ReceptorSimulado identidad = new ReceptorSimulado();
    private final DatosDesdeProcesos adaptador =
            new DatosDesdeProcesos(
                    new ConsultasEntreProcesos(
                            RestClient.create(),
                            new ProveedorDeTokenDeProceso(
                                    RestClient.create(),
                                    datos.urlToken(),
                                    "distributivo",
                                    "clave",
                                    Clock.systemUTC())),
                    datos.base(),
                    identidad.base());

    @AfterEach
    void cerrar() {
        datos.close();
        identidad.close();
    }

    @Test
    void dadoUnPeriodo_cuandoSePideSuEstado_entoncesLoDaAcademico() {
        datos.responderConsulta(
                "/interno/academico/periodos/2026-I/estado",
                200,
                "{\"periodo\":\"2026-I\",\"estado\":\"PLANIFICACION\"}");

        assertThat(adaptador.estado("2026-I")).contains(new EstadoDelPeriodo("PLANIFICACION"));
    }

    @Test
    void dadoUnPeriodoQueNoExiste_cuandoSePideSuEstado_entoncesVacio() {
        assertThat(adaptador.estado("NADA")).isEqualTo(Optional.empty());
    }

    @Test
    void dadoUnPeriodo_cuandoSePidenLosFirmantes_entoncesLosDaRoles() {
        identidad.responderConsulta(
                "/interno/roles/firmantes",
                200,
                "{\"rolesConUsuario\":[\"RECTOR\"],\"responsableDesignado\":true}");

        assertThat(adaptador.firmantes("2026-I")).isEqualTo(new Firmantes(Set.of("RECTOR"), true));
        assertThat(identidad.direccionesConsultadas().getLast()).endsWith("?periodo=2026-I");
    }

    @Test
    void dadoQueRolesNoTieneLaRuta_cuandoSePidenLosFirmantes_entoncesFallaSinInventarlos() {
        assertThatThrownBy(() -> adaptador.firmantes("2026-I"))
                .isInstanceOf(IllegalStateException.class);
    }
}
